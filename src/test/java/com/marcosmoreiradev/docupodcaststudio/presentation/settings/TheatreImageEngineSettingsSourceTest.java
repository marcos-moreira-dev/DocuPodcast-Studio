package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreImageEngineSettingsSourceTest {
    @Test
    void settingsExposesOperativeTheatreImageEngineCard() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String card = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsCard.java");
        String operations = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsOperations.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java");

        assertTrue(dialog.contains("imageEngineSettingsCard.create"));
        assertTrue(card.contains("Imagen IA teatral local"));
        assertTrue(card.contains("Verificar todo"));
        assertTrue(card.contains("Crear carpetas"));
        assertTrue(card.contains("Iniciar motor"));
        assertTrue(card.contains("Detener motor"));
        assertTrue(card.contains("Probar generacion"));
        assertTrue(card.contains("Avanzado / diagnostico"));
        assertTrue(card.contains("settings-image-resource-row"));
        assertTrue(card.contains("settings-image-resource-actions"));
        assertTrue(card.contains("resourceRow(\"Runtime local\""));
        assertTrue(card.contains("Identidad SD 1.5 / IP-Adapter Plus"));
        assertTrue(card.contains("workflow-sd15-ipadapter-reference-api.json"));
        assertTrue(card.contains("Identidad Flux / PuLID-FLUX"));
        assertTrue(card.contains("workflow-flux-pulid-reference-api.json"));
        assertTrue(card.contains("Boceto estructural / ControlNet"));
        assertTrue(card.contains("no define estilo"));
        assertFalse(card.contains("addHeader(grid, 4, \"Acciones\")"));
        assertFalse(card.contains("setDisable(profile.downloadUrl().isBlank())"));
        assertTrue(operations.contains("confirmAndDownloadPackage"));
        assertTrue(operations.contains("startEngine"));
        assertTrue(operations.contains("stopEngine"));
        assertTrue(operations.contains("runDetailed"));
        assertTrue(services.contains("InspectLocalTheatreImageSetupReadinessUseCase"));
        assertTrue(services.contains("InspectLocalTheatreImageEngineUseCase"));
        assertTrue(services.contains("StartLocalTheatreImageEngineUseCase"));
        assertTrue(services.contains("StopLocalTheatreImageEngineUseCase"));
        assertTrue(services.contains("DownloadLocalTheatreImagePackageUseCase"));
    }

    @Test
    void recommendedSetupWarnsButDoesNotDownloadHeavyImagePackage() throws IOException {
        String assistant = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java");
        String setupBody = assistant.substring(assistant.indexOf("private void runRecommendedSetup"));
        assertTrue(assistant.contains("Imagen IA teatral no se descarga aqui"));
        assertTrue(assistant.contains("requiere confirmacion aparte"));
        assertFalse(setupBody.contains("downloadLocalTheatreImagePackage()"));
        assertFalse(setupBody.contains("prepareLocalTheatreImageRuntime()"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
