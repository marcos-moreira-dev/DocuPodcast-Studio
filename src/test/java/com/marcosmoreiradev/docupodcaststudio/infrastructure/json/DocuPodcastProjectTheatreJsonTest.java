package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectTheatreJsonTest {
    @Test
    void roundTripsOptionalTheatreLayerWithoutTurningTnIntoCharacters() throws Exception {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "SEG-001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "SEG-002")),
                List.of(new TheatreProjectLayer.CharacterProfile(
                        "CHR-HEROE",
                        "Héroe",
                        List.of("HEROE"),
                        "Detectado desde guión")),
                List.of(new TheatreProjectLayer.VoiceRoleAlias(
                        "ROLE-VILLANO",
                        "El villano",
                        "VOC-MARIO-ALONZO",
                        "CHR-HEROE",
                        "Voz real: Mario Alonzo")),
                List.of(new TheatreProjectLayer.CharacterImage(
                        "CHR-HEROE",
                        "frontal",
                        "IMG-HEROE-FRONTAL",
                        "Referencia frontal")),
                List.of(new TheatreProjectLayer.IntervencionVisual(
                        "INTERVENCION-1",
                        "IMG-T1",
                        "Imagen del primer fragmento")),
                List.of(new TheatreProjectLayer.TheatreAct(
                        "ACT-001",
                        "Acto 1",
                        "Presentacion")),
                List.of(new TheatreProjectLayer.Scene(
                        "ESC-001",
                        "Escena 1",
                        "Hangar",
                        "ACT-001",
                        "IMG-MAPA-ESC-001")),
                List.of(new TheatreProjectLayer.SpatialPosition(
                        "ESC-001",
                        "INTERVENCION-1",
                        "CHR-HEROE",
                        0.35,
                        0.75,
                        "Entrada izquierda")),
                List.of(new TheatreProjectLayer.TheatreAction(
                        "ESC-001",
                        "INTERVENCION-1",
                        "INTERVENCION-2",
                        "CHR-HEROE",
                        "Cruza al centro",
                        true)),
                List.of(new TheatreProjectLayer.TextActionPlacement(
                        "INTERVENCION-1", "ESC-001", "CHR-HEROE",
                        "centro", "hacia el publico", "Publico",
                        Map.of("CHR-HEROE", "centro"))),
                List.of(new TheatreProjectLayer.ObjectImage(
                        "OBJIMG-BRUJULA-ESC-001",
                        "OBJ-BRUJULA",
                        "ESC-001",
                        "escena",
                        "IMG-BRUJULA",
                        "Foto de continuidad del objeto")),
                List.of(new TheatreProjectLayer.TheatreObject(
                        "OBJ-BRUJULA",
                        "Brujula",
                        "Objeto de utileria para continuidad visual")));
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withTheatre(theatre);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"theatre\""));
        assertTrue(json.contains("\"intervenciones\""));
        assertTrue(json.contains("\"id\": \"INTERVENCION-1\""));
        assertTrue(json.contains("\"displayName\": \"Héroe\""));
        assertTrue(json.contains("\"voiceRoleAliases\""));
        assertTrue(json.contains("\"displayName\": \"El villano\""));
        assertTrue(json.contains("\"voiceProfileId\": \"VOC-MARIO-ALONZO\""));
        assertTrue(json.contains("\"positions\""));
        assertTrue(json.contains("\"actions\""));
        assertTrue(json.contains("\"acts\""));
        assertTrue(json.contains("\"actId\": \"ACT-001\""));
        assertTrue(json.contains("\"spatialMapAssetId\": \"IMG-MAPA-ESC-001\""));
        assertTrue(json.contains("\"objects\""));
        assertTrue(json.contains("\"objectId\": \"OBJ-BRUJULA\""));
        assertTrue(json.contains("\"displayName\": \"Brujula\""));
        assertTrue(json.contains("\"textActionPlacements\""));
        assertEquals("INTERVENCION-1", opened.theatre().intervenciones().get(0).id());
        assertEquals("SEG-001", opened.theatre().intervenciones().get(0).blockId());
        assertEquals("CHR-HEROE", opened.theatre().characters().get(0).id());
        assertEquals("ACT-001", opened.theatre().acts().get(0).id());
        assertEquals("ACT-001", opened.theatre().scenes().get(0).actId());
        assertEquals("IMG-MAPA-ESC-001", opened.theatre().scenes().get(0).spatialMapAssetId());
        assertEquals("El villano", opened.theatre().voiceRoleAliases().get(0).displayName());
        assertEquals("VOC-MARIO-ALONZO", opened.theatre().voiceRoleAliases().get(0).voiceProfileId());
        assertEquals("INTERVENCION-2", opened.theatre().actions().get(0).toAlias());
        assertEquals("IMG-BRUJULA", opened.theatre().objectImages().get(0).assetId());
        assertEquals("OBJ-BRUJULA", opened.theatre().objects().get(0).id());
        assertEquals("INTERVENCION-1", opened.theatre().textActionPlacements().get(0).intervencionId());
    }

    @Test
    void projectsWithoutTheatreLayerLoadAsEmptyTheatreLayer() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Lectura");

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertEquals(0, opened.theatre().intervenciones().size());
        assertEquals(0, opened.theatre().characters().size());
        assertEquals(0, opened.theatre().voiceRoleAliases().size());
        assertEquals(0, opened.theatre().acts().size());
        assertEquals(0, opened.theatre().positions().size());
        assertEquals(0, opened.theatre().objectImages().size());
        assertEquals(0, opened.theatre().textActionPlacements().size());
        assertEquals(0, opened.theatre().objects().size());
    }

    @Test
    void roundTripsIntermediateFramesAndLoadsLegacyProjectsWithEmptyList() throws Exception {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002")),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new TheatreProjectLayer.IntermediateFrame(
                        "INTERVENCION-1", "INTERVENCION-2", "IMG-MID", "Inferido aprobado")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withTheatre(theatre);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"intermediateFrames\""));
        assertEquals(1, opened.theatre().intermediateFrames().size());
        assertEquals("INTERVENCION-1", opened.theatre().intermediateFrames().getFirst().fromIntervencionId());
        assertEquals("INTERVENCION-2", opened.theatre().intermediateFrames().getFirst().toIntervencionId());
        assertEquals("IMG-MID", opened.theatre().intermediateFrames().getFirst().assetId());

        String legacyJson = json.replace(",\n        \"intermediateFrames\": [\n            {\n                \"fromIntervencionId\": \"INTERVENCION-1\",\n                \"toIntervencionId\": \"INTERVENCION-2\",\n                \"assetId\": \"IMG-MID\",\n                \"notes\": \"Inferido aprobado\"\n            }\n        ]", "");
        assertTrue(new DocuPodcastProjectJsonReader().read(legacyJson).theatre().intermediateFrames().isEmpty());
    }

    @Test
    void roundTripsTheatreCamerasAndStageBackdrops() throws Exception {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B0001")),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new TheatreProjectLayer.CameraReference(
                        "CERCA_CENTRO_NIVEL", "CERCA CENTRO NIVEL", "IMG-CAMERA-DEFAULT",
                        "CERCA", "CENTRO", "NIVEL", true, "Default")),
                List.of(new TheatreProjectLayer.CameraCue("INTERVENCION-1", "CERCA_CENTRO_NIVEL", "Cue")),
                List.of(new TheatreProjectLayer.StageBackdrop(
                        "BACKDROP-HANGAR", "Hangar", "IMG-BACKDROP-HANGAR", "Telon")),
                List.of(new TheatreProjectLayer.StageBackdropAssignment(
                        TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE, "SCN-001", "BACKDROP-HANGAR", "Escena")));
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withTheatre(theatre);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"cameraReferences\""));
        assertTrue(json.contains("\"cameraCues\""));
        assertTrue(json.contains("\"stageBackdrops\""));
        assertTrue(json.contains("\"stageBackdropAssignments\""));
        assertEquals("CERCA_CENTRO_NIVEL", opened.theatre().cameraReferences().getFirst().id());
        assertTrue(opened.theatre().cameraReferences().getFirst().defaultCamera());
        assertEquals("INTERVENCION-1", opened.theatre().cameraCues().getFirst().intervencionId());
        assertEquals("BACKDROP-HANGAR", opened.theatre().stageBackdrops().getFirst().id());
        assertEquals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE,
                opened.theatre().stageBackdropAssignments().getFirst().scope());
    }
}
