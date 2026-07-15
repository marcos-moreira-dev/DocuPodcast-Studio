package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class XttsLegacySettingsRepairHf2SourceTest {
    @Test
    void persistedLegacyAdvancedVoiceSettingsAreRepairedBeforeUse() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettingsMigrationPolicy.java"));
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/settings/PropertiesOperationalSettingsRepository.java"));
        String voiceGateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareVoiceTestSynthesisGateway.java"));
        String audioGateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));

        assertTrue(policy.contains("looksLikeLegacyAdvancedVoiceCommand"));
        assertTrue(policy.contains("componentes locales ia avanzada-wrapper"));
        assertTrue(policy.contains("recursos locales ia avanzada"));
        assertTrue(policy.contains("model.pth"));
        assertTrue(repository.contains("OperationalSettingsMigrationPolicy.repair"));
        assertTrue(voiceGateway.contains("OperationalSettingsMigrationPolicy.repair"));
        assertTrue(audioGateway.contains("OperationalSettingsMigrationPolicy.repair"));
    }

    @Test
    void cudaInstallScriptUsesOnlyLocalPython() throws Exception {
        String script = Files.readString(Path.of("scripts/tts/setup-xtts-pytorch-cuda.ps1"));
        String bat = Files.readString(Path.of("scripts/39-preparar-pytorch-cuda-xtts.bat"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));

        assertTrue(script.contains("tools\\xtts-wrapper\\.venv\\Scripts\\python.exe"));
        assertTrue(script.contains("No se usa Python global"));
        assertTrue(script.contains("torch.cuda.is_available"));
        assertTrue(script.contains("--no-deps"));
        assertTrue(script.contains("numpy==1.22.0"));
        assertTrue(script.contains("networkx==2.8.8"));
        assertTrue(script.contains("import TTS, numpy, networkx"));
        assertTrue(script.contains("pip\", \"check"));
        assertTrue(bat.contains("setup-xtts-pytorch-cuda.ps1"));
        assertTrue(settings.contains("39-preparar-pytorch-cuda-xtts.bat"));
        assertTrue(settings.contains("Preparar CUDA para Voz IA avanzada"));
        assertTrue(advancedVoice.contains("PrepareXttsPytorchCudaUseCase"));
        assertTrue(settings.contains("prepareXttsPytorchCuda"));
    }
}
