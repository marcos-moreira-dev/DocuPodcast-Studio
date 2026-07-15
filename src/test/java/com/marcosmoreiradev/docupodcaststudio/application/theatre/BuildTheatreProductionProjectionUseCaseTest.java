package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.BuildFragmentWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreProductionProjectionUseCaseTest {
    private final BuildTheatreProductionProjectionUseCase useCase = new BuildTheatreProductionProjectionUseCase();
    private final BuildFragmentWorkspaceProjectionUseCase fragmentUseCase = new BuildFragmentWorkspaceProjectionUseCase();

    @Test
    void buildsOperationalProjectionWithFragmentAudioVisualSceneCharactersAndObjects() {
        ReadableDocument document = document();
        NarrationScriptDocument script = script();
        DocuPodcastProject project = theatreProject();
        AudioJobSnapshot job = completedJob("SEG-001");
        FragmentWorkspaceProjection fragments = fragmentUseCase.build(document, script, project, null, List.of(job), PlaybackManifest.empty());

        TheatreProductionProjection projection = useCase.build(
                project,
                document,
                script,
                fragments,
                List.of(job),
                PlaybackManifest.empty());

        assertEquals("Obra", projection.title());
        assertEquals(1, projection.scenes().size());
        assertEquals(2, projection.interventions().size());
        assertEquals(2, projection.readiness().interventionCount());
        assertEquals(2, projection.readiness().linkedInterventionCount());
        assertEquals(1, projection.readiness().audioReadyCount());
        assertEquals(1, projection.readiness().visualReadyCount());
        assertEquals(1, projection.readiness().characterCount());
        assertEquals(1, projection.readiness().objectCount());

        TheatreProductionIntervention first = projection.interventionById("INTERVENCION-1").orElseThrow();
        assertEquals(FragmentId.fromBlockId("B001"), first.fragmentId());
        assertEquals("SEG-001", first.segmentId());
        assertEquals("SCN-001", first.sceneId());
        assertEquals("CHR-PILOTO", first.characterId());
        assertEquals("Piloto", first.characterName());
        assertTrue(first.fragmentLinked());
        assertTrue(first.audioReady());
        assertTrue(first.visualReady());
        assertTrue(first.hasTextPlacement());
        assertTrue(first.hasPosition());
        assertTrue(first.hasAction());

        TheatreProductionScene scene = projection.sceneById("SCN-001").orElseThrow();
        assertEquals(2, scene.interventionCount());
        assertEquals(1, scene.audioReadyCount());
        assertEquals(1, scene.visualReadyCount());
        assertTrue(scene.hasTextPlacements());
        assertTrue(scene.hasPositions());
        assertTrue(scene.hasActions());
    }

    @Test
    void reportsBrokenReferencesAndOrphanInterventionsWithoutChangingTheatreSchema() {
        ReadableDocument document = document();
        NarrationScriptDocument script = script();
        TheatreProjectLayer brokenTheatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B404")),
                List.of(),
                List.of(new TheatreProjectLayer.VoiceRoleAlias("ROLE-MISSING", "Fantasma", "VOC-1", "CHR-MISSING", "")),
                List.of(new TheatreProjectLayer.CharacterImage("IMGREF-1", "CHR-MISSING", "SCN-MISSING", "frontal", "IMG-MISSING", "")),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-99", "IMG-MISSING", "")),
                List.of(),
                List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", "ACT-MISSING", "IMG-MAP-MISSING")),
                List.of(new TheatreProjectLayer.SpatialPosition("SCN-MISSING", "INTERVENCION-1", "CHR-MISSING", 0.5, 0.5, "")),
                List.of(new TheatreProjectLayer.TheatreAction("SCN-MISSING", "INTERVENCION-1", "INTERVENCION-2", "CHR-MISSING", "Cruza", true)),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-99", "SCN-MISSING", "CHR-MISSING", "", "", "", Map.of())),
                List.of(new TheatreProjectLayer.ObjectImage("OBJIMG-1", "OBJ-MISSING", "SCN-MISSING", "frontal", "IMG-MISSING", "")),
                List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Rota", ProjectMode.THEATRE_PRODUCTION)
                .withTheatre(brokenTheatre);
        FragmentWorkspaceProjection fragments = fragmentUseCase.build(document, script, project, null, List.of(), PlaybackManifest.empty());

        TheatreProductionProjection projection = useCase.build(
                project,
                document,
                script,
                fragments,
                List.of(),
                PlaybackManifest.empty());

        assertEquals("B404", project.theatre().intervenciones().getFirst().blockId());
        assertEquals(1, projection.readiness().orphanInterventionCount());
        assertFalse(projection.readiness().workExportable());
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("no resuelve un FragmentId")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("intervencion inexistente INTERVENCION-99")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("asset inexistente IMG-MISSING")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("personaje inexistente CHR-MISSING")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("escena inexistente SCN-MISSING")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("acto inexistente ACT-MISSING")));
        assertTrue(projection.diagnostics().stream().anyMatch(text -> text.contains("objeto inexistente OBJ-MISSING")));
    }

    @Test
    void linkPolicyResolvesBothDirectionsThroughFragmentId() {
        DocuPodcastProject project = theatreProject();
        FragmentWorkspaceProjection fragments = fragmentUseCase.build(document(), script(), project, null, List.of(), PlaybackManifest.empty());
        TheatreFragmentLinkPolicy policy = new TheatreFragmentLinkPolicy();

        TheatreFragmentLink link = policy.linkForIntervention(project.theatre(), project.theatre().intervenciones().getFirst(), fragments);

        assertEquals("INTERVENCION-1", link.interventionId());
        assertEquals("B001", link.blockId());
        assertEquals(FragmentId.fromBlockId("B001"), link.fragmentId());
        assertEquals("SEG-001", link.segmentId());
        assertEquals("SCN-001", link.sceneId());
        assertTrue(link.linked());
        assertEquals("INTERVENCION-1", policy.interventionForFragment(project.theatre(), FragmentId.fromBlockId("B001")).orElseThrow().id());
        assertEquals("SCN-001", policy.sceneForFragment(project.theatre(), FragmentId.fromBlockId("B001")).orElseThrow());
    }

    @Test
    void mapReadinessRequiresPlacementsAndPositionsOrActions() {
        TheatreProjectLayer noMapData = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B001")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", "ACT-001", "")),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-001", "", "centro", "", "", Map.of())),
                List.of(),
                List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Sin mapa", ProjectMode.THEATRE_PRODUCTION)
                .withTheatre(noMapData);
        AudioJobSnapshot job = completedJob("SEG-001");
        FragmentWorkspaceProjection fragments = fragmentUseCase.build(document(), script(), project, null, List.of(job), PlaybackManifest.empty());

        TheatreProductionProjection projection = useCase.build(project, document(), script(), fragments, List.of(job), PlaybackManifest.empty());

        assertTrue(projection.readiness().workExportable());
        assertFalse(projection.readiness().spatialMapExportable());
        assertTrue(projection.readiness().spatialMapMissingRequirements().stream()
                .anyMatch(text -> text.contains("posiciones o acciones")));
    }

    private static ReadableDocument document() {
        return new ReadableDocument(
                "Guion",
                SourceDocumentFormat.TXT,
                Path.of("guion.txt"),
                List.of(
                        DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "PILOTO: Preparar motores", "txt"),
                        DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH, "PILOTO: Despegar", "txt")));
    }

    private static NarrationScriptDocument script() {
        return new NarrationScriptDocument(
                "SCRIPT-001",
                "Guion",
                "es",
                "guion.txt",
                List.of(
                        NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intervencion 1", "Preparar motores", List.of("B001")),
                        NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Intervencion 2", "Despegar", List.of("B002"))),
                Instant.EPOCH,
                Instant.EPOCH,
                "");
    }

    private static DocuPodcastProject theatreProject() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B002")),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-PILOTO", "Piloto", List.of("PILOTO"), "")),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-I1", "Visual listo")),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", "ACT-001", "")),
                List.of(new TheatreProjectLayer.SpatialPosition("SCN-001", "INTERVENCION-1", "CHR-PILOTO", 0.4, 0.5, "centro")),
                List.of(new TheatreProjectLayer.TheatreAction("SCN-001", "INTERVENCION-1", "INTERVENCION-2", "CHR-PILOTO", "Habla al publico", true)),
                List.of(
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-001", "CHR-PILOTO", "centro", "publico", "Publico", Map.of()),
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-2", "SCN-001", "CHR-PILOTO", "centro", "publico", "Publico", Map.of())),
                List.of(new TheatreProjectLayer.ObjectImage("OBJIMG-1", "OBJ-AVION", "SCN-001", "frontal", "IMG-I1", "")),
                List.of(new TheatreProjectLayer.TheatreObject("OBJ-AVION", "Avion", "")));
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION)
                .withAsset(new ProjectAssetReference("IMG-I1", ProjectAssetKind.IMAGE, "Intervencion 1",
                        "assets/images/i1.png", "image/png", "Visual teatral", "", ""))
                .withTheatre(theatre);
    }

    private static AudioJobSnapshot completedJob(String segmentId) {
        AudioSegmentSnapshot segment = AudioSegmentSnapshot.pending(segmentId, "Intervencion")
                .completed("jobs/JOB-001/" + segmentId + ".wav", 2.5);
        return new AudioJobSnapshot(
                "JOB-001",
                "Guion",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1.0,
                segmentId,
                "Intervencion",
                0,
                "OK",
                "jobs/JOB-001",
                "",
                "jobs/JOB-001/manifest.json",
                List.of(segment),
                Instant.EPOCH,
                Instant.EPOCH);
    }
}
