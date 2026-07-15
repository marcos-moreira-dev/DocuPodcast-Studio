package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentListenFlowStateTest {
    @Test
    void describesWordToListenFlowInUserLanguage() {
        assertEquals("1. Abre un Word/DOCX", DocumentListenFlowState.of(false, false, false, false, false, false).title());
        assertEquals("2. Lectura preparada al escuchar", DocumentListenFlowState.of(true, true, false, false, false, false).title());
        assertEquals("3. Guarda el proyecto para generar audio", DocumentListenFlowState.of(true, true, true, false, false, false).title());
        assertEquals("3. Audio listo al escuchar", DocumentListenFlowState.of(true, true, true, false, true, false).title());
        assertEquals("Preparando audio por fragmentos", DocumentListenFlowState.of(true, true, true, false, true, true).title());
        assertEquals("Listo para reproducir", DocumentListenFlowState.of(true, true, true, true, true, false).title());
        assertTrue(DocumentListenFlowState.notNarratable().detail().contains("perfil de lectura"));
    }
}
