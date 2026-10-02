package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A normalized block extracted from DOCX/PDF/Markdown/TXT. */
public record DocumentBlock(
        String id,
        DocumentBlockType type,
        String text,
        String originalStyle,
        Map<String, String> metadata
) {
    public DocumentBlock {
        id = requireToken(id, "id");
        type = Objects.requireNonNullElse(type, DocumentBlockType.PARAGRAPH);
        text = text == null ? "" : text.strip();
        originalStyle = originalStyle == null ? "" : originalStyle.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static DocumentBlock of(String id, DocumentBlockType type, String text, String originalStyle) {
        return new DocumentBlock(id, type, text, originalStyle, Map.of());
    }

    public static DocumentBlock of(String id, DocumentBlockType type, String text, String originalStyle, Map<String, String> metadata) {
        return new DocumentBlock(id, type, text, originalStyle, metadata);
    }

    public boolean narratable() {
        String decision = metadata.getOrDefault("narratability", "").strip();
        if ("NON_NARRATABLE".equalsIgnoreCase(decision)
                || "UNCERTAIN".equalsIgnoreCase(decision)) {
            return false;
        }
        return type.narratableByDefault() && !text.isBlank();
    }

    public boolean sourceVisual() {
        return type.sourceVisual() || Boolean.parseBoolean(metadata.getOrDefault("visualBlock", "false"));
    }

    public boolean nonNarratableSourceVisual() {
        return sourceVisual() && !narratable();
    }

    public boolean structural() {
        return type.structural();
    }

    public boolean manuallyOverridden() {
        return Boolean.parseBoolean(metadata.getOrDefault("manualOverride", "false"));
    }

    public DocumentBlock withType(DocumentBlockType newType, String reason) {
        return withType(newType, reason, true);
    }

    public DocumentBlock withProfileType(DocumentBlockType newType, String profileName) {
        Map<String, String> next = mutableMetadata();
        next.put("readingProfile", profileName == null || profileName.isBlank() ? "default" : profileName.strip());
        next.put("classificationSource", "reading-profile");
        return new DocumentBlock(id, Objects.requireNonNull(newType, "newType"), text, originalStyle, next);
    }

    public DocumentBlock withType(DocumentBlockType newType, String reason, boolean manual) {
        Map<String, String> next = mutableMetadata();
        if (manual) {
            next.put("manualOverride", "true");
            next.put("manualOverrideReason", reason == null || reason.isBlank() ? "user-action" : reason.strip());
            next.put("narratability",
                    newType.narratableByDefault() ? "NARRATABLE" : "NON_NARRATABLE");
            next.put("narratabilitySource", "manual-override");
        }
        next.put("previousType", type.name());
        return new DocumentBlock(id, Objects.requireNonNull(newType, "newType"), text, originalStyle, next);
    }

    public int wordCount() {
        if (text.isBlank()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    public String preview(int maxCharacters) {
        if (text.length() <= maxCharacters) {
            return text;
        }
        return text.substring(0, Math.max(0, maxCharacters)).strip() + "…";
    }


    public String sourceLocatorLabel(SourceDocumentFormat format) {
        String explicit = metadata.getOrDefault("sourceLocatorLabel", "").strip();
        if (!explicit.isBlank()) {
            return explicit;
        }
        String page = metadata.getOrDefault("sourcePage", "").strip();
        if (!page.isBlank()) {
            return "Página " + page;
        }
        String lineStart = metadata.getOrDefault("sourceLineStart", "").strip();
        String lineEnd = metadata.getOrDefault("sourceLineEnd", "").strip();
        if (!lineStart.isBlank()) {
            if (!lineEnd.isBlank() && !lineEnd.equals(lineStart)) {
                return "Líneas " + lineStart + "–" + lineEnd;
            }
            return "Línea " + lineStart;
        }
        SourceDocumentFormat safeFormat = format == null ? SourceDocumentFormat.UNKNOWN : format;
        return switch (safeFormat) {
            case DOCX -> "Word/DOCX · bloque " + id + " (paginación dinámica)";
            case PDF -> "PDF · bloque " + id + " (página no disponible en extractor V1)";
            case TXT, MARKDOWN -> "Bloque " + id;
            default -> "Bloque " + id;
        };
    }

    public Optional<String> metadataValue(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String value = metadata.get(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private Map<String, String> mutableMetadata() {
        return new LinkedHashMap<>(metadata);
    }

    private static String requireToken(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}
