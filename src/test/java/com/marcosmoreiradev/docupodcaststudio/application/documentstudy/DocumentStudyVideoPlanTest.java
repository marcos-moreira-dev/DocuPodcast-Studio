package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
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
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoPlanTest {
    @TempDir
    Path projectDirectory;

    @Test
    void resolvesOnlyParagraphsTablesAndHeadingsInDocxOrder() {
        ReadableDocument document = document();
        List<DocumentStudyVideoContentResolver.Item> items = new DocumentStudyVideoContentResolver()
                .resolve(document, DocumentStudyVideoConfiguration.empty());

        assertEquals(List.of(
                        DocumentStudyVideoContentResolver.Kind.COVER,
                        DocumentStudyVideoContentResolver.Kind.PARAGRAPH,
                        DocumentStudyVideoContentResolver.Kind.TABLE,
                        DocumentStudyVideoContentResolver.Kind.PARAGRAPH),
                items.stream().map(DocumentStudyVideoContentResolver.Item::kind).toList());
        assertFalse(items.stream().anyMatch(item -> item.block().type() == DocumentBlockType.IMAGE_NOTICE));
    }

    @Test
    void reusesParagraphSlideForAllAudioUnitsAndKeepsTableSilent() throws Exception {
        ReadableDocument document = document();
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-HEAD", NarrationSegmentType.HEADING, "Titulo", "Titulo", List.of("B000")),
                NarrationSegment.of("SEG-P1-A", NarrationSegmentType.PARAGRAPH, "P1", "Primera parte", List.of("B001")),
                NarrationSegment.of("SEG-P1-B", NarrationSegmentType.PARAGRAPH, "P1", "Segunda parte", List.of("B001")),
                new NarrationSegment("SEG-TABLE", NarrationSegmentType.TABLE_NOTICE, "Tabla", "No debe usarse",
                        List.of("B002"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                        Map.of("secondaryReadUnit", "true")),
                NarrationSegment.of("SEG-P2", NarrationSegmentType.PARAGRAPH, "P2", "Parrafo final", List.of("B004"))));
        List<String> segmentIds = List.of("SEG-HEAD", "SEG-P1-A", "SEG-P1-B", "SEG-P2");
        for (String id : segmentIds) {
            Path file = projectDirectory.resolve("jobs/JOB/" + id + ".wav");
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[]{1});
        }
        List<AudioSegmentSnapshot> audio = segmentIds.stream()
                .map(id -> AudioSegmentSnapshot.pending(id, id).completed("jobs/JOB/" + id + ".wav", 1.0))
                .toList();
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withTitle("Documental")
                .withTable(new DocumentTableSlideConfiguration("B002", 9.0))
                .withClosingSlide(new DocumentStudyClosingSlide("DOC-CLOSING-1", "Gracias", 7.0, ""));
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                project, document, script, List.of(audioJob(audio)), projectDirectory,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(6, plan.frameCount());
        assertEquals("SEG-HEAD", plan.frames().get(0).segmentId());
        assertEquals("SEG-P1-A", plan.frames().get(1).segmentId());
        assertEquals("SEG-P1-B", plan.frames().get(2).segmentId());
        assertEquals(plan.frames().get(1).imageRelativePath(), plan.frames().get(2).imageRelativePath());
        SimpleVideoFrame table = plan.frames().get(3);
        assertTrue(table.silentVisual());
        assertEquals(9.0, table.frameDurationSeconds());
        assertTrue(table.audioRelativePath().isBlank());
        assertEquals("SEG-P2", plan.frames().get(4).segmentId());
        SimpleVideoFrame closing = plan.frames().get(5);
        assertTrue(closing.silentVisual());
        assertEquals("CLOSING-DOC-CLOSING-1", closing.segmentId());
        assertEquals(7.0, closing.frameDurationSeconds());
        assertTrue(closing.audioRelativePath().isBlank());
        assertTrue(plan.exportableAsRenderedVideo());
    }

    @Test
    void disabledContentStaysResolvableButSkipsItsVisualAndNarration() throws Exception {
        ReadableDocument document = document();
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-HEAD", NarrationSegmentType.HEADING, "Titulo", "Titulo", List.of("B000")),
                NarrationSegment.of("SEG-P1", NarrationSegmentType.PARAGRAPH, "P1", "No debe usarse", List.of("B001")),
                NarrationSegment.of("SEG-P2", NarrationSegmentType.PARAGRAPH, "P2", "Debe continuar", List.of("B004"))));
        List<String> segmentIds = List.of("SEG-HEAD", "SEG-P1", "SEG-P2");
        for (String id : segmentIds) {
            Path file = projectDirectory.resolve("jobs/JOB-DISABLED/" + id + ".wav");
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[]{1});
        }
        List<AudioSegmentSnapshot> audio = segmentIds.stream()
                .map(id -> AudioSegmentSnapshot.pending(id, id)
                        .completed("jobs/JOB-DISABLED/" + id + ".wav", 1.0))
                .toList();
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withBlockEnabled("B001", false)
                .withBlockEnabled("B002", false);
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration));

        List<DocumentStudyVideoContentResolver.Item> content = new DocumentStudyVideoContentResolver()
                .resolve(document, configuration);
        assertFalse(content.stream().filter(item -> item.block().id().equals("B001")).findFirst().orElseThrow().enabled());

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                project, document, script, List.of(audioJob(audio)), projectDirectory,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(List.of("SEG-HEAD", "SEG-P2"),
                plan.frames().stream().map(SimpleVideoFrame::segmentId).toList());
        assertFalse(plan.frames().stream().anyMatch(SimpleVideoFrame::silentVisual));
    }

    @Test
    void musicPlaylistLoopsAndTrimsAtExactVideoEnd() throws Exception {
        Files.createDirectories(projectDirectory.resolve("media/audio"));
        Files.write(projectDirectory.resolve("media/audio/one.wav"), new byte[]{1});
        Files.write(projectDirectory.resolve("media/audio/two.wav"), new byte[]{1});
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withAsset(audioAsset("AUD-1", "media/audio/one.wav"))
                .withAsset(audioAsset("AUD-2", "media/audio/two.wav"));
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty().withMusicTracks(List.of(
                new DocumentStudyMusicTrack("MUSIC-1", "AUD-1", 4.0, 0.20),
                new DocumentStudyMusicTrack("MUSIC-2", "AUD-2", 3.0, 0.35)));
        project = project.withStudy(project.study().withDocumentaryVideoConfiguration(configuration));
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-1", "TABLE-1", "Tabla", "", "", "slide.png", "",
                0.0, 12.0, true, false, true);
        SimpleVideoPlan video = new SimpleVideoPlan("Video", List.of(frame), 0.0, Instant.EPOCH);

        var overlays = new BuildDocumentStudyVideoAudioOverlayPlanUseCase()
                .build(project, projectDirectory, video);

        assertEquals(4, overlays.inputs().size());
        assertEquals(List.of(0.0, 4.0, 7.0, 11.0),
                overlays.inputs().stream().map(input -> input.timelineStartSeconds()).toList());
        assertEquals(12.0, overlays.inputs().getLast().timelineEndSeconds());
        assertEquals(1.0, overlays.inputs().getLast().sourceEndSeconds());
        assertEquals(0.35, overlays.inputs().get(1).volume());
    }

    private static ReadableDocument document() {
        return new ReadableDocument("Documento", SourceDocumentFormat.DOCX, Path.of("source.docx"), List.of(
                DocumentBlock.of("B000", DocumentBlockType.HEADING, "Introduccion", "Heading 1"),
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto de prueba para el primer parrafo.", "Normal"),
                DocumentBlock.of("B002", DocumentBlockType.TABLE_NOTICE, "Tabla", "table", Map.of(
                        "table.rowCount", "2", "table.columnCount", "2",
                        "table.header.0", "Nombre", "table.header.1", "Valor",
                        "table.cell.1.0", "A", "table.cell.1.1", "1")),
                DocumentBlock.of("B003", DocumentBlockType.IMAGE_NOTICE, "Imagen incrustada", "image"),
                DocumentBlock.of("B004", DocumentBlockType.PARAGRAPH, "Texto de cierre.", "Normal")));
    }

    private static AudioJobSnapshot audioJob(List<AudioSegmentSnapshot> segments) {
        return new AudioJobSnapshot(
                "JOB-1", "Documento", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                segments.size(), segments.size(), 0, 1.0, "", "", 0, "OK", "jobs/JOB", "",
                "jobs/JOB/manifest.json", segments, Instant.EPOCH, Instant.EPOCH);
    }

    private static ProjectAssetReference audioAsset(String id, String path) {
        return new ProjectAssetReference(id, ProjectAssetKind.AUDIO_CLIP, id, path,
                "audio/wav", "Musica documental", "", "");
    }
}
