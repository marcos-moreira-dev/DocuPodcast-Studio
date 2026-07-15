package com.marcosmoreiradev.docupodcaststudio.productization;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioJobMaintenanceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSourceImportService;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrException;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessStatus;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.playback.BuildPlaybackManifestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.InspectProjectIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityReport;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityStatus;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRoundTripRequest;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRoundTripResult;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRoundTripUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.smoke.BrainSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.smoke.BrainSmokeStep;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BuildStoryboardFromImageLayersUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportSimpleVideoPackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPackageExportResult;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.InMemoryAudioJobQueue;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.MockAudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.DocxDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.MarkdownDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PlainTextDocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.export.FileSystemProjectBundleExporter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard.StoryboardWorkspaceFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automatic core smoke: it exercises the app brain without launching JavaFX.
 */
final class BrainSmokeScenarioTest {
    @TempDir
    Path tempDir;

    @Test
    void automaticBrainSmokeCoversDocumentAudioRoundTripIntegrityAndExports() throws Exception {
        Instant started = Instant.now();
        ArrayList<BrainSmokeStep> steps = new ArrayList<>();
        ArrayList<String> artifacts = new ArrayList<>();
        Path projectRoot = tempDir.resolve("brain-smoke-project");
        Path projectFile = projectRoot.resolve("brain-smoke.docupodcast.json");
        Path evidenceRoot = Path.of("target/docupodcast-smoke").toAbsolutePath().normalize();
        Files.createDirectories(projectRoot);
        Files.createDirectories(evidenceRoot);

        SmokeDocuments documents = step(steps, "DOC-SOURCES", "Importar fuentes y abrir PDF visual si OCR no esta disponible", () -> {
            SmokeSources sources = writeSmokeSources(tempDir.resolve("sources"));
            DocumentSourceImportService importService = new DocumentSourceImportService(new ImportDocumentUseCase(List.of(
                    new DocxDocumentImporter(), new MarkdownDocumentImporter(), new PlainTextDocumentImporter(),
                    new PdfDocumentImporter(new PdfBoxRenderEngine(), new BuildPdfOcrTextLayerUseCase(request -> {
                        throw new PdfOcrException(PdfOcrErrorCode.TESSERACT_NOT_FOUND, "OCR smoke no disponible.");
                    }))
            )));
            ReadableDocument docx = importService.importSource(sources.docx());
            ReadableDocument txt = importService.importSource(sources.txt());
            ReadableDocument markdown = importService.importSource(sources.markdown());
            ReadableDocument pdf = importService.importSource(sources.nativePdf());
            ReadableDocument scannedPdf = importService.importSource(sources.scannedPdf());
            assertEquals(SourceDocumentFormat.DOCX, docx.format());
            assertEquals(SourceDocumentFormat.TXT, txt.format());
            assertEquals(SourceDocumentFormat.MARKDOWN, markdown.format());
            assertEquals(SourceDocumentFormat.PDF, pdf.format());
            assertEquals(SourceDocumentFormat.PDF, scannedPdf.format());
            assertTrue(scannedPdf.blocks().stream().anyMatch(block -> "visual-fallback".equals(block.metadata().get("extractionMode"))));
            return new StepValue<>(new SmokeDocuments(sources, docx, txt, markdown, pdf),
                    "DOCX/TXT/Markdown/PDF nativo importados; PDF escaneado abierto como visual por OCR no disponible");
        });

        NarrationScriptDocument script = step(steps, "NARRATION", "Construir narración interna desde Documento", () -> {
            NarrationScriptDocument generated = new BuildNarrationScriptUseCase().build(documents.docx(), "es");
            assertFalse(generated.empty());
            assertTrue(generated.segmentCount() >= 2, "El DOCX smoke debe generar varios segmentos narrables");
            return new StepValue<>(generated, generated.segmentCount() + " segmentos narrables generados");
        });

        SmokeProject smokeProject = step(steps, "LAYERS-STORYBOARD", "Asignar imagen real y construir storyboard", () -> {
            Files.createDirectories(projectRoot.resolve("media/images"));
            Path image = projectRoot.resolve("media/images/escena-001.png");
            Files.writeString(image, "imagen-smoke", StandardCharsets.UTF_8);
            ProjectAssetReference imageAsset = new ProjectAssetReference(
                    "IMG-SMOKE-001",
                    ProjectAssetKind.IMAGE,
                    "Escena de apoyo",
                    "media/images/escena-001.png",
                    "image/png",
                    "Imagen real elegida por el usuario para una oración/segmento",
                    "sha256:" + sha256(image),
                    "Smoke automático T79"
            );
            String segmentId = script.segments().get(0).id();
            String blockId = documents.docx().blocks().get(0).id();
            NarrativeLayerAssignment imageLayer = new NarrativeLayerAssignment(
                    "LAY-SMOKE-IMG-001",
                    NarrativeLayerKind.IMAGE,
                    new ScriptTextRange(segmentId, 0, 12),
                    new DocumentTextRange(blockId, 0, 12),
                    imageAsset.id(),
                    "Escena de apoyo",
                    "Capa opcional del proyecto; no modifica el documento fuente"
            );
            DocuPodcastProject base = DocuPodcastProject.createNew("Smoke cerebro T79");
            DocuPodcastProject project = base
                    .withMetadata(base.metadata().withKind(ProjectKind.FULL_PROJECT))
                    .withAsset(imageAsset)
                    .withNarrativeLayerAssignment(imageLayer);
            StoryboardDocument storyboard = new BuildStoryboardFromImageLayersUseCase()
                    .build(script, null, project.assets(), project.narrativeLayerAssignments());
            assertEquals(1, storyboard.bindingCount());
            return new StepValue<>(new SmokeProject(project, storyboard), "1 asset real + 1 binding storyboard desde capa IMAGE");
        });

        SmokeAudio audio = step(steps, "AUDIO-MOCK", "Generar audio mock persistido", () -> {
            AudioJobFileRepository repository = new AudioJobFileRepository();
            MockAudioGenerationGateway gateway = new MockAudioGenerationGateway(new InMemoryAudioJobQueue(), repository, 0);
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<String> jobId = new AtomicReference<>();
            String submitted = gateway.submit(new AudioGenerationRequest(script, projectRoot, "Smoke audio T79"), status -> {
                if (status.state() == AudioJobState.COMPLETED) {
                    completed.countDown();
                }
            });
            jobId.set(submitted);
            assertTrue(completed.await(10, TimeUnit.SECONDS), "El audio mock debe completarse sin interacción UI");
            AudioJobSnapshot snapshot = repository.load(projectRoot, jobId.get()).orElseThrow();
            assertEquals(AudioJobState.COMPLETED, snapshot.state());
            assertTrue(Files.exists(projectRoot.resolve(snapshot.finalAudioPath())));
            assertTrue(new InspectAudioJobMaintenanceUseCase().inspect(projectRoot, snapshot).canReuseAudio());
            return new StepValue<>(new SmokeAudio(repository, snapshot), snapshot.completedSegments() + "/" + snapshot.totalSegments() + " segmentos WAV listos");
        });

        PlaybackManifest playback = step(steps, "PLAYBACK", "Construir manifest de reproducción sincronizada", () -> {
            PlaybackManifest manifest = new BuildPlaybackManifestUseCase().build(script, audio.job(), smokeProject.storyboard());
            assertFalse(manifest.cues().isEmpty());
            assertEquals(audio.job().jobId(), manifest.sourceJobId());
            return new StepValue<>(manifest, manifest.cueCount() + " cues de playback construidos");
        });
        assertNotNull(playback);

        SmokeRoundTrip roundTrip = step(steps, "ROUNDTRIP", "Guardar y reabrir proyecto completo", () -> {
            ProjectRoundTripUseCase useCase = new ProjectRoundTripUseCase(
                    new DocuPodcastProjectFileRepository(),
                    new ReadableDocumentWorkspaceRepository(),
                    new NarrationScriptWorkspaceFileRepository(),
                    new StoryboardWorkspaceFileRepository(),
                    audio.repository()
            );
            ProjectRoundTripResult result = useCase.execute(ProjectRoundTripRequest.of(
                    smokeProject.project(), projectFile, documents.docx(), script, smokeProject.storyboard(), List.of(audio.job())
            ));
            assertTrue(result.successful(), result.summary() + " " + result.messages());
            assertTrue(result.hydration().importedDocument().isPresent());
            assertTrue(result.hydration().narrationScript().isPresent());
            assertTrue(result.hydration().storyboard().isPresent());
            assertFalse(result.restoredAudioJobs().isEmpty());
            return new StepValue<>(new SmokeRoundTrip(result), "Proyecto, documento, guion, storyboard y audio rehidratados");
        });

        ProjectIntegrityReport integrity = step(steps, "INTEGRITY", "Inspeccionar integridad del proyecto reabierto", () -> {
            ProjectIntegrityReport report = new InspectProjectIntegrityUseCase(audio.repository())
                    .inspect(roundTrip.result().restoredProject(), projectFile, roundTrip.result().hydration(), roundTrip.result().restoredAudioJobs());
            assertEquals(ProjectIntegrityStatus.OK, report.status(), report.toMarkdown());
            return new StepValue<>(report, "Integridad OK sin reparación requerida");
        });
        assertTrue(integrity.ok());

        ExportReadinessReport readiness = step(steps, "EXPORT-READINESS", "Inspeccionar preparación de exportaciones", () -> {
            ExportReadinessReport report = new InspectExportReadinessUseCase().inspect(
                    roundTrip.result().restoredProject(),
                    projectFile,
                    roundTrip.result().hydration().narrationScript().orElse(script),
                    roundTrip.result().hydration().storyboard().orElse(smokeProject.storyboard()),
                    roundTrip.result().restoredAudioJobs()
            );
            assertEquals(ExportReadinessStatus.EXPORTABLE, report.items().stream()
                    .filter(item -> item.kind() == ExportableArtifactKind.PROJECT_BUNDLE)
                    .findFirst()
                    .orElseThrow()
                    .status());
            assertTrue(report.exportableCount() >= 4, report.toMarkdown());
            return new StepValue<>(report, report.exportableCount() + " salidas exportables inspeccionadas");
        });
        assertTrue(readiness.hasExportableOutput());

        ProjectBundleExportResult bundle = step(steps, "EXPORT-BUNDLE", "Exportar paquete auditable", () -> {
            ProjectBundleExportResult result = new FileSystemProjectBundleExporter().export(new ProjectBundleExportRequest(
                    roundTrip.result().restoredProject(),
                    projectFile,
                    roundTrip.result().hydration().narrationScript().orElse(script),
                    roundTrip.result().hydration().storyboard().orElse(smokeProject.storyboard()),
                    roundTrip.result().restoredAudioJobs(),
                    evidenceRoot.resolve("bundle")
            ));
            assertTrue(Files.exists(result.exportReadinessFile()));
            assertTrue(Files.exists(result.manifestFile()));
            assertTrue(Files.readString(result.exportReadinessFile()).contains("Preparación de exportaciones"));
            return new StepValue<>(result, "Bundle exportado con EXPORT_READINESS.md y manifiesto");
        });
        artifacts.add(evidenceRoot.relativize(bundle.rootDirectory()).toString());

        SimpleVideoPackageExportResult videoPackage = step(steps, "VIDEO-PACKAGE", "Exportar paquete de video simple", () -> {
            SimpleVideoPackageExportResult result = new ExportSimpleVideoPackageUseCase().export(
                    roundTrip.result().restoredProject(),
                    roundTrip.result().hydration().narrationScript().orElse(script),
                    roundTrip.result().hydration().storyboard().orElse(smokeProject.storyboard()),
                    roundTrip.result().restoredAudioJobs(),
                    evidenceRoot.resolve("simple-video")
            );
            assertTrue(Files.exists(result.renderManifestFile()));
            assertTrue(Files.exists(result.renderCommandsFile()));
            assertTrue(Files.readString(result.renderManifestFile()).contains("docupodcast-simple-video-render-v1"));
            return new StepValue<>(result, result.frameCount() + " frames en paquete de video simple");
        });
        artifacts.add(evidenceRoot.relativize(videoPackage.rootDirectory()).toString());

        Path projectTree = evidenceRoot.resolve("project-tree.txt");
        writeTree(projectRoot, projectTree);
        artifacts.add(evidenceRoot.relativize(projectTree).toString());
        Path readinessFile = evidenceRoot.resolve("EXPORT_READINESS.md");
        Files.writeString(readinessFile, readiness.toMarkdown(), StandardCharsets.UTF_8);
        artifacts.add(evidenceRoot.relativize(readinessFile).toString());
        Path integrityFile = evidenceRoot.resolve("PROJECT_INTEGRITY.md");
        Files.writeString(integrityFile, integrity.toMarkdown(), StandardCharsets.UTF_8);
        artifacts.add(evidenceRoot.relativize(integrityFile).toString());

        BrainSmokeReport report = new BrainSmokeReport(
                "docupodcast-brain-smoke-v1",
                "Smoke automático del cerebro — T79",
                started,
                Instant.now(),
                steps,
                artifacts
        );
        Path reportFile = evidenceRoot.resolve("SMOKE_REPORT.md");
        Files.writeString(reportFile, report.toMarkdown(), StandardCharsets.UTF_8);

        assertTrue(report.successful(), report.toMarkdown());
        assertTrue(Files.readString(reportFile).contains("Smoke automático del cerebro"));
    }

