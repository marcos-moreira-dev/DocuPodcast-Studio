package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoquiPortablePythonOnboardingT90BSourceTest {
    @Test
    void t90bProvidesRepoLocalPythonOnboardingWithoutGlobalInstallOrWhisperScope() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("scripts/20-preparar-python-portable-coqui.bat")));
        assertTrue(Files.isRegularFile(Path.of("scripts/21-probar-coqui-xtts.bat")));
        assertTrue(Files.isRegularFile(Path.of("scripts/22-verificar-coqui-xtts-local.bat")));
        assertTrue(Files.isRegularFile(Path.of("scripts/tts/setup-xtts-portable-python.ps1")));
        assertTrue(Files.isRegularFile(Path.of("tools/xtts-wrapper/requirements-xtts.txt")));
        assertTrue(Files.isRegularFile(Path.of("tools/xtts-wrapper/check_xtts_runtime.py")));

        String setup = Files.readString(Path.of("scripts/tts/setup-xtts-portable-python.ps1"));
        String doc = Files.readString(Path.of("docs/productizacion/T90B_ONBOARDING_PYTHON_PORTABLE_COQUI.md"));
        assertTrue(setup.contains("nuget.org/api/v2/package/python"));
        assertTrue(setup.contains("tools\\python"));
        assertTrue(setup.contains("tools\\xtts-wrapper\\.venv"));
        assertTrue(doc.contains("No modifica PATH"));
        assertTrue(doc.contains("No reintroduce Whisper/STT"));
        assertFalse(setup.contains("whisper"));
    }

    @Test
    void xttsScriptExplainsHowToPrepareLocalPythonWhenMissing() throws Exception {
        String script = Files.readString(Path.of("scripts/tts/xtts-file-to-wav.ps1"));
        assertTrue(script.contains("No existe el Python local de Coqui/XTTS"));
        assertTrue(script.contains("scripts\\20-preparar-python-portable-coqui.bat"));
    }
}
