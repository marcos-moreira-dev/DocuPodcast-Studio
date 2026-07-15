package com.marcosmoreiradev.docupodcaststudio.domain.assignment;

/**
 * Kind of production layer assigned to a text range. These layers are stored in the
 * DocuPodcast project, not written back into the source Word/DOCX.
 */
public enum NarrativeLayerKind {
    VOICE("Voz principal", true, false),
    HUMAN_AUDIO("Audio principal", true, false),
    EMOTION("Emoción / intención", false, true),
    IMAGE("Imagen principal", false, true),
    BRIDGE_IMAGE("Imagen puente", false, true),
    AMBIENT_AUDIO("Audio ambiente", false, true),
    NOTE("Nota de producción", false, true);

    private final String displayName;
    private final boolean primaryNarrationLayer;
    private final boolean stackableLayer;

    NarrativeLayerKind(String displayName, boolean primaryNarrationLayer, boolean stackableLayer) {
        this.displayName = displayName;
        this.primaryNarrationLayer = primaryNarrationLayer;
        this.stackableLayer = stackableLayer;
    }

    public String displayName() {
        return displayName;
    }

    public boolean primaryNarrationLayer() {
        return primaryNarrationLayer;
    }

    public boolean stackableLayer() {
        return stackableLayer;
    }
}
