package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.time.Instant;

/** Canonical PDF workspace manifest; prepared page count is derived from page files. */
public record PdfDocumentManifest(
        int schemaVersion,
        String title,
        String sourceFile,
        String sourceSha256,
        int pageCount,
        String preparationSignature,
        PdfDocumentAnalysisSummary analysisSummary,
        Instant createdAt,
        Instant updatedAt,
        PdfReadingStrategy readingStrategy,
        PdfNativeTextProvider nativeTextProvider
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public PdfDocumentManifest {
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        title = normalize(title, "Documento PDF");
        sourceFile = fileName(sourceFile);
        sourceSha256 = token(sourceSha256, "sourceSha256");
        if (pageCount <= 0) throw new IllegalArgumentException("pageCount must be positive");
        preparationSignature = normalize(preparationSignature, "pdf-v2");
        analysisSummary = analysisSummary == null ? PdfDocumentAnalysisSummary.empty() : analysisSummary;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        readingStrategy = readingStrategy == null ? PdfReadingStrategy.SEMANTIC : readingStrategy;
        nativeTextProvider = nativeTextProvider == null ? PdfNativeTextProvider.PDFBOX : nativeTextProvider;
    }

    /** Missing preferences retain the previous behavior of existing documents. */
    public PdfDocumentManifest(int schemaVersion, String title, String sourceFile, String sourceSha256,
                               int pageCount, String preparationSignature,
                               PdfDocumentAnalysisSummary analysisSummary, Instant createdAt, Instant updatedAt) {
        this(schemaVersion, title, sourceFile, sourceSha256, pageCount, preparationSignature,
                analysisSummary, createdAt, updatedAt, PdfReadingStrategy.SEMANTIC, PdfNativeTextProvider.PDFBOX);
    }

    public PdfDocumentManifest withReadingPreferences(PdfReadingStrategy strategy, PdfNativeTextProvider provider) {
        return new PdfDocumentManifest(schemaVersion, title, sourceFile, sourceSha256, pageCount,
                preparationSignature, analysisSummary, createdAt, Instant.now(), strategy, provider);
    }

    public PdfDocumentManifest(int schemaVersion, String title, String sourceFile, String sourceSha256,
                               int pageCount, String preparationSignature,
                               Instant createdAt, Instant updatedAt) {
        this(schemaVersion, title, sourceFile, sourceSha256, pageCount, preparationSignature,
                PdfDocumentAnalysisSummary.empty(), createdAt, updatedAt);
    }

    private static String fileName(String value) {
        String normalized = normalize(value, "source.pdf").replace('\\', '/');
        if (normalized.contains("/") || normalized.equals(".") || normalized.equals("..")) {
            throw new IllegalArgumentException("sourceFile must be a file name");
        }
        return normalized;
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a token");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
