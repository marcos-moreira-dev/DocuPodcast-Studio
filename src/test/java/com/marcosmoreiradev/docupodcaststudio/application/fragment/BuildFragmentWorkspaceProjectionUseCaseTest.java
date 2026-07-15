package com.marcosmoreiradev.docupodcaststudio.application.fragment;

import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildFragmentWorkspaceProjectionUseCaseTest {
    private final BuildFragmentWorkspaceProjectionUseCase useCase = new BuildFragmentWorkspaceProjectionUseCase();

    @Test
    void projectsNarratableDocumentBlocksAudioStoryboardAndTheatreByFragmentId() {
        ReadableDocument document = document();
        NarrationScriptDocument script = new BuildNarrationScriptUseCase().build(document, "es");
        assertEquals("FRG-B001", script.segments().getFirst().metadata().get("fragmentId"));

        DocuPodcastProject project = projectWithImageAndTheatre();
        StoryboardDocument storyboard = storyboard(script);
        AudioJobSnapshot job = completedAudioJob(script.segments().getFirst().id());
        PlaybackManifest manifest = playbackManifest(script.segments().getFirst().id());

        FragmentWorkspaceProjection projection = useCase.build(
                document,
                script,
                project,
                storyboard,
                List.of(job),
                manifest);

        assertEquals(1, projection.fragments().size());
        DocumentFragment spoken = projection.fragmentForBlock("B001").orElseThrow();
        assertEquals(FragmentId.fromBlockId("B001"), spoken.fragmentId());
        assertEquals("SEG-001", spoken.segmentId());
        assertEquals(FragmentStatus.AUDIO_READY, spoken.status());
        assertEquals("FRG-B001", spoken.metadata().get("fragmentId"));

        assertTrue(projection.fragmentForBlock("B002").isEmpty());

        var spokenBindings = projection.bindingsForFragment(FragmentId.fromBlockId("B001"));
        assertTrue(spokenBindings.stream().anyMatch(binding -> binding.role() == FragmentAssetRole.AUDIO_TTS
                && binding.source() == FragmentAssetSource.AUDIO_JOB
                && binding.assetPath().equals("jobs/JOB-001/SEG-001.wav")));
        assertTrue(spokenBindings.stream().anyMatch(binding -> binding.role() == FragmentAssetRole.MAIN_IMAGE
                && binding.source() == FragmentAssetSource.STORYBOARD
                && binding.assetPath().equals("assets/images/one.png")));
        assertTrue(spokenBindings.stream().anyMatch(binding -> binding.role() == FragmentAssetRole.PLAYBACK_CUE));
        assertTrue(spokenBindings.stream().anyMatch(binding -> binding.role() == FragmentAssetRole.THEATRE_INTERVENTION));

        assertTrue(projection.bindingsForFragment(FragmentId.fromBlockId("B002")).isEmpty());
    }

    @Test
    void createsSegmentFallbackFragmentsWhenNoSourceBlockExists() {
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Lectura",
                "es",
                "Sin documento",
                List.of(new com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment(
                        "SEG-900",
                        com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType.PARAGRAPH,
                        "Extra",
                        "Texto narrable suelto",
                        List.of(),
                        "CHR-NARRATOR",
                        "VOC-NARRATOR",
                        "STY-NEUTRAL",
                        Map.of()
                )));

        FragmentWorkspaceProjection projection = useCase.build(null, script, null, null, List.of(), PlaybackManifest.empty());

        assertEquals(1, projection.fragments().size());
        assertEquals(FragmentId.fromSegmentId("SEG-900"), projection.fragments().getFirst().fragmentId());
        assertEquals("SEG-900", projection.fragments().getFirst().segmentId());
    }

    @Test
    void mapsBridgeImageNarrativeLayerToBridgeFragmentRole() {
        ReadableDocument document = document();
        NarrationScriptDocument script = new BuildNarrationScriptUseCase().build(document, "es");
        DocuPodcastProject project = DocuPodcastProject.createNew("Video")
                .withAsset(new ProjectAssetReference("IMG-BRIDGE", ProjectAssetKind.IMAGE, "Puente",
                        "assets/images/bridge.png", "image/png", "Puente", "", ""))
                .withNarrativeLayerAssignment(new NarrativeLayerAssignment(
                        "LYR-BRIDGE-001",
                        NarrativeLayerKind.BRIDGE_IMAGE,
                        new ScriptTextRange("SEG-001", 0, 10),
                        "IMG-BRIDGE",
                        "Puente",
                        "Prompt puente"));

        FragmentWorkspaceProjection projection = useCase.build(document, script, project, null, List.of(), PlaybackManifest.empty());

        assertTrue(projection.bindingsForFragment(FragmentId.fromBlockId("B001")).stream()
                .anyMatch(binding -> binding.role() == FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT
                        && binding.assetId().equals("IMG-BRIDGE")));
    }

    @Test
    void secondaryTableReadUnitsDoNotBecomePrimaryFragmentsOrAssetBindings() {
        ReadableDocument document = new ReadableDocument(
                "Tabla",
                SourceDocumentFormat.DOCX,
                Path.of("tabla.docx"),
                List.of(DocumentBlock.of("B010", DocumentBlockType.TABLE_NOTICE, "Tabla fuente", "table",
                        Map.of("table.rowCount", "2", "table.columnCount", "1", "table.header.0", "H", "table.cell.1.0", "v"))));
        NarrationScriptDocument script = new BuildNarrationScriptUseCase().build(document, "es", false, TableNarrationPolicy.READ_STRUCTURED);
        AudioJobSnapshot job = completedAudioJob(script.segments().getFirst().id());

        FragmentWorkspaceProjection projection = useCase.build(document, script, null, null, List.of(job), PlaybackManifest.empty());

        assertEquals(0, projection.fragments().size());
        assertTrue(projection.fragmentForBlock("B010").isEmpty());
        assertTrue(projection.bindingsForFragment(FragmentId.fromBlockId("B010")).stream()
                .noneMatch(binding -> binding.role() == FragmentAssetRole.AUDIO_TTS));
    }

    private static ReadableDocument document() {
        return new ReadableDocument(
                "Documento",
                SourceDocumentFormat.TXT,
                Path.of("documento.txt"),
                List.of(
                        DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Hola mundo", "txt"),
                        DocumentBlock.of("B002", DocumentBlockType.IMAGE_NOTICE, "Diagrama fuente", "txt",
                                Map.of("embeddedImagePath", "assets/source/diagram.png"))
                ));
    }

    private static DocuPodcastProject projectWithImageAndTheatre() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B001")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        return DocuPodcastProject.createNew("Documento")
                .withAsset(new ProjectAssetReference(
                        "IMG-001",
                        ProjectAssetKind.IMAGE,
                        "Imagen principal",
                        "assets/images/one.png",
                        "image/png",
                        "Imagen principal del fragmento",
                        "",
                        ""))
                .withTheatre(theatre);
    }

    private static StoryboardDocument storyboard(NarrationScriptDocument script) {
        return StoryboardDocument.createForScript(script)
                .withBinding(StoryboardBinding.of("STB-001", "SEG-001", "IMG-001", "Imagen del fragmento"));
    }

    private static AudioJobSnapshot completedAudioJob(String segmentId) {
        AudioSegmentSnapshot segment = AudioSegmentSnapshot.pending(segmentId, "Hola mundo")
                .completed("jobs/JOB-001/SEG-001.wav", 1.2);
        return new AudioJobSnapshot(
                "JOB-001",
                "Documento",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1.0,
                segmentId,
                "Hola mundo",
                0,
                "Listo",
                "jobs/JOB-001",
                "",
                "jobs/JOB-001/manifest.json",
                List.of(segment),
                Instant.EPOCH,
                Instant.EPOCH);
    }

    private static PlaybackManifest playbackManifest(String segmentId) {
        return new PlaybackManifest(
                "PLAYBACK-001",
                "JOB-001",
                List.of(new PlaybackCue(
                        segmentId,
                        "UNIT-001",
                        0.0,
                        1.2,
                        "AUD-001",
                        "jobs/JOB-001/SEG-001.wav",
                        "IMG-001",
                        "Hola mundo",
                        "Hola mundo")),
                "",
                Instant.EPOCH);
    }
}