    private static <T> T step(List<BrainSmokeStep> steps, String id, String name, CheckedSupplier<StepValue<T>> supplier) throws Exception {
        Instant started = Instant.now();
        try {
            StepValue<T> value = supplier.get();
            steps.add(BrainSmokeStep.passed(id, name, value.detail(), Duration.between(started, Instant.now())));
            return value.value();
        } catch (Exception ex) {
            steps.add(BrainSmokeStep.failed(id, name, ex.getClass().getSimpleName() + ": " + ex.getMessage(), Duration.between(started, Instant.now())));
            throw ex;
        }
    }

    private static SmokeSources writeSmokeSources(Path sourceRoot) throws IOException {
        Files.createDirectories(sourceRoot);
        Path docx = sourceRoot.resolve("documento-smoke.docx");
        writeMinimalDocx(docx);
        Path txt = sourceRoot.resolve("documento-smoke.txt");
        Files.writeString(txt, "Documento TXT Smoke\n\nEste párrafo valida la entrada de texto plano para el cerebro.", StandardCharsets.UTF_8);
        Path markdown = sourceRoot.resolve("documento-smoke.md");
        Files.writeString(markdown, "# Documento Markdown Smoke\n\n- Punto narrable desde Markdown.", StandardCharsets.UTF_8);
        Path nativePdf = sourceRoot.resolve("documento-smoke.pdf");
        Files.writeString(nativePdf, nativeTextPdf(), StandardCharsets.ISO_8859_1);
        Path scannedPdf = sourceRoot.resolve("documento-escaneado.pdf");
        Files.writeString(scannedPdf, imageOnlyPdf(), StandardCharsets.ISO_8859_1);
        return new SmokeSources(docx, txt, markdown, nativePdf, scannedPdf);
    }

