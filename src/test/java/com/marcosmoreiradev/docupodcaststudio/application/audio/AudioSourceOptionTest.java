package com.marcosmoreiradev.docupodcaststudio.application.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioSourceOptionTest {
    @Test
    void namesEachBackendWithoutLosingUnavailableEngineGuidance() {
        AudioSourceOption piper = AudioSourceOption.from(
                AudioEngineAvailability.localSimple(true, "Listo", ""));
        AudioSourceOption xtts = AudioSourceOption.from(
                AudioEngineAvailability.advanced(false, "Falta runtime", "Reparar"));
        AudioSourceOption test = AudioSourceOption.from(AudioEngineAvailability.testMode());
        AudioSourceOption qwen = AudioSourceOption.from(AudioEngineAvailability.from(
                new VoiceEngineOperationalState("qwen3-tts-local", "Qwen3-TTS local · 1.7B Q8",
                        true, true, true, true, true, "Listo", "Qwen listo",
                        java.util.List.of(), java.util.List.of())));
        AudioSourceOption manual = AudioSourceOption.from(AudioEngineAvailability.computerAudio());

        assertEquals("Voz local simple — Piper", piper.accessibleLabel());
        assertEquals("Voz IA avanzada — Coqui XTTS", xtts.accessibleLabel());
        assertEquals("Modo de prueba — Diagnóstico", test.accessibleLabel());
        assertEquals("Qwen3-TTS local · 1.7B Q8 — llama.cpp local", qwen.accessibleLabel());
        assertEquals("Audio del computador — Archivo manual", manual.accessibleLabel());
        assertTrue(piper.selectable());
        assertFalse(xtts.selectable());
        assertEquals("Falta runtime", xtts.message());
        assertEquals("Reparar", xtts.recommendedAction());
    }
}
