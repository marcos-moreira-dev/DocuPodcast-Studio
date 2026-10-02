package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class Qwen3TtsEngineAdministrationTest {
    @TempDir Path temporary;

    @Test
    void publishesOneManagedPinnedQ8InstallationThroughTheSharedAdministrationContract() throws Exception {
        Qwen3TtsEngineAdministration administration = administration();

        EngineActionDescriptor install = administration.actions().stream()
                .filter(action -> EngineActionId.INSTALL.equals(action.id())).findFirst().orElseThrow();
        ManagedDownloadPreflight preflight = administration.inspectDownload(
                new EngineActionRequest(Qwen3TtsVoiceEngine.ID, EngineActionId.INSTALL, Map.of()));

        assertEquals("true", install.metadata().get("managedDownload"));
        assertEquals(Qwen3TtsEngineAdministration.TOTAL_DOWNLOAD_BYTES, install.approximateBytes());
        assertTrue(install.metadata().get("license").contains("Apache-2.0"));
        assertEquals(ManagedDownloadState.MISSING, preflight.state());
        assertTrue(preflight.expectedSha256().contains(Qwen3TtsEngineAdministration.MODEL_SHA256));
        assertTrue(preflight.source().contains(Qwen3TtsEngineAdministration.MODEL_REVISION));
    }

    @Test
    void manualModelImportRejectsBf16BeforePublishingAnything() throws Exception {
        Qwen3TtsEngineAdministration administration = administration();
        Path bf16 = Files.write(temporary.resolve("Qwen3-TTS-1.7B-BF16.gguf"), new byte[]{1});
        Path codec = Files.write(temporary.resolve("mmproj-Qwen3-TTS-1.7B-Q8_0.gguf"), new byte[]{1});
        EngineActionId importId = new EngineActionId("import-q8-model");

        IOException failure = assertThrows(IOException.class, () -> administration.execute(
                new EngineActionRequest(Qwen3TtsVoiceEngine.ID, importId,
                        Map.of("modelFile", bf16.toString(), "codecFile", codec.toString())),
                ExecutionContext.defaults("qwen-import")));

        assertTrue(failure.getMessage().contains("Q8_0"));
        assertFalse(Files.exists(temporary.resolve(
                "models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf")));
    }

    private Qwen3TtsEngineAdministration administration() {
        Path executable = temporary.resolve("tools/qwen3-tts/llama.cpp/llama-tts.exe");
        Path model = temporary.resolve("models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf");
        Path codec = temporary.resolve("models/tts/qwen3-tts/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf");
        Path speaker = temporary.resolve("samples/neutral.wav");
        RuntimeAssetCatalog assets = new RuntimeAssetCatalog(temporary, Map.of(
                Qwen3TtsVoiceEngine.ID, Map.of("executable", executable, "model", model,
                        "codec", codec, "speaker", speaker)));
        Qwen3TtsVoiceEngine engine = new Qwen3TtsVoiceEngine(new EngineConfiguration(
                Qwen3TtsVoiceEngine.ID, Map.of("executable", executable.toString(),
                "model", model.toString(), "codec", codec.toString(),
                "defaultSpeaker", speaker.toString())), () -> "auto");
        return new Qwen3TtsEngineAdministration(engine, assets, ignored -> Long.MAX_VALUE);
    }
}
