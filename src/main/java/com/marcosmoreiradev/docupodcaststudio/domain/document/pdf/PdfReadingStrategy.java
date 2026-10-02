package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Document preference, independent of the engines installed on this computer. */
public enum PdfReadingStrategy {
    NATIVE_TEXT("Lectura directa", "Utiliza el texto que ya contiene el PDF. Es la opción más rápida."),
    NATIVE_WITH_OCR("Lectura con reconocimiento", "Recupera también texto de páginas escaneadas cuando hace falta."),
    SEMANTIC("Lectura con interpretación", "Analiza contenido complejo con las herramientas locales. Requiere más tiempo.");

    private final String label;
    private final String description;
    PdfReadingStrategy(String label, String description) {
        this.label = label;
        this.description = description;
    }
    public String description() { return description; }
    @Override public String toString() { return label; }
}
