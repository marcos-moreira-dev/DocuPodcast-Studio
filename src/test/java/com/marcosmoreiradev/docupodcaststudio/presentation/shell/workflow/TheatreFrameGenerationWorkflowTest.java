package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ImageGenerationWorkspaceSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.FrameGenerationMode;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreFrameGenerationWorkflowTest {
    @Test
    void singleModePlansOneFramePerIntervention() {
        TheatreFrameGenerationWorkflow workflow = new TheatreFrameGenerationWorkflow(
                WorkspaceTestServices.empty(), mediaCapabilities());

        TheatreFrameGenerationWorkflow.Estimate estimate = workflow.estimate(session(), script(),
                request(FrameGenerationMode.SINGLE, TheatreFrameGenerationScope.all()));

        assertEquals(2, estimate.interventions());
        assertEquals(2, estimate.frames());
        assertEquals(0, estimate.pending());
    }

    @Test
    void stopMotionPlansIntermediateFramesBetweenConsecutiveInterventions() {
        TheatreFrameGenerationWorkflow workflow = new TheatreFrameGenerationWorkflow(
                WorkspaceTestServices.empty(), mediaCapabilities());

        TheatreFrameGenerationWorkflow.Estimate estimate = workflow.estimate(session(), script(),
                request(FrameGenerationMode.DOUBLE_STOP_MOTION, TheatreFrameGenerationScope.all()));

        assertEquals(2, estimate.interventions());
        assertEquals(3, estimate.frames());
        assertEquals(1, estimate.pending());
    }

    @Test
    void sceneScopeFiltersFrameUnits() {
        TheatreFrameGenerationWorkflow workflow = new TheatreFrameGenerationWorkflow(
                WorkspaceTestServices.empty(), mediaCapabilities());

        TheatreFrameGenerationWorkflow.Estimate estimate = workflow.estimate(session(), script(),
                request(FrameGenerationMode.SINGLE, TheatreFrameGenerationScope.scene("SC-2")));

        assertEquals(1, estimate.interventions());
        assertEquals(1, estimate.frames());
    }

    private static TheatreFrameGenerationRequest request(FrameGenerationMode mode, TheatreFrameGenerationScope scope) {
        return new TheatreFrameGenerationRequest(scope, mode, Path.of("."), TheatreImageGenerationPreset.TEST_4GB_SD15,
                ImageGenerationWorkspaceSettings.defaults(null), false);
    }

    private static MediaCapabilityService mediaCapabilities() {
        return new MediaCapabilityService(MediaEnginePlatform.empty(), LocalResourceScheduler.safeDefaults());
    }

    private static ProjectSession session() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withTheatre(new TheatreProjectLayer(
                        List.of(
                                new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1),
                                new TheatreProjectLayer.Intervencion("INTERVENCION-2", "B0002", 2)),
                        List.of(), List.of(), List.of(), List.of(),
                        List.of(new TheatreProjectLayer.TheatreAct("ACT-1", "Acto 1", "")),
                        List.of(
                                new TheatreProjectLayer.Scene("SC-1", "Escena 1", "", "ACT-1"),
                                new TheatreProjectLayer.Scene("SC-2", "Escena 2", "", "ACT-1")),
                        List.of(), List.of(),
                        List.of(
                                new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SC-1", "NARRADOR", "centro", "centro", "", Map.of("NARRADOR", "centro")),
                                new TheatreProjectLayer.TextActionPlacement("INTERVENCION-2", "SC-2", "NARRADOR", "centro", "centro", "", Map.of("NARRADOR", "centro"))),
                        List.of(), List.of()));
        return ProjectSession.opened(project, Path.of("obra.docupodcast.json"));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-1", NarrationSegmentType.PARAGRAPH, "Uno", "NARRADOR: Primer texto.", List.of("B0001")),
                NarrationSegment.of("SEG-2", NarrationSegmentType.PARAGRAPH, "Dos", "NARRADOR: Segundo texto.", List.of("B0002"))));
    }

}
