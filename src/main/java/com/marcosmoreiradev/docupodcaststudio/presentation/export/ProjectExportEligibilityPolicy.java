package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;

public final class ProjectExportEligibilityPolicy {
    private static final String PDF_TITLE = "Prepara el contenido PDF para exportar";
    private static final String PDF_MESSAGE = "El PDF todavía no dispone de una proyección audiovisual preparada. "
            + "Procesa su lectura para construir contenido, narración y visuales antes de abrir el centro de exportaciones.";

    public ExportEligibility evaluate(ProjectDocumentSource source) {
        return evaluate(source, false);
    }

    public ExportEligibility evaluate(ProjectDocumentSource source,
                                      boolean exportProjectionAvailable) {
        if (source instanceof PreparedPdfSource) {
            return exportProjectionAvailable
                    ? ExportEligibility.allowed()
                    : ExportEligibility.denied(PDF_TITLE, PDF_MESSAGE);
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
