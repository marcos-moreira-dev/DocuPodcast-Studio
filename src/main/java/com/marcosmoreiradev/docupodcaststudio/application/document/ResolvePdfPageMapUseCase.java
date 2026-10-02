package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMapManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Lazy, non-destructive PageMap migration with an immediate V3 rollback path. */
public final class ResolvePdfPageMapUseCase {
    public static final String FEATURE_PROPERTY = "docupodcast.pdf.page-map.enabled";

    private final PreparedPdfDocumentRepository v3Repository;
    private final PdfPageMapRepository pageMapRepository;
    private final BuildPdfPageMapUseCase builder;
    private final boolean enabled;

    public ResolvePdfPageMapUseCase(PreparedPdfDocumentRepository v3Repository,
                                    PdfPageMapRepository pageMapRepository,
                                    BuildPdfPageMapUseCase builder,
                                    boolean enabled) {
        this.v3Repository = Objects.requireNonNull(v3Repository, "v3Repository");
        this.pageMapRepository = Objects.requireNonNull(pageMapRepository, "pageMapRepository");
        this.builder = Objects.requireNonNull(builder, "builder");
        this.enabled = enabled;
    }

    public static ResolvePdfPageMapUseCase fromSystemProperty(
            PreparedPdfDocumentRepository v3Repository,
            PdfPageMapRepository pageMapRepository) {
        return new ResolvePdfPageMapUseCase(v3Repository, pageMapRepository,
                new BuildPdfPageMapUseCase(), Boolean.getBoolean(FEATURE_PROPERTY));
    }

    public boolean enabled() {
        return enabled;
    }

    public PdfPageMapResolution resolve(PreparedPdfWorkspaceRef workspace, int pageNumber) throws IOException {
        if (workspace == null) throw new IllegalArgumentException("workspace is required");
        PreparedPdfPage source = v3Repository.loadPage(workspace.projectRoot(), pageNumber)
                .orElseThrow(() -> new IOException("No existe la página V3 " + pageNumber));
        if (!enabled) {
            return new PdfPageMapResolution(builder.build(source, null),
                    PdfPageMapResolution.Source.V3_FALLBACK,
                    List.of("PdfPageMap desactivado; se usa V3 sin escribir sidecars."));
        }
        try {
            Optional<PdfPageMapManifest> manifest = pageMapRepository.loadManifest(workspace.projectRoot());
            Optional<PdfPageMap> persisted = pageMapRepository.loadPage(workspace.projectRoot(), pageNumber);
            boolean compatibleManifest = manifest
                    .filter(value -> value.sourceSha256().equals(workspace.sourceSha256()))
                    .filter(value -> value.builderSignature().equals(BuildPdfPageMapUseCase.BUILDER_SIGNATURE))
                    .isPresent();
            if (compatibleManifest && persisted
                    .filter(value -> value.sourcePageRevision() == source.revision())
                    .filter(value -> value.builderSignature().equals(BuildPdfPageMapUseCase.BUILDER_SIGNATURE))
                    .isPresent()) {
                return new PdfPageMapResolution(persisted.orElseThrow(),
                        PdfPageMapResolution.Source.SIDECAR, List.of());
            }
            PdfPageMap previous = compatibleManifest ? persisted.orElse(null) : null;
            PdfPageMap rebuilt = builder.build(source, previous);
            pageMapRepository.savePage(workspace.projectRoot(), workspace.sourceSha256(), rebuilt);
            return new PdfPageMapResolution(rebuilt,
                    PdfPageMapResolution.Source.LAZY_REBUILT, List.of());
        } catch (IOException | RuntimeException sidecarFailure) {
            return new PdfPageMapResolution(builder.build(source, null),
                    PdfPageMapResolution.Source.V3_FALLBACK,
                    List.of("PdfPageMap no disponible; fallback V3: " + sidecarFailure.getMessage()));
        }
    }
}