    private static void writeMinimalDocx(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            writeEntry(zip, "word/styles.xml", """
                    <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                    <w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">
                      <w:style w:type=\"paragraph\" w:styleId=\"Titulo1\"><w:name w:val=\"Título 1\"/></w:style>
                    </w:styles>
                    """);
            writeEntry(zip, "docProps/core.xml", """
                    <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                    <cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">
                      <dc:title>Documento Smoke T79</dc:title>
                    </cp:coreProperties>
                    """);
            writeEntry(zip, "word/document.xml", """
                    <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                    <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">
                      <w:body>
                        <w:p><w:pPr><w:pStyle w:val=\"Titulo1\"/></w:pPr><w:r><w:t>Documento Smoke T79</w:t></w:r></w:p>
                        <w:p><w:r><w:t>Primera oración narrable para validar el cerebro de DocuPodcast.</w:t></w:r></w:p>
                        <w:p><w:r><w:t>Segunda oración narrable para audio, playback, exportación e integridad.</w:t></w:r></w:p>
                      </w:body>
                    </w:document>
                    """);
        }
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String nativeTextPdf() {
        return "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /Contents 4 0 R >> endobj\n"
                + "4 0 obj << /Length 190 >> stream\n"
                + "BT /F1 12 Tf 72 720 Td (Documento PDF con texto nativo para smoke automático T79) Tj "
                + "0 -16 Td (Este contenido puede convertirse en documento narrable sin OCR ni interfaz gráfica.) Tj ET\n"
                + "endstream endobj\n%%EOF";
    }

