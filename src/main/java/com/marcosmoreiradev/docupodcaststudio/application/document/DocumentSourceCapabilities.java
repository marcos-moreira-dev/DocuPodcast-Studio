package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes what the imported source can actually support.
 * The Document workspace stays unified, but Word/PDF/TXT/Markdown must not pretend
 * to expose identical operations.
 */
public record DocumentSourceCapabilities(
        SourceDocumentFormat format,
        Set<DocumentSourceCapability> capabilities,
        String operationalSummary
) {
    public DocumentSourceCapabilities {
        format = Objects.requireNonNullElse(format, SourceDocumentFormat.UNKNOWN);
        capabilities = capabilities == null || capabilities.isEmpty()
                ? Set.of()
                : Set.copyOf(capabilities);
        operationalSummary = operationalSummary == null ? "" : operationalSummary.strip();
    }

    public boolean supports(DocumentSourceCapability capability) {
        return capabilities.contains(capability);
    }

    public boolean supportsRichWordStructure() {
        return supports(DocumentSourceCapability.HIERARCHICAL_OUTLINE)
                && supports(DocumentSourceCapability.PRECISE_PARAGRAPH_STYLES);
    }

    public boolean supportsEmbeddedSourceVisuals() {
        return supports(DocumentSourceCapability.EMBEDDED_SOURCE_VISUALS);
    }

    public boolean supportsVisualPageRendering() {
        return supports(DocumentSourceCapability.VISUAL_PAGE_RENDERING);
    }

    public static DocumentSourceCapabilities forFormat(SourceDocumentFormat format) {
        SourceDocumentFormat resolved = Objects.requireNonNullElse(format, SourceDocumentFormat.UNKNOWN);
        return switch (resolved) {
            case DOCX -> new DocumentSourceCapabilities(resolved,
                    EnumSet.of(DocumentSourceCapability.READABLE_TEXT,
                            DocumentSourceCapability.HIERARCHICAL_OUTLINE,
                            DocumentSourceCapability.EMBEDDED_SOURCE_VISUALS,
                            DocumentSourceCapability.PRECISE_PARAGRAPH_STYLES),
                    "Word/DOCX ofrece la ruta documental mas rica: títulos, estilos, imágenes fuente y bloques narrables.");
            case MARKDOWN -> new DocumentSourceCapabilities(resolved,
                    EnumSet.of(DocumentSourceCapability.READABLE_TEXT,
                            DocumentSourceCapability.HIERARCHICAL_OUTLINE),
                    "Markdown permite estructura por encabezados y texto narrable; los visuales se gestionan como assets del proyecto.");
            case PDF -> new DocumentSourceCapabilities(resolved,
                    EnumSet.of(DocumentSourceCapability.READABLE_TEXT,
                            DocumentSourceCapability.NATIVE_TEXT_REQUIRED,
                            DocumentSourceCapability.VISUAL_PAGE_RENDERING),
                    "PDF V1 conserva texto nativo cuando existe y ahora ofrece render visual embebido por pagina; OCR sigue fuera.");
            case TXT -> new DocumentSourceCapabilities(resolved,
                    EnumSet.of(DocumentSourceCapability.READABLE_TEXT,
                            DocumentSourceCapability.LINEAR_TEXT_ONLY),
                    "Texto plano ofrece lectura lineal sin jerarquía ni visuales fuente.");
            case UNKNOWN -> new DocumentSourceCapabilities(resolved, Set.of(),
                    "Formato no reconocido; no se anuncian capacidades operativas.");
        };
    }
}
