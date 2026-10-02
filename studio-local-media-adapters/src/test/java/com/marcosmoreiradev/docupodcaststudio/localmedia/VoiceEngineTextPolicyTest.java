package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceEngineTextPolicyTest {
    @Test
    void piperPreservesItsExistingAsciiSpanishWorkaround() {
        PiperVoiceEngine engine = new PiperVoiceEngine(
                new EngineConfiguration(PiperVoiceEngine.ID, Map.of()));

        assertEquals("aeiou n AEIOU N ?Hola!",
                engine.prepareText("áéíóú ñ ÁÉÍÓÚ Ñ ¿Hola!"));
    }

    @Test
    void xttsPreservesPunctuationAndMakesKnownMathSymbolsSpeakable() {
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, Map.of()));

        assertEquals(
                "n es par si y solo si existe k pertenece a Z : n es igual a 2k. Valor 3.14.",
                engine.prepareText("n es par ⇔ ∃k ∈ Z : n = 2k. Valor 3.14..."));
    }

    @Test
    void xttsRejectsPunctuationOnlyAndNormalizesStudyText() {
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, Map.of()));

        assertEquals("", engine.prepareText("..."));
        assertEquals("Óptimo no significa automáticamente programación dinámica.",
                engine.prepareText("“Óptimo” no significa automáticamente programación dinámica.."));
    }

    @Test
    void xttsTurnsSeparatorDashesIntoNaturalPausesWithoutDamagingLexicalHyphens() {
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, Map.of()));

        assertEquals("Estructuras de datos, Fuerza bruta, Backtracking.",
                engine.prepareText("Estructuras de datos – Fuerza bruta — Backtracking."));
        assertEquals("Una idea, entre guiones, continúa.",
                engine.prepareText("Una idea —entre guiones— continúa."));
        assertEquals("teórico-práctico; 2025-2026; -5",
                engine.prepareText("teórico-práctico; 2025-2026; -5"));
    }

    @Test
    void xttsFingerprintInvalidatesTheExperimentalRoboticProfile() {
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, Map.of()));

        assertTrue(engine.acousticFingerprint().contains("coqui-native-defaults-v2"));
        assertTrue(engine.acousticFingerprint().contains("coqui-study-v3-dash-pauses"));
    }

    @Test
    void unknownEngineDefaultIsConservativeAndKeepsSpanishOrthography() {
        var engine = new TestVoiceEngine();
        assertEquals("¿El niño está bien?", engine.prepareText("  ¿El niño está bien?  "));
    }
}
