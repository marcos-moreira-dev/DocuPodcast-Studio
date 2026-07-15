package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiperIntermediateModeT118SourceTest {
    @Test
    void piperHasRealAssistantAndIsPresentedAsIntermediateMode() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String inspector = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectPiperSetupReadinessUseCase.java"));
        String selector = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/SelectPiperAsEngineUseCase.java"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ModelFolderContract.java"));

        assertTrue(settings.contains("Voz local simple"));
        assertTrue(settings.contains("Voz local simple") && settings.contains("Verificar"));
        assertTrue(settings.contains("selectPiperIfReady") || settings.contains("Usar"));
        assertTrue(services.contains("inspectPiperSetupReadiness"));
        assertTrue(services.contains("selectPiperAsEngine"));
        assertTrue(inspector.contains("RuntimeArtifactPaths"));
        assertTrue(inspector.contains("piperExecutable"));
        assertTrue(inspector.contains(".onnx.json"));
        assertTrue(selector.contains("lectura liviana"));
        assertTrue(catalog.contains("lectura local intermedia/liviana"));
    }

    @Test
    void piperDoesNotPromiseCoquiCapabilities() throws Exception {
        String all = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"))
                + Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineSetupCatalog.java"))
                + Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantCatalog.java"));

        assertFalse(all.contains("Piper — TTS promedio/liviano"));
        assertFalse(all.contains("Motor promedio/liviano"));
        assertTrue(all.contains("no habilita clonación") || all.contains("no habilita clonacion"));
        assertTrue(all.contains("emociones") || all.contains("emocion"));
    }
}
