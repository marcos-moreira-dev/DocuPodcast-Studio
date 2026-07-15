package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

public final class ProjectExportEligibilityPolicy {
    private static final String PDF_TITLE = "Exportacion no disponible para PDF";
    private static final String PDF_MESSAGE = "El centro de exportacion solo esta disponible para proyectos basados en "
            + "documentos Word/DOCX o texto editable preparado. Para renderizar audio o video final, DocuPodcast Studio "
            + "fuerza el trabajo sobre un guion editable con diseno creativo previo. Los PDF quedan para lectura, estudio, "
            + "OCR y capturas visuales; no son candidatos a renderizacion final.";

    public ExportEligibility evaluate(ReadableDocument document) {
        if (document != null && document.format() == SourceDocumentFormat.PDF) {
            return ExportEligibility.denied(PDF_TITLE, PDF_MESSAGE);
        }
        return ExportEligibility.allowed();
    }

    public record ExportEligibility(boolean eligible, String title, String message) {
        public static ExportEligibility allowed() {
            return new ExportEligibility(true, "", "");
        }

        public static ExportEligibility denied(String title, String message) {
            return new ExportEligibility(false, title == null ? "" : title, message == null ? "" : message);
        }
    }
}
