package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfCropRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDocumentInfo;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOpenOptions;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TesseractPdfOcrEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void retriesEnglishWhenConfiguredSpanishEnglishRuntimeIsIncomplete() throws Exception {
        Path pdf = pdfFile();
        RecordingRunner runner = new RecordingRunner(List.of("spa+eng"), "eng");
        TesseractPdfOcrEngine engine = new TesseractPdfOcrEngine(
                new FakeRenderEngine(),
                runner,
                () -> "tesseract");

        PdfOcrPageResult result = engine.recognize(new PdfOcrRequest(
                pdf,
                1,
                300,
                PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                "spa+eng",
                true,
                null));

        assertEquals(1, result.words().size());
        assertTrue(runner.languages.contains("spa+eng"));
        assertTrue(runner.languages.contains("eng"));
    }

    @Test
    void writesPlainTextPageCacheInsideProjectCacheDirectory() throws Exception {
        Path pdf = pdfFile();
        Path cacheDirectory = tempDir.resolve(".docupodcast-cache").resolve("pdf-ocr");
        TesseractPdfOcrEngine engine = new TesseractPdfOcrEngine(
                new FakeRenderEngine(),
                new RecordingRunner(List.of(), "eng"),
                () -> "tesseract");

        engine.recognize(new PdfOcrRequest(
                pdf,
                7,
                300,
                PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                "eng",
                true,
                cacheDirectory));

        List<Path> textFiles;
        try (var paths = Files.walk(cacheDirectory.resolve("text"))) {
            textFiles = paths.filter(Files::isRegularFile).toList();
        }
        assertEquals(1, textFiles.size());
        assertTrue(textFiles.getFirst().getFileName().toString().contains("-p00007-"));
        assertEquals("Hello", Files.readString(textFiles.getFirst()).strip());
    }

    @Test
    void placesTessdataDirectoryBeforeImageSoTsvConfigIsApplied() throws Exception {
        Path pdf = pdfFile();
        Path executable = fakePortableTesseract();
        RecordingRunner runner = new RecordingRunner(List.of(), "eng");
        TesseractPdfOcrEngine engine = new TesseractPdfOcrEngine(
                new FakeRenderEngine(),
                runner,
                () -> executable.toString());

        engine.recognize(new PdfOcrRequest(
                pdf,
                2,
                300,
                PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                "eng",
                true,
                null));

        List<String> command = runner.lastCommand;
        int tessdata = command.indexOf("--tessdata-dir");
        int languageFlag = command.indexOf("-l");
        int image = languageFlag - 2;
        assertTrue(tessdata > 0);
        assertTrue(tessdata < image);
        assertTrue(!command.contains("stdout"));
    }

    private Path pdfFile() throws IOException {
        Path pdf = tempDir.resolve("source.pdf");
        Files.writeString(pdf, "%PDF-1.4");
        return pdf;
    }

    private Path fakePortableTesseract() throws IOException {
        Path runtime = tempDir.resolve("runtime");
        Path executable = runtime.resolve("bin").resolve("tesseract.exe");
        Files.createDirectories(executable.getParent());
        Files.createDirectories(runtime.resolve("tessdata"));
        Files.writeString(executable, "");
        Files.writeString(runtime.resolve("tessdata").resolve("eng.traineddata"), "");
        return executable;
    }

    private static String language(ExternalProcessRequest request) {
        List<String> command = request.command();
        int index = command.indexOf("-l");
        return index >= 0 && index + 1 < command.size() ? command.get(index + 1) : "";
    }

    private static String validTsv() {
        return String.join("\t", "level", "page_num", "block_num", "par_num", "line_num",
                "word_num", "left", "top", "width", "height", "conf", "text")
                + System.lineSeparator()
                + String.join("\t", "5", "1", "1", "1", "1", "1", "10", "20",
                "40", "10", "96", "Hello");
    }

    private static final class RecordingRunner implements ExternalProcessRunner {
        private final List<String> languagesToFail;
        private final String successLanguage;
        private final List<String> languages = new ArrayList<>();
        private List<String> lastCommand = List.of();

        private RecordingRunner(List<String> languagesToFail, String successLanguage) {
            this.languagesToFail = languagesToFail;
            this.successLanguage = successLanguage;
        }

        @Override
        public ExternalProcessResult run(ExternalProcessRequest request) {
            lastCommand = request.command();
            String language = language(request);
            languages.add(language);
            if (languagesToFail.contains(language)) {
                return new ExternalProcessResult(1, false, false, "",
                        "Error opening data file " + language + ".traineddata",
                        request.commandAudit(), Duration.ofMillis(1));
            }
            if (successLanguage.equals(language)) {
                writeTsvOutput(request);
                return new ExternalProcessResult(0, false, false, "", "",
                        request.commandAudit(), Duration.ofMillis(1));
            }
            return new ExternalProcessResult(1, false, false, "",
                    "language unavailable", request.commandAudit(), Duration.ofMillis(1));
        }

        private static void writeTsvOutput(ExternalProcessRequest request) {
            List<String> command = request.command();
            int index = command.indexOf("-l");
            if (index < 2) {
                return;
            }
            try {
                Files.writeString(Path.of(command.get(index - 1) + ".tsv"), validTsv());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static final class FakeRenderEngine implements PdfRenderEngine {
        @Override
        public PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PdfPageRenderResult renderPage(PdfPageRenderRequest request) {
            return new PdfPageRenderResult(
                    request.pageNumber(),
                    request.pageNumber(),
                    100.0,
                    200.0,
                    request.dpi(),
                    new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB),
                    "fake",
                    List.of());
        }

        @Override
        public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) throws PdfRenderException {
            throw new UnsupportedOperationException();
        }
    }
}
