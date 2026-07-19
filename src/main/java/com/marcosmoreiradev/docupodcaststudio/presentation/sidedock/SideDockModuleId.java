package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

/** Stable identifiers for workspace side-dock modules. */
public enum SideDockModuleId {
    DOCUMENT_CONTEXT_DETAILS("Fragmento"),
    DOCUMENT_AUDIO_NARRATION("Audio"),
    DOCUMENT_TECHNICAL_PROBLEM("Problema"),
    DOCUMENT_STUDY_VIDEO("Video documental"),
    NARRATIVE_VIDEO_CONTENT("Contenido del video"),
    DOCUMENT_IMAGE("Imagen"),
    THEATRE_FRAGMENT_IMAGES("Capas multimedia"),
    THEATRE_CHARACTERS("Personajes"),
    THEATRE_TEXTUAL_MAP("Mapa textual"),
    THEATRE_SPATIAL_MAP("Mapa espacial"),
    THEATRE_ACTIONS("Acciones"),
    THEATRE_OBJECTS("Objetos"),
    DOCUMENT_INDEX("Índice"),
    DOCUMENT_STRUCTURE("Estructura"),
    DOCUMENT_PROPERTIES("Propiedades"),
    DOCUMENT_ACTIONS("Acciones"),
    READING_PROFILE("Perfil"),
    DOCUMENT_DIAGNOSTICS("Diagnóstico"),
    OPERATIONAL_HELP("Ayuda");

    private final String displayName;

    SideDockModuleId(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
