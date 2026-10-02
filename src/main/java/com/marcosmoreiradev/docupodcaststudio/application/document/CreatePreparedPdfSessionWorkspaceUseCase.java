package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Creates and owns the canonical V2 workspace used by a PDF before its first project save.
 *
 * <p>The marker makes cleanup deliberately narrow: arbitrary project directories can never be
 * removed through this use case.</p>
 */
public final class CreatePreparedPdfSessionWorkspaceUseCase {
    static final String SESSION_MARKER = ".docupodcast-pdf-session";

    private final PreparedPdfDocumentRepository repository;
    private final BuildPdfVisualDocumentUseCase inspectPdf;

    public CreatePreparedPdfSessionWorkspaceUseCase(PreparedPdfDocumentRepository repository,
                                                    BuildPdfVisualDocumentUseCase inspectPdf) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.inspectPdf = Objects.requireNonNull(inspectPdf, "inspectPdf");
    }

    public PreparedPdfSource create(Path externalPdf, String title) throws IOException {
        Path input = Objects.requireNonNull(externalPdf, "externalPdf").toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) throw new IOException("No existe el PDF fuente: " + input);

        Path root = Files.createTempDirectory("docupodcast-pdf-session-").toAbsolutePath().normalize();
        try {
            Files.writeString(root.resolve(SESSION_MARKER), "managed-pdf-v2\n", StandardCharsets.UTF_8);
            Path sourceDirectory = Files.createDirectories(root.resolve("source"));
            Path source = sourceDirectory.resolve(safeFileName(input.getFileName().toString()));
            Files.copy(input, source, StandardCopyOption.COPY_ATTRIBUTES);

            String sha256 = sha256(source);
            int pageCount;
            try {
                pageCount = inspectPdf.build(source, 144).pageCount();
            } catch (PdfRenderException ex) {
                throw new IOException("No se pudo inspeccionar el PDF importado.", ex);
            }
            Instant now = Instant.now();
            PdfDocumentManifest manifest = new PdfDocumentManifest(
                    PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                    title,
                    source.getFileName().toString(),
                    sha256,
                    pageCount,
                    "pdf-v2",
                    now,
                    now);
            manifest = manifest.withReadingPreferences(
                    com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy.NATIVE_TEXT,
                    com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNativeTextProvider.PDFBOX);
            repository.initialize(root, manifest);
            return new PreparedPdfSource(new PreparedPdfWorkspaceRef(root, source, sha256), title);
        } catch (IOException | RuntimeException ex) {
            deleteManagedRoot(root);
            throw ex;
        }
    }

    public void closeIfSessionWorkspace(PreparedPdfSource source) throws IOException {
        if (source == null) return;
        deleteManagedRoot(source.workspace().projectRoot());
    }

    public boolean isSessionWorkspace(PreparedPdfSource source) {
        return source != null && Files.isRegularFile(
                source.workspace().projectRoot().resolve(SESSION_MARKER));
    }

    private static void deleteManagedRoot(Path candidate) throws IOException {
        if (candidate == null) return;
        Path root = candidate.toAbsolutePath().normalize();
        if (!Files.isRegularFile(root.resolve(SESSION_MARKER))) return;
        try (var entries = Files.walk(root)) {
            for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String safeFileName(String value) {
        String normalized = value == null ? "source.pdf" : value.replace('\\', '_').replace('/', '_');
        return normalized.isBlank() ? "source.pdf" : normalized;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                for (int read; (read = input.read(buffer)) >= 0; ) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}
