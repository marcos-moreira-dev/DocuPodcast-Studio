package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchDraft;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentBackgroundImageFit;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.JsonDocumentVideoBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentVideoBatchProjectUseCaseTest {
    @TempDir Path temporary;

    @Test void perDocumentBackgroundsInheritVisibilityAndFitAndSurviveProjectCopy() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("custom-documents"));
        writeDocx(source.resolve("one.docx"), 0);
        writeDocx(source.resolve("two.docx"), 0);
        Path a = Files.write(temporary.resolve("a.png"), new byte[]{1});
        Path b = Files.write(temporary.resolve("b.png"), new byte[]{2});
        var backgrounds = java.util.Map.of(
                "one.docx", new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(b.toString(), null),
                "two.docx", new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(b.toString(), 0.6));
        var common = DocumentTextVideoOptions.defaults().withBackgroundImage(a.toString(), 0.35);
        var profile = new DocumentVideoBatchProfile(common, 6, false, null, null, null, "", "", backgrounds);
        assertEquals(a.toString(), profile.effectiveVideo("other.docx").backgroundImagePath());
        assertEquals(b.toString(), profile.effectiveVideo("one.docx").backgroundImagePath());
        assertEquals(0.35, profile.effectiveVideo("one.docx").backgroundImageOpacity());
        assertEquals(0.6, profile.effectiveVideo("two.docx").backgroundImageOpacity());
        var changed = new DocumentVideoBatchProfile(common.withBackgroundImage(a.toString(), 0.45),6,false,null,null,null,"","",backgrounds);
        assertEquals(0.45, changed.effectiveVideo("one.docx").backgroundImageOpacity());
        assertEquals(0.6, changed.effectiveVideo("two.docx").backgroundImageOpacity());
        assertEquals(common.backgroundImageFit(), changed.effectiveVideo("two.docx").backgroundImageFit());
        var repository = new JsonDocumentVideoBatchRepository();
        Path draft = temporary.resolve("custom.json");
        repository.saveDraft(new DocumentVideoBatchDraft("Custom",source.toString(),temporary.toString(),changed),draft);
        assertEquals(changed,repository.openDraft(draft).profile());
        var created = new CreateDocumentVideoBatchProjectUseCase(new DiscoverDocumentVideoBatchSourcesUseCase(),repository,
                new DocuPodcastProjectFileRepository()).create("Custom",source,temporary,changed);
        var loaded = repository.open(created.descriptor()).profile();
        assertEquals(0.45,loaded.effectiveVideo("one.docx").backgroundImageOpacity());
        assertTrue(Files.isRegularFile(Path.of(loaded.effectiveVideo("one.docx").backgroundImagePath())));
        assertTrue(loaded.documentBackgrounds().get("one.docx").visibility() == null);
    }

    @Test void discoversNaturalOrderAndIgnoresUnrelatedFiles() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("entrada"));
        writeDocx(source.resolve("episodio-10.docx"), 2);
        writeDocx(source.resolve("episodio-2.docx"), 1);
        Files.writeString(source.resolve("notas.txt"), "no copiar", StandardCharsets.UTF_8);
        Files.write(source.resolve("portada.pdf"), "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        BatchSourceInventory inventory = new DiscoverDocumentVideoBatchSourcesUseCase().discover(source);

        assertEquals(3, inventory.documents().size());
        assertEquals("episodio-2.docx", inventory.documents().get(0).relativePath());
        assertEquals("episodio-10.docx", inventory.documents().get(1).relativePath());
        assertEquals(3, inventory.embeddedMediaCount());
        assertEquals(1, inventory.ignoredFileCount());
    }

    @Test void createsVerifiedContainerChildProjectsAndRoundTripsQueue() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("documentos"));
        Path episode = Files.createDirectory(source.resolve("YT-01"));
        writeDocx(episode.resolve("Introduccion.docx"), 1);
        Files.writeString(episode.resolve("ignorar.md"), "ajeno");
        Path destination = Files.createDirectory(temporary.resolve("salida con espacios"));
        JsonDocumentVideoBatchRepository repository = new JsonDocumentVideoBatchRepository();
        var useCase = new CreateDocumentVideoBatchProjectUseCase(new DiscoverDocumentVideoBatchSourcesUseCase(),
                repository, new DocuPodcastProjectFileRepository());

        var created = useCase.create("Mi serie", source, destination, DocumentVideoBatchProfile.defaults());

        assertTrue(Files.isRegularFile(created.descriptor()));
        assertTrue(Files.isDirectory(created.projectRoot().resolve("videos-renderizados")));
        assertTrue(Files.isRegularFile(created.projectRoot().resolve(created.project().items().getFirst().copiedSourceRelativePath())));
        assertTrue(Files.isRegularFile(created.projectRoot().resolve(created.project().items().getFirst().childProjectRelativePath())));
        assertFalse(Files.exists(created.projectRoot().resolve("fuentes/YT-01/ignorar.md")));

        var reopened = repository.open(created.descriptor());
        assertEquals(created.project().id(), reopened.id());
        assertEquals(1, reopened.items().size());
        var manager = new ManageDocumentVideoBatchQueueUseCase(repository);
        var paused = manager.pause(reopened, created.descriptor(), reopened.items().getFirst().id());
        assertEquals(BatchItemState.PAUSED, paused.items().getFirst().state());
        assertEquals(BatchItemState.PAUSED, repository.open(created.descriptor()).items().getFirst().state());

        var running = manager.resume(paused, created.descriptor(), paused.items().getFirst().id());
        running = manager.transition(running, created.descriptor(), running.items().getFirst().id(),
                BatchItemState.RUNNING, BatchItemStage.AUDIO_GENERATION, 0.42,
                "Generando voz");
        assertEquals(0.42, repository.open(created.descriptor()).items().getFirst().progress(), 0.0001);
        var recovered = manager.recoverInterrupted(running, created.descriptor());
        assertEquals(BatchItemState.PENDING, recovered.items().getFirst().state());
        assertEquals(BatchItemStage.AUDIO_GENERATION, recovered.items().getFirst().stage());
        assertTrue(recovered.items().getFirst().message().contains("continuar"));

        var cancelled = manager.cancelAll(recovered, created.descriptor());
        assertEquals(BatchItemState.CANCELLED, cancelled.items().getFirst().state());
        assertTrue(cancelled.items().getFirst().message().contains("derivados válidos"));
        assertEquals(BatchItemState.CANCELLED,
                repository.open(created.descriptor()).items().getFirst().state());
    }

    @Test void audioQueuesPreserveFormatUniqueOutputsAndCancellationOnReopen() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("audio-input"));
        writeDocx(Files.createDirectory(source.resolve("a")).resolve("tema.docx"), 0);
        writeDocx(Files.createDirectory(source.resolve("b")).resolve("tema.docx"), 0);
        var repository = new JsonDocumentVideoBatchRepository();
        var creator = new CreateDocumentVideoBatchProjectUseCase(new DiscoverDocumentVideoBatchSourcesUseCase(),
                repository, new DocuPodcastProjectFileRepository());
        for (var format : com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.values()) {
            var profile = new DocumentVideoBatchProfile(null, 6, false, null,
                    com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.AUDIO, format,
                    "piper", "qwen3-vl-local");
            var created = creator.create("audio-" + format, source, temporary, profile);
            var reopened = repository.open(created.descriptor());
            assertTrue(reopened.profile().audioOnly());
            assertEquals(format, reopened.profile().audioFormat());
            assertEquals("piper", reopened.profile().voiceEngineId());
            assertEquals("qwen3-vl-local", reopened.profile().aiEngineId());
            assertEquals(reopened.profile().video().fontFamily(), reopened.profile().video().titleFontFamily());
            assertTrue(Files.readString(created.descriptor()).contains("\"titleFontFamily\""));
            assertEquals(2, reopened.items().stream().map(item -> item.outputRelativePath()).distinct().count());
            assertTrue(reopened.items().stream().allMatch(item -> item.outputRelativePath().startsWith("audios-exportados/")
                    && item.outputRelativePath().endsWith(format.extension())));
            assertFalse(Files.exists(created.projectRoot().resolve("videos-renderizados")));
            var queue = new ManageDocumentVideoBatchQueueUseCase(repository);
            var first = reopened.items().getFirst();
            var completed = queue.transition(reopened, created.descriptor(), first.id(),
                    BatchItemState.COMPLETED, BatchItemStage.FINISHED, 1, "Audio verificado");
            var cancelled = queue.cancelAll(completed, created.descriptor());
            assertEquals(BatchItemState.COMPLETED, cancelled.items().getFirst().state());
            assertEquals(BatchItemState.CANCELLED, cancelled.items().getLast().state());
            assertEquals(format, repository.open(created.descriptor()).profile().audioFormat());
        }
    }

    @Test void legacyDescriptorWithoutOutputFieldsStillMeansVideo() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("legacy-input"));
        writeDocx(source.resolve("tema.docx"), 0);
        var repository = new JsonDocumentVideoBatchRepository();
        var creator = new CreateDocumentVideoBatchProjectUseCase(new DiscoverDocumentVideoBatchSourcesUseCase(),
                repository, new DocuPodcastProjectFileRepository());
        var created = creator.create("legacy", source, temporary, DocumentVideoBatchProfile.defaults());
        String json = Files.readString(created.descriptor()).replace("\"outputKind\":\"VIDEO\",\"audioFormat\":\"MP3\",", "");
        Files.writeString(created.descriptor(), json);
        assertFalse(repository.open(created.descriptor()).profile().audioOnly());
        assertTrue(repository.open(created.descriptor()).items().getFirst().outputRelativePath().endsWith(".mp4"));
    }

    @Test void savesAndLoadsAnEditableExpressConfigurationBeforeCreatingTheQueue() throws Exception {
        var repository = new JsonDocumentVideoBatchRepository();
        Path descriptor = temporary.resolve("curso.docupodcast-express.json");
        DocumentTextVideoOptions defaults = DocumentTextVideoOptions.defaults();
        DocumentTextVideoOptions video = new DocumentTextVideoOptions(defaults.resolution(),
                defaults.backgroundMode(), defaults.backgroundColor(), "fondo.png",
                defaults.textColor(), defaults.accentColor(), "Georgia", "Verdana",
                62, true, defaults.narratedUnderlineColor(), 5, 0.5,
                DocumentBackgroundImageFit.BLUR_AND_CONTAIN, defaults.textEffect(),
                defaults.textEffectColor(), defaults.textEffectThicknessPx());
        var profile = new DocumentVideoBatchProfile(video, 8.5, true, null,
                com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.VIDEO,
                com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.MP3,
                "xtts", "qwen3-vl-local");
        var draft = new DocumentVideoBatchDraft("Curso ERP", "C:/entrada", "D:/salida", profile);

        repository.saveDraft(draft, descriptor);
        var reopened = repository.openDraft(descriptor);

        assertEquals("Curso ERP", reopened.title());
        assertEquals("C:/entrada", reopened.sourceRoot());
        assertEquals("D:/salida", reopened.destinationRoot());
        assertEquals("Georgia", reopened.profile().video().fontFamily());
        assertEquals("Verdana", reopened.profile().video().titleFontFamily());
        assertEquals(DocumentBackgroundImageFit.BLUR_AND_CONTAIN,
                reopened.profile().video().backgroundImageFit());
        assertEquals("xtts", reopened.profile().voiceEngineId());
        assertTrue(Files.readString(descriptor).contains("document-video-express-configuration"));
    }

    @Test void audioReuseRequiresSuccessfulExportReceiptAndUnchangedBytes() throws Exception {
        var verifier = new VerifyBatchAudioOutputUseCase();
        var format = com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.MP3;
        Path audio = temporary.resolve("audio.mp3");
        Files.write(audio, new byte[128]);
        assertFalse(verifier.verify(audio, format));
        verifier.recordCompleted(audio, format);
        assertTrue(verifier.verify(audio, format));
        assertFalse(verifier.verify(audio, com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.AAC));
        Files.write(audio, new byte[129]);
        assertFalse(verifier.verify(audio, format));
    }

    @Test void acceptsOnlyPlausibleMp4HeadersForResumeReuse() throws Exception {
        VerifyBatchVideoOutputUseCase verifier = new VerifyBatchVideoOutputUseCase();
        Path partial = temporary.resolve("parcial.mp4");
        Files.write(partial, new byte[64]);
        assertFalse(verifier.verify(partial));

        byte[] plausible = new byte[64];
        plausible[3] = 24;
        plausible[4] = 'f'; plausible[5] = 't'; plausible[6] = 'y'; plausible[7] = 'p';
        plausible[8] = 'i'; plausible[9] = 's'; plausible[10] = 'o'; plausible[11] = 'm';
        Path completed = temporary.resolve("terminado.mp4");
        Files.write(completed, plausible);
        assertTrue(verifier.verify(completed));
    }

    @Test void writesAReadableFinalReportInsideTheBatchProject() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("lote con espacios"));
        Path descriptor = root.resolve("mi-lote.docupodcast-batch.json");
        Files.writeString(descriptor, "{}", StandardCharsets.UTF_8);
        var item = new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem(
                "uno", 0, "Álgebra inicial", "curso/uno.docx", "fuentes/uno.docx",
                "proyectos/uno/uno.docupodcast.json", "videos-renderizados/uno.mp4",
                "abc", 12L, 0, BatchItemState.COMPLETED, BatchItemStage.FINISHED,
                1.0, "MP4 verificado", Instant.now());
        var project = new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject(
                1, "lote", "Curso nocturno", root.toString(), root.toString(),
                descriptor.getFileName().toString(), DocumentVideoBatchProfile.defaults(),
                List.of(item), 2, Instant.now(), Instant.now());

        Path report = new WriteDocumentVideoBatchReportUseCase()
                .write(project, descriptor, false, "Trabajo terminado");

        assertEquals(root.resolve(WriteDocumentVideoBatchReportUseCase.REPORT_RELATIVE_PATH), report);
        String text = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(text.contains("Curso nocturno"));
        assertTrue(text.contains("Completados: 1"));
        assertTrue(text.contains("Álgebra inicial — Completado"));
        assertFalse(text.toLowerCase().contains("token"));
    }

    @Test void rejectsAnOutputProjectInsideTheInputTree() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("fuentes"));
        writeDocx(source.resolve("tema.docx"), 0);
        var useCase = new CreateDocumentVideoBatchProjectUseCase(
                new DiscoverDocumentVideoBatchSourcesUseCase(),
                new JsonDocumentVideoBatchRepository(),
                new DocuPodcastProjectFileRepository());

        IOException failure = assertThrows(IOException.class, () -> useCase.create(
                "Salida contaminante", source, source, DocumentVideoBatchProfile.defaults()));

        assertTrue(failure.getMessage().contains("no puede crearse dentro"));
        assertFalse(Files.exists(source.resolve("Salida-contaminante")));
    }

    private static void writeDocx(Path target, int mediaCount) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml")); zip.write("<Types/>".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            for (int index = 0; index < mediaCount; index++) {
                zip.putNextEntry(new ZipEntry("word/media/image" + index + ".png")); zip.write(new byte[]{1,2,3}); zip.closeEntry();
            }
        }
    }
}
