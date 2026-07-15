package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoquiXttsAssistantT117SourceTest {
    @Test
    void settingsExposesRealCoquiAssistantActions() throws Exception {
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String advancedVoice = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java");
        String settingsSurface = settings + advancedVoice;
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java");

        assertTrue(settings.contains("Voz IA avanzada"));
        assertTrue(settings.contains("Voz IA avanzada") && settings.contains("Verificar"));
        assertTrue(settings.contains("selectXttsIfReady") || settings.contains("Usar"));
        assertTrue(settingsSurface.contains("inspectXttsSetupReadiness"));
        assertTrue(settingsSurface.contains("selectXttsAsEngine"));
        assertTrue(services.contains("InspectXttsSetupReadinessUseCase"));
        assertTrue(services.contains("SelectXttsAsEngineUseCase"));
    }

    @Test
    void coquiReadinessChecksPortablePythonWrapperModelAndNeutralVoice() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java");
        String selector = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/SelectXttsAsEngineUseCase.java");

        assertTrue(useCase.contains("RuntimeArtifactPaths"));
        assertTrue(useCase.contains("xttsPythonExecutable"));
        assertTrue(useCase.contains("xttsWrapperScript"));
        assertTrue(useCase.contains("xttsPortableSetupScript"));
        assertTrue(useCase.contains("ModelFolderContract.xttsHighQuality()"));
        assertTrue(useCase.contains("speakerWavPath"));
        assertTrue(useCase.contains("voz-por-defecto.wav"));
        assertTrue(selector.contains("xtts"));
        assertFalse(useCase.toLowerCase(java.util.Locale.ROOT).contains("python global"));
        assertFalse(useCase.toLowerCase(java.util.Locale.ROOT).contains("system path"));
    }

    @Test
    void documentationRecordsT117Contract() throws Exception {
        String doc = read("docs/productizacion/T117_ASISTENTE_REAL_COQUI_XTTS.md");
        assertTrue(doc.contains("Coqui/XTTS"));
        assertTrue(doc.contains("Python local"));
        assertTrue(doc.contains("voz neutral"));
        assertTrue(doc.contains("No usa Python global"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
