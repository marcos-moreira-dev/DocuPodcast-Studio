package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Inspects manifest, page files, temporaries, cache and dependent jobs without mutating them. */
public final class DiagnosePreparedPdfWorkspaceUseCase {
    private final PreparedPdfDocumentRepository repository;

    public DiagnosePreparedPdfWorkspaceUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
    }

    public PdfWorkspaceDiagnosticReport diagnose(Path projectRoot, Path cacheDirectory) {
        ArrayList<String> issues = new ArrayList<>();
        int declared = 0;
        List<PreparedPdfPage> pages = List.of();
        boolean manifestValid = false;
        try {
            var manifest = repository.loadManifest(projectRoot);
            manifestValid = manifest.isPresent();
            declared = manifest.map(value -> value.pageCount()).orElse(0);
            if (!manifestValid) issues.add("Falta document/manifest.json.");
            pages = repository.loadPages(projectRoot);
        } catch (IOException ex) {
            issues.add("No se pudo validar el almacenamiento PDF V2: " + ex.getMessage());
        }
        long regions = pages.stream().mapToLong(page -> page.regions().size()).sum();
        long uncertain = pages.stream().flatMap(page -> page.regions().stream())
                .filter(region -> region.effectiveNarratability() == PdfNarratability.UNCERTAIN).count();
        long ocr = pages.stream().flatMap(page -> page.regions().stream())
                .filter(region -> region.evidence().origin() == PdfRegionOrigin.OCR_LOCAL
                        || region.evidence().origin() == PdfRegionOrigin.HYBRID).count();
        int derived = pages.stream().mapToInt(page -> page.derivedTreatments().size()).sum();
        Path document = projectRoot == null ? null : projectRoot.resolve("document");
        int temporaries = countMatching(document, path -> path.getFileName().toString().endsWith(".tmp"));
        long preparedBytes = size(document);
        long cacheBytes = size(cacheDirectory);
        Path jobs = projectRoot == null ? null : projectRoot.resolve("audio").resolve("jobs");
        int audioJobs = countMatching(jobs, path -> "job.json".equalsIgnoreCase(path.getFileName().toString()));
        long totalMillis = pages.stream().mapToLong(
                page -> page.preparationMetrics().elapsedMillis()).sum();
        long totalCpuMillis = pages.stream().mapToLong(
                page -> page.preparationMetrics().cpuMillis()).sum();
        long maximumHeap = pages.stream().mapToLong(
                page -> page.preparationMetrics().observedHeapBytes()).max().orElse(0L);
        double averageMillis = pages.isEmpty() ? 0.0 : totalMillis / (double) pages.size();
        if (declared > 0 && pages.size() > declared) issues.add("Hay más páginas preparadas que las declaradas.");
        if (temporaries > 0) issues.add("Existen temporales recuperables pendientes de inspección.");
        return new PdfWorkspaceDiagnosticReport(manifestValid, declared, pages.size(), temporaries,
                Math.toIntExact(regions), Math.toIntExact(uncertain), derived,
                preparedBytes, cacheBytes, audioJobs, regions == 0 ? 0.0 : ocr / (double) regions,
                totalMillis, totalCpuMillis, maximumHeap, averageMillis, issues);
    }

    private static int countMatching(Path root, java.util.function.Predicate<Path> predicate) {
        if (root == null || !Files.isDirectory(root)) return 0;
        try (var paths = Files.walk(root)) {
            return Math.toIntExact(paths.filter(Files::isRegularFile).filter(predicate).count());
        } catch (IOException ex) {
            return 0;
        }
    }

    private static long size(Path root) {
        if (root == null || !Files.exists(root)) return 0L;
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ex) {
                    return 0L;
                }
            }).sum();
        } catch (IOException ex) {
            return 0L;
        }
    }
}