    private static String imageOnlyPdf() {
        return "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /XObject /Subtype /Image /Width 10 /Height 10 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Length 0 >> stream\n\nendstream endobj\n"
                + "5 0 obj << /Length 30 >> stream\nq 10 0 0 10 0 0 cm /Im1 Do Q\nendstream endobj\n%%EOF";
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static void writeTree(Path root, Path target) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        StringBuilder out = new StringBuilder();
        try (var stream = Files.walk(normalized)) {
            for (Path path : stream.sorted().toList()) {
                out.append(normalized.relativize(path).toString().replace('\\', '/'));
                if (Files.isDirectory(path)) {
                    out.append('/');
                }
                out.append('\n');
            }
        }
        Files.writeString(target, out.toString(), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private record StepValue<T>(T value, String detail) {
    }

    private record SmokeSources(Path docx, Path txt, Path markdown, Path nativePdf, Path scannedPdf) {
    }

    private record SmokeDocuments(SmokeSources sources, ReadableDocument docx, ReadableDocument txt, ReadableDocument markdown, ReadableDocument pdf) {
    }

    private record SmokeProject(DocuPodcastProject project, StoryboardDocument storyboard) {
    }

    private record SmokeAudio(AudioJobFileRepository repository, AudioJobSnapshot job) {
    }

    private record SmokeRoundTrip(ProjectRoundTripResult result) {
    }
}
