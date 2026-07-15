package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF2A guardrail: advanced AI voice setup must be local, guided and stricter than a folder-only check. */
final class VoiceAiPortableRuntimePf2ASourceTest {
    @Test
    void guidedSetupCanUseLocalTtsRepositoryAndDoesNotRequireGlobalPython() throws Exception {
        String setup = Files.readString(Path.of("scripts/tts/setup-xtts-portable-python.ps1"));
        String oneClick = Files.readString(Path.of("scripts/30-preparar-voz-ia-avanzada-local.bat"));

        assertTrue(setup.contains("[string]$LocalTtsRepo"));
        assertTrue(setup.contains("pip install $repoPath"));
        assertTrue(setup.contains("pyproject.toml"));
        assertTrue(setup.contains("setup.py"));
        assertTrue(setup.contains("tools\\python"));
        assertTrue(Files.exists(Path.of("scripts/30-preparar-voz-ia-avanzada-local.bat")));
        assertTrue(oneClick.contains("scripts\\20-preparar-python-portable-coqui.bat -LocalTtsRepo"));
        assertTrue(oneClick.contains("scripts\\22-verificar-coqui-xtts-local.bat"));
        assertTrue(oneClick.contains("scripts\\21-probar-coqui-xtts.bat"));
    }

    @Test
    void runtimeCheckerRequiresConcreteLocalXttsFiles() throws Exception {
        String checker = Files.readString(Path.of("tools/xtts-wrapper/check_xtts_runtime.py"));
        String config = Files.readString(Path.of("scripts/04-verificar-tts-config.bat"));

        assertTrue(checker.contains("config.json"));
        assertTrue(checker.contains("model.pth"));
        assertTrue(checker.contains("vocab.json"));
        assertTrue(checker.contains("Modelo XTTS local incompleto"));
        assertTrue(config.contains("operational-settings.properties"));
        assertTrue(config.contains("Modo seleccionado"));
        assertTrue(config.contains("Voz IA avanzada seleccionada"));
        assertFalse(config.contains("La app usara el motor mock integrado"));
    }
}
