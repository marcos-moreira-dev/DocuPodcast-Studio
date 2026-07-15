package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards productized local theatre image engine setup and explicit heavy downloads. */
final class TheatreImageEngineProductizationSourceTest {
    @Test
    void imagePackageDownloadUsesRedirectsPreflightAndUsefulAccessMessages() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadLocalTheatreImagePackageUseCase.java");
        String profile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImageModelPackageProfile.java");
        String operations = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsOperations.java");

        assertTrue(useCase.contains("followRedirects(HttpClient.Redirect.NORMAL)"));
        assertTrue(useCase.contains("public ImagePackageDownloadPreflight preflight"));
        assertTrue(useCase.contains("X-Linked-Size"));
        assertTrue(useCase.contains("401") && useCase.contains("403"));
        assertTrue(useCase.contains("GatedRepo") || useCase.contains("gated"));
        assertTrue(profile.contains("SD15_DREAMSHAPER"));
        assertTrue(profile.contains("DreamShaper_8_pruned.safetensors"));
        assertTrue(profile.contains("HIGH_QUALITY_FLUX"));
        assertTrue(profile.contains("GATED_OR_TOKEN"));
        assertTrue(profile.contains("aceptar sus terminos"));
        assertTrue(profile.contains("ae.safetensors"));
        assertTrue(profile.contains("clip_l.safetensors"));
        assertTrue(profile.contains("T5 FP8/FP16/BF16"));
        assertTrue(profile.contains("workflow FLUX viene integrado"));
        assertTrue(profile.contains("providerUrl"));
        assertTrue(profile.contains("ImageModelPackageInstallType"));
        assertTrue(operations.contains("preflight(form.toSettings(), applicationRoot)"));
        assertTrue(operations.contains("Nunca es descarga default"));
        assertTrue(operations.contains("forceReinstall"));
    }

    @Test
    void settingsCardShowsProfileSelectorAndNoTechnicalEndpointInNormalArea() throws Exception {
        String form = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java");
        String card = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsCard.java");

        assertTrue(form.contains("\"SD15_DREAMSHAPER\""));
        assertTrue(form.contains("\"HIGH_QUALITY_FLUX\""));
        assertTrue(form.contains("\"CUSTOM_COMFY_WORKFLOW\""));
        assertTrue(card.contains("addSummaryNodeRow(grid, 0, \"Perfil\", form.imagePreset)"));
        assertTrue(card.contains("Catalogo de modelos y componentes"));
        assertTrue(card.contains("resourceRow"));
        assertTrue(card.contains("settings-image-resource-row"));
        assertTrue(card.contains("Proveedor"));
        assertTrue(card.contains("profile.providerUrl()"));
        assertTrue(card.contains("profile.downloadUrl()"));
        assertTrue(card.contains("missingFluxComponents"));
        assertTrue(card.contains("FluxModelBundle"));
        assertTrue(card.contains("text_encoder_2"));
        assertTrue(card.contains("Confirmo que inicie sesion y acepte la licencia FLUX.1-dev"));
        assertTrue(card.contains("Descargar modelo"));
        assertTrue(card.contains("Reinstalar modelo"));
        assertTrue(card.contains("addSummaryNodeRow(grid, 8, \"Memoria\", form.imageMemoryProfile)"));
        assertTrue(card.contains("ImageGenerationMemoryProfile"));
        assertTrue(card.contains("Crear carpetas"));
        assertFalse(card.contains("addHeader(grid, 4, \"Acciones\")"));
        assertFalse(card.contains("setDisable(profile.downloadUrl().isBlank())"));
        assertTrue(card.contains("ImageModelPackageProfile.fromPreset(value).displayName()"));
        assertTrue(card.contains("ImageModelPackageProfile.fromPreset(item).displayName()"));
        assertTrue(card.contains("Avanzado / diagnostico"));
        assertTrue(card.contains("Endpoint local"));
    }

    @Test
    void enginesSettingsSummaryIsCompactAndRecommendedSetupDoesNotDownloadImageAutomatically() throws Exception {
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String assistant = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java");
        String initialSetup = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java");

        assertTrue(settings.contains("engineOperationalSummary"));
        assertTrue(settings.contains("Resumen operativo"));
        assertTrue(settings.contains("addEngineStatusRow(grid, 0, \"Estado\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 1, \"Motor de voz activo\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 2, \"Prueba de voz\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 3, \"Video local\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 4, \"OCR PDF local\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 5, \"Imagen IA teatral\""));
        assertTrue(settings.contains("addEngineStatusRow(grid, 6, \"Siguiente paso\""));
        assertFalse(settings.contains(".addRow(\"Camino"));
        assertFalse(settings.contains(".addRow(\"Mejora"));
        assertFalse(settings.contains(".addRow(\"Video MP4\""));
        assertFalse(settings.contains(".addRow(\"Imagen IA\","));
        assertFalse(settings.contains("page.addNode(engineStatusCard"));
        assertTrue(assistant.contains("Imagen IA teatral no se descarga aqui"));
        assertFalse(assistant.contains("downloadLocalTheatreImagePackage()"));
        assertTrue(initialSetup.contains("imageChoice.setSelected(false)"));
        assertTrue(initialSetup.contains("Confirmar descarga Prueba 4GB"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
