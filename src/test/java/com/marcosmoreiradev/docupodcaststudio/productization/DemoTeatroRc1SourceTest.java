package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DemoTeatroRc1SourceTest {
    @Test
    void teatroDemoContainsFragmentVisualsAndBindingWorkflow() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/ClasspathExampleProjectCatalog.java"));
        String creationWorkflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleProjectCreationWorkflow.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleVisualBindingWorkflow.java"));
        String configurator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/TheatreAviadoresConfigurator.java"));
        String manifest = Files.readString(Path.of("src/main/resources/examples/aviadores-comicos/PROYECTO_DEMO.md"));

        assertTrue(catalog.contains("fragmento_01_escena1_hangar_presentacion.png"));
        assertTrue(catalog.contains("fragmento_24_cierre_tornillo_dorado.png"));
        assertTrue(catalog.contains("/examples/aviadores-comicos/PROYECTO_DEMO.md"));
        assertTrue(catalog.contains("imagen_01_presentacion_personajes.png"));
        assertTrue(catalog.contains("personajes/capitan_bigote/capitan_bigote_01_frontal.png"));
        assertTrue(catalog.contains("personajes/teniente_tornillo/teniente_tornillo_01_frontal.png"));
        assertTrue(catalog.contains("utileria/avion_tornillo_dorado_01.png"));
        assertTrue(catalog.contains("utileria/obj_bidon_combustible_01.png"));
        assertTrue(catalog.contains("mapas/mapa-espacial.png"));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/fragmentos/fragmento_01_escena1_hangar_presentacion.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/fragmentos/fragmento_24_cierre_tornillo_dorado.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/mapas/mapa-espacial.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_01_frontal.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_05_trasero.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_01_frontal.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_05_trasero.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/utileria/avion_tornillo_dorado_01.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/utileria/obj_bidon_combustible_01.png")));
        assertTrue(workflow.contains("NarrativeLayerKind.IMAGE"));
        assertTrue(workflow.contains("importImageAsset().importImage"));
        assertTrue(creationWorkflow.contains("createInDirectory"));
        assertTrue(creationWorkflow.contains("example.hasTheatreMarkdown()"));
        assertTrue(creationWorkflow.contains("materializeInDirectory"));
        assertTrue(creationWorkflow.contains("configureAviadoresTheatreDemo"));
        assertTrue(configurator.contains("REQUIRED_ASSET_FILENAMES"));
        assertTrue(configurator.contains("TheatreAviadoresConfigurator"));
        assertTrue(manifest.contains("### Escena: El hangar"));
        assertTrue(manifest.contains("texto_inicio=5 | texto_fin=10"));
        assertTrue(manifest.contains("texto_inicio=12 | texto_fin=20"));
        assertTrue(manifest.contains("texto_inicio=22 | texto_fin=27"));
        assertTrue(manifest.contains("mapa_espacial=mapas/mapa-espacial.png"));
        assertTrue(manifest.contains("imagen=fragmentos/fragmento_24_cierre_tornillo_dorado.png"));
    }

    @Test
    void teatroDemoConfiguresProjectCharactersObjectsAndVoices() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String configurator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/TheatreAviadoresConfigurator.java"));

        assertTrue(viewModel.contains("configureAviadoresTheatreDemo"));
        assertTrue(viewModel.contains("currentScript.get()"));
        assertTrue(configurator.contains("existingVoiceIds"));
        assertTrue(configurator.contains("VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO"));
        assertTrue(configurator.contains("VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR"));
        assertTrue(configurator.contains("VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR"));
        assertFalse(configurator.contains("VOC-DEMO-NARRADOR"));
        assertFalse(configurator.contains("VOC-DEMO-BIGOTE"));
        assertFalse(configurator.contains("VOC-DEMO-TORNILLO"));
        assertFalse(configurator.contains("VOICE-DEMO-NARRADOR"));
        assertFalse(configurator.contains("VOICE-DEMO-BIGOTE"));
        assertFalse(configurator.contains("VOICE-DEMO-TORNILLO"));
        assertTrue(configurator.contains("CHR-CAPITAN-BIGOTE"));
        assertTrue(configurator.contains("CHR-TENIENTE-TORNILLO"));
        assertTrue(configurator.contains("OBJ-AVION"));
        assertTrue(configurator.contains("OBJ-BIDON"));
        assertTrue(configurator.contains("SCN-001"));
        assertTrue(configurator.contains("SCN-002"));
        assertTrue(configurator.contains("SCN-003"));
        assertTrue(configurator.contains("addCharImg(characterImages, \"CHR-CAPITAN-BIGOTE\""));
        assertTrue(configurator.contains("addCharImg(characterImages, \"CHR-TENIENTE-TORNILLO\""));
        assertTrue(configurator.contains("capitan_bigote_05_trasero.png"));
        assertTrue(configurator.contains("teniente_tornillo_05_trasero.png"));
        assertTrue(configurator.contains("buildEmotions"));
        assertTrue(configurator.contains("NLA-EMOTION-DEMO-"));
        assertTrue(configurator.contains("VoiceReferenceTone.DRAMATIC"));
        assertTrue(configurator.contains("VoiceReferenceTone.ENTHUSIASTIC"));
        assertTrue(configurator.contains("tone.layerTargetId()"));
    }
}
