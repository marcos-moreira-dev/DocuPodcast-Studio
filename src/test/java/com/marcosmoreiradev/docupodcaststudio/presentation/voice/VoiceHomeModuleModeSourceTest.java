package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceHomeModuleModeSourceTest {
    @Test
    void homeIsASelectionSurfaceWithoutNextStepPanel() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertTrue(source.contains("Consulta qué voces puede usar Documento"));
        assertTrue(source.contains("Documento usa voces listas"));
        assertFalse(source.contains("homeNextStepPanel"));
        assertFalse(source.contains("section(\"Siguiente paso\")"));
        assertFalse(source.contains("Documento asigna · Voces prepara"));
    }

    @Test
    void selectedVoiceDetailFollowsTheSelectedVoiceMode() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertTrue(source.contains("VoiceProfilePresentationPolicy.displayName(voice)"));
        assertTrue(source.contains("VoiceProfilePresentationPolicy.simpleVoice(voice)"));
        assertTrue(source.contains("voice-home-selection-panel"));
        assertTrue(source.contains("voiceBrowser.setPrefHeight(280)"));
        assertTrue(source.contains("voiceBrowser.setMaxHeight(Double.MAX_VALUE)"));
        assertTrue(source.contains("homeManageVoiceDisabled"));
        assertTrue(source.contains("edit.disableProperty().bind"));
        assertTrue(source.contains("VoiceProfilePresentationPolicy.predefinedVoice(selected)"));
        assertTrue(source.contains("Voz simple lista para lectura neutral"));
        assertTrue(source.contains("No hay pasos de preparación para esta voz desde Inicio"));
        assertTrue(source.contains("Neutral prediseñada disponible"));
        assertTrue(source.contains("Neutral registrada. Las emociones adicionales son opcionales"));
    }

    @Test
    void voiceRowsTreatSimpleVoicesAsReadyWithoutSamples() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceListItemView.java"));

        assertTrue(source.contains("static boolean simpleVoice"));
        assertTrue(source.contains("VoiceProfilePresentationPolicy.simpleVoice(voice)"));
        assertTrue(source.contains("VoiceProfilePresentationPolicy.displayName(voice)"));
        assertTrue(source.contains("case \"Neutral\" -> \"voice-list-status-ready\""));
    }
}
