package com.marcosmoreiradev.docupodcaststudio.application.theatre;

/** How many theatrical still frames should be produced for each intervention slot. */
public enum FrameGenerationMode {
    SINGLE("1 frame por intervencion", 1),
    DOUBLE_STOP_MOTION("Intermedios entre intervenciones", 2);

    private final String displayName;
    private final int framesPerIntervention;

    FrameGenerationMode(String displayName, int framesPerIntervention) {
        this.displayName = displayName;
        this.framesPerIntervention = framesPerIntervention;
    }

    public String displayName() {
        return displayName;
    }

    public int framesPerIntervention() {
        return framesPerIntervention;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
