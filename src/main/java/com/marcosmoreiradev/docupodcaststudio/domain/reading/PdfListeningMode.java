package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** Understandable presets; object-level policy remains independently editable. */
public enum PdfListeningMode {
    ESSENTIAL_READING(
            "Lectura esencial",
            "Prosa fiable y resaltado; omite objetos pendientes sin usar IA local.",
            TableNarrationPolicy.SKIP,
            ImageNarrationPolicy.IGNORE_IMAGES,
            false),
    INTELLIGENT_DOCUMENTARY(
            "Documental inteligente",
            "Prepara figuras, tablas y fórmulas como borradores revisables.",
            TableNarrationPolicy.SUMMARIZE,
            ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT,
            true),
    ADVANCED_REVIEW(
            "Revisión avanzada",
            "Muestra evidencias y deja cada generación bajo control manual.",
            TableNarrationPolicy.SUMMARIZE,
            ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT,
            false);

    private final String displayName;
    private final String description;
    private final TableNarrationPolicy tablePolicy;
    private final ImageNarrationPolicy imagePolicy;
    private final boolean automaticDrafts;

    PdfListeningMode(String displayName, String description,
                     TableNarrationPolicy tablePolicy,
                     ImageNarrationPolicy imagePolicy,
                     boolean automaticDrafts) {
        this.displayName = displayName;
        this.description = description;
        this.tablePolicy = tablePolicy;
        this.imagePolicy = imagePolicy;
        this.automaticDrafts = automaticDrafts;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public TableNarrationPolicy tablePolicy() { return tablePolicy; }
    public ImageNarrationPolicy imagePolicy() { return imagePolicy; }
    public boolean automaticDrafts() { return automaticDrafts; }

    @Override public String toString() { return displayName; }
}
