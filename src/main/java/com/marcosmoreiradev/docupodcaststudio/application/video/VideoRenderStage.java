package com.marcosmoreiradev.docupodcaststudio.application.video;

/** Stages displayed while the main workspace is blocked for video rendering. */
public enum VideoRenderStage {
    IDLE("Sin render activo"),
    PREPARING("Preparando unidades"),
    BUILDING_FRAMES("Componiendo mapas"),
    RENDERING_WITH_FFMPEG("Codificando clips"),
    MIXING_TRACKS("Mezclando pistas"),
    VERIFYING_OUTPUT("Finalizando"),
    COMPLETED("Video terminado"),
    FAILED("Render falló"),
    CANCELLED("Render cancelado");

    private final String label;

    VideoRenderStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
