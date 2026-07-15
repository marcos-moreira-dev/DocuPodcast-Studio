package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class XttsReadinessSmokeFinalRepairSourceTest {
    @Test
    void readinessSmokeRepairsLegacySettingsBeforeAnyVoiceTest() throws Exception {
        String smoke = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/RunXttsReadinessSmokeUseCase.java"));
        String inspect = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java"));
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareVoiceTestSynthesisGateway.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));

        assertTrue(smoke.contains("OperationalSettingsMigrationPolicy.repair"));
        assertTrue(inspect.contains("OperationalSettingsMigrationPolicy.repair"));
        assertTrue(gateway.contains("loadAndPersistRepairedSettings"));
        assertTrue(gateway.contains("settingsRepository.save(repaired)"));
        assertTrue(advancedVoice.contains("smokeSettings"));
        assertTrue(advancedVoice.contains("smoke.run(smokeSettings"));
    }

    @Test
    void powershellAndPythonAlwaysCollapseModelPthLeafToParent() throws Exception {
        String bridge = Files.readString(Path.of("scripts/tts/Voz IA avanzada-file-to-wav.ps1"));
        String canonical = Files.readString(Path.of("scripts/tts/xtts-file-to-wav.ps1"));
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts.py"));

        assertTrue(bridge.contains("model-dir-termina-en-model-pth"));
        assertTrue(canonical.contains("model-dir-termina-en-model-pth"));
        assertTrue(bridge.contains("model-normalizado"));
        assertTrue(canonical.contains("model-normalizado"));
        assertTrue(wrapper.contains("model_dir_termina_en_model_pth"));
        assertFalse(wrapper.contains("model_dir_apunta_a_carpeta_model_pth"));
        assertTrue(wrapper.contains("Passing model.pth here makes XTTS append another model.pth internally"));
    }
}
