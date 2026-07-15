package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedTtsJobStallFixSourceTest {
    @Test
    void localProcessRunsPythonUnbufferedAndKillsChildProcessesOnCancelOrTimeout() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        String runner = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/process/DefaultExternalProcessRunner.java"));

        assertTrue(source.contains("PYTHONUNBUFFERED"));
        assertTrue(source.contains("PYTHONIOENCODING"));
        assertTrue(source.contains("ExternalProcessRunner"));
        assertTrue(source.contains("ExternalProcessObserver"));
        assertTrue(source.contains("combinedOutputTail"));
        assertTrue(runner.contains("destroyTree"));
        assertTrue(runner.contains("process.toHandle().descendants()"));
        assertTrue(runner.contains("process.waitFor(2, TimeUnit.SECONDS)"));
    }

    @Test
    void xttsWrapperReportsPhasesAndDoesNotPassOpaqueAutoDeviceToTorch() throws Exception {
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts.py"));
        String script = Files.readString(Path.of("scripts/tts/xtts-file-to-wav.ps1"));
        String mapper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/ComputeDeviceArgumentMapper.java"));

        assertTrue(wrapper.contains("DOCUPODCAST_XTTS:"));
        assertTrue(wrapper.contains("resolve_device"));
        assertTrue(wrapper.contains("return \"cpu\""));
        assertTrue(wrapper.contains("cargando_modelo"));
        assertTrue(wrapper.contains("sintetizando"));
        assertTrue(script.contains("PYTHONUNBUFFERED"));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE: iniciando wrapper local"));
        assertTrue(mapper.contains("toProcessDeviceArgument"));
        assertTrue(mapper.contains("toCudaDeviceArgument"));
    }

    @Test
    void advancedVoiceUsesLongerTimeoutThanSimpleEngines() throws Exception {
        String aware = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));
        String select = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/SelectXttsAsEngineUseCase.java"));

        assertTrue(aware.contains("effectiveTtsTimeoutSeconds"));
        assertTrue(aware.contains("Math.max(configured, 900)"));
        assertTrue(select.contains("Math.max(current.tts().timeoutSeconds(), 900)"));
    }

    @Test
    void gatewayPublishesAdvancedVoiceProcessPhasesInsteadOfLookingFrozen() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        String aware = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));

        assertTrue(source.contains("processProgressMessage"));
        assertTrue(source.contains("tts_process_phase"));
        assertTrue(source.contains("cargando modelo local"));
        assertTrue(source.contains("GPU no disponible"));
        assertTrue(aware.contains("usesAdvancedVoiceCommand"));
        assertTrue(aware.contains("manualTtsDeviceRequested"));
    }
}
