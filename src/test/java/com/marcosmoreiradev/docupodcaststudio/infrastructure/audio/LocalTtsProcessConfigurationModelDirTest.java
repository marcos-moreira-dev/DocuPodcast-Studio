package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalTtsProcessConfigurationModelDirTest {
    @Test
    void normalizesLegacyModelPthArgumentBeforeCallingPowerShellOrPython() {
        List<String> command = LocalTtsProcessConfiguration.normalizeXttsModelDirectoryArgument(List.of(
                "powershell", "-File", "scripts/tts/Voz IA avanzada-file-to-wav.ps1",
                "-ModelDir", "C:/Users/MOREIRA/Downloads/h/recursos locales IA avanzada/model.pth",
                "-Text", "in.txt", "-Output", "out.wav"));

        String joined = String.join("/", command).replace('\\', '/');
        assertFalse(joined.contains("model.pth/model.pth"));
        assertTrue(joined.contains("recursos locales IA avanzada"));
        assertFalse(command.contains("C:/Users/MOREIRA/Downloads/h/recursos locales IA avanzada/model.pth"));
    }
}
