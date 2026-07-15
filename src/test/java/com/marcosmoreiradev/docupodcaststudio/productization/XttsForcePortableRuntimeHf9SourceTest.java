package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsForcePortableRuntimeHf9SourceTest {
    @Test
    void powershellBridgesForcePortableRuntimeAndStayAsciiSafe() throws Exception {
        Path canonical = Path.of("scripts/tts/xtts-file-to-wav.ps1");
        Path localized = Path.of("scripts/tts/Voz IA avanzada-file-to-wav.ps1");

        assertPortableBridge(canonical);
        assertPortableBridge(localized);
    }

    @Test
    void javaBlocksLegacyRuntimeCommandsBeforeProcessExecution() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java"));

        assertTrue(source.contains("validateNoLegacyRuntimeCommand"));
        assertTrue(source.contains("containsBlockedLegacyRuntimePath"));
        assertTrue(source.contains("componentes locales ia avanzada-wrapper"));
        assertTrue(source.contains("recursos locales ia avanzada"));
        assertTrue(source.contains("model.pth/model.pth"));
        assertTrue(source.contains("EngineUnavailableException"));
    }

    @Test
    void pythonWrapperKeepsTorchCheckpointLoadingSelfContained() throws Exception {
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts.py"));

        assertTrue(wrapper.contains("configure_local_torch_deserialization"));
        assertTrue(wrapper.contains("kwargs.setdefault(\"weights_only\", False)"));
        assertTrue(wrapper.contains("torch.load = docupodcast_torch_load"));
        assertTrue(wrapper.contains("add_safe_globals([XttsConfig, XttsAudioConfig, XttsArgs])"));
        assertTrue(wrapper.contains("torch_load_weights_only=false"));
        assertTrue(wrapper.contains("configure_local_audio_loader"));
        assertTrue(wrapper.contains("torchaudio_load_fallback=soundfile"));
        assertTrue(wrapper.contains("utf-8-sig"));
    }

    private static void assertPortableBridge(Path path) throws Exception {
        String script = Files.readString(path);
        byte[] bytes = Files.readAllBytes(path);

        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: script="));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: python="));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: wrapper="));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: modelDir="));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: ignoredLegacyArg="));
        assertTrue(script.contains("tools\\xtts-wrapper"));
        assertTrue(script.contains("models\\tts\\xtts"));
        assertTrue(script.contains("recursos locales ia avanzada"));
        assertTrue(script.contains("componentes locales ia avanzada-wrapper"));
        assertFalse(script.contains("TrimEnd"));
        for (byte b : bytes) {
            assertTrue((b & 0xff) <= 0x7f, path + " must stay ASCII-safe");
        }
    }
}
