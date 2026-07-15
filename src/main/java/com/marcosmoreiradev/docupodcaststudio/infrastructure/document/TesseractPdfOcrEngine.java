package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrTsvParser;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrLine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Local OCR backend using Tesseract CLI TSV output. */
public final class TesseractPdfOcrEngine implements PdfOcrEngine {
    private static final Duration OCR_TIMEOUT = Duration.ofMinutes(3);
    private static final String OCR_CACHE_VERSION = "pdf-ocr-v2";
    private static final Path TEMP_PLAIN_TEXT_CACHE_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"), "docupodcast-studio", "pdf-ocr-text");
    private static final AtomicBoolean CLEANUP_HOOK_INSTALLED = new AtomicBoolean(false);

    private final PdfRenderEngine renderEngine;
    private final ExternalProcessRunner processRunner;
    private final PdfOcrTsvParser tsvParser;
    private final Supplier<String> tesseractCommand;

    public TesseractPdfOcrEngine(PdfRenderEngine renderEngine, ExternalProcessRunner processRunner) {
        this(renderEngine, processRunner, () -> "tesseract");
    }

    public TesseractPdfOcrEngine(PdfRenderEngine renderEngine,
                                 ExternalProcessRunner processRunner,
                                 Supplier<String> tesseractCommand) {
        this(renderEngine, processRunner, new PdfOcrTsvParser(), tesseractCommand);
    }

    TesseractPdfOcrEngine(PdfRenderEngine renderEngine,
                          ExternalProcessRunner processRunner,
                          PdfOcrTsvParser tsvParser) {
        this(renderEngine, processRunner, tsvParser, () -> "tesseract");
    }

    TesseractPdfOcrEngine(PdfRenderEngine renderEngine,
                          ExternalProcessRunner processRunner,
                          PdfOcrTsvParser tsvParser,
                          Supplier<String> tesseractCommand) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.tsvParser = Objects.requireNonNull(tsvParser, "tsvParser");
        this.tesseractCommand = tesseractCommand == null ? () -> "tesseract" : tesseractCommand;
    }

    @Override
    public PdfOcrPageResult recognize(PdfOcrRequest request) throws PdfOcrException {
        validate(request);
        Path cacheFile = cacheFile(request);
        PdfPageRenderResult page = renderPage(request);
        String tsv = readCache(cacheFile).orElse(null);
        if (tsv == null) {
            Path imagePath = writeTempPageImage(page);
            try {
                tsv = runTesseract(imagePath, request.languages());
                writeCache(cacheFile, tsv);
            } finally {
                deleteQuietly(imagePath);
            }
        }
        List<String> warnings = new ArrayList<>(page.warnings());
        PdfOcrPageResult parsed = tsvParser.parse(tsv, page.pageNumber(), page.dpi(),
                page.image().getWidth(), page.image().getHeight(), page.pageWidthPoints(), page.pageHeightPoints());
        parsed = withDominantLineColors(parsed, page.image());
        writePlainTextPageCache(request, parsed);
        warnings.addAll(parsed.warnings());
        return new PdfOcrPageResult(parsed.pageNumber(), parsed.dpi(), parsed.imageWidthPixels(),
                parsed.imageHeightPixels(), parsed.pageWidthPoints(), parsed.pageHeightPoints(),
                parsed.lines(), parsed.words(), parsed.textLayer(), warnings);
    }

    private static PdfOcrPageResult withDominantLineColors(PdfOcrPageResult result, BufferedImage image) {
        if (result == null || image == null || result.lines().isEmpty()) {
            return result;
        }
        List<PdfOcrLine> lines = result.lines().stream()
                .map(line -> new PdfOcrLine(
                        line.pageNumber(),
                        line.text(),
                        line.region(),
                        line.words(),
                        line.confidence(),
                        dominantTextColor(image, line.region())))
                .toList();
        PdfTextLayer layer = new PdfTextLayer(result.pageNumber(),
                lines.isEmpty() ? PdfTextLayerOrigin.UNAVAILABLE : PdfTextLayerOrigin.OCR_LOCAL,
                lines.stream().map(PdfOcrLine::toTextLine).toList(),
                result.textLayer().warnings());
        return new PdfOcrPageResult(result.pageNumber(), result.dpi(), result.imageWidthPixels(),
                result.imageHeightPixels(), result.pageWidthPoints(), result.pageHeightPoints(),
                lines, result.words(), layer, result.warnings());
    }

    private static String dominantTextColor(BufferedImage image, PdfPageRegion region) {
        if (image == null || region == null || region.pageWidthPoints() <= 0.0 || region.pageHeightPoints() <= 0.0) {
            return "";
        }
        int x1 = clamp((int) Math.floor(image.getWidth() * region.xMinPoints() / region.pageWidthPoints()), 0, image.getWidth() - 1);
        int y1 = clamp((int) Math.floor(image.getHeight() * region.yMinPoints() / region.pageHeightPoints()), 0, image.getHeight() - 1);
        int x2 = clamp((int) Math.ceil(image.getWidth() * region.xMaxPoints() / region.pageWidthPoints()), x1 + 1, image.getWidth());
        int y2 = clamp((int) Math.ceil(image.getHeight() * region.yMaxPoints() / region.pageHeightPoints()), y1 + 1, image.getHeight());
        int stepX = Math.max(1, (x2 - x1) / 80);
        int stepY = Math.max(1, (y2 - y1) / 12);
        int black = 0;
        int blue = 0;
        int red = 0;
        int other = 0;
        for (int y = y1; y < y2; y += stepY) {
            for (int x = x1; x < x2; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                int min = Math.min(r, Math.min(g, b));
                int max = Math.max(r, Math.max(g, b));
                if (max > 242 && min > 225) {
                    continue;
                }
                if (b > r + 28 && b > g + 12) {
                    blue++;
                } else if (r > g + 28 && r > b + 12) {
                    red++;
                } else if (max < 170 || max - min < 26) {
                    black++;
                } else {
                    other++;
                }
            }
        }
        int max = Math.max(Math.max(black, blue), Math.max(red, other));
        if (max <= 0) {
            return "";
        }
        if (blue == max) {
            return "blue";
        }
        if (red == max) {
            return "red";
        }
        if (black == max) {
            return "black";
        }
        return "other";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void validate(PdfOcrRequest request) throws PdfOcrException {
        if (request == null || request.sourcePdf() == null || request.pageNumber() <= 0) {
            throw new PdfOcrException(PdfOcrErrorCode.INVALID_REQUEST,
                    "Solicitud OCR PDF invalida: falta PDF o pagina.");
        }
        if (!Files.isRegularFile(request.sourcePdf())) {
            throw new PdfOcrException(PdfOcrErrorCode.INVALID_REQUEST,
                    "El PDF para OCR no existe o no es un archivo regular.");
        }
    }

    private PdfPageRenderResult renderPage(PdfOcrRequest request) throws PdfOcrException {
        try {
            return renderEngine.renderPage(new PdfPageRenderRequest(
                    request.sourcePdf(),
                    request.pageNumber(),
                    request.dpi(),
                    request.maxPixelCount(),
                    Color.WHITE,
                    true));
        } catch (PdfRenderException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.PDF_RENDER_FAILED,
                    "No se pudo renderizar la pagina PDF para OCR: " + ex.getMessage(), ex);
        }
    }

    private static Path writeTempPageImage(PdfPageRenderResult page) throws PdfOcrException {
        try {
            Path imagePath = Files.createTempFile("docupodcast-pdf-ocr-p" + page.pageNumber() + "-", ".png");
            ImageIO.write(page.image(), "png", imagePath.toFile());
            return imagePath;
        } catch (IOException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                    "No se pudo preparar imagen temporal para OCR.", ex);
        }
    }

    private String runTesseract(Path imagePath, String languages) throws PdfOcrException {
        PdfOcrException lastFailure = null;
        for (String candidateLanguages : languageCandidates(languages)) {
            try {
                return runTesseractWithLanguages(imagePath, candidateLanguages);
            } catch (PdfOcrException ex) {
                if (ex.code() == PdfOcrErrorCode.TESSERACT_NOT_FOUND) {
                    throw ex;
                }
                lastFailure = ex;
            }
        }
        if (lastFailure != null) {
            throw new PdfOcrException(lastFailure.code(),
                    lastFailure.getMessage() + " Idiomas intentados: "
                            + String.join(", ", languageCandidates(languages)) + ".",
                    lastFailure);
        }
        throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                "No se pudo ejecutar OCR local: no hay idiomas OCR candidatos.");
    }

    private String runTesseractWithLanguages(Path imagePath, String languages) throws PdfOcrException {
        String command = resolvedTesseractCommand();
        Path outputBase = tesseractOutputBase();
        Path tsvOutput = Path.of(outputBase.toString() + ".tsv");
        List<String> args = new ArrayList<>();
        args.add(command);
        resolvedTessdataDirectory(command).ifPresent(directory -> {
            args.add("--tessdata-dir");
            args.add(directory.toString());
        });
        args.add(imagePath.toString());
        args.add(outputBase.toString());
        args.add("-l");
        args.add(languages);
        args.add("tsv");
        ExternalProcessRequest request = ExternalProcessRequest.of(args, "tesseract-pdf-ocr", OCR_TIMEOUT);
        try {
            ExternalProcessResult result;
            try {
                result = processRunner.run(request);
            } catch (IOException ex) {
                throw new PdfOcrException(PdfOcrErrorCode.TESSERACT_NOT_FOUND,
                        "No se encontro Tesseract CLI. Configura o prepara el runtime OCR local con idiomas spa/eng.", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                        "OCR local interrumpido.", ex);
            }
            if (!result.succeeded()) {
                throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                        "Tesseract no pudo detectar texto PDF. " + result.combinedOutputTail());
            }
            if (!Files.isRegularFile(tsvOutput)) {
                throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                        "Tesseract termino sin generar salida TSV OCR.");
            }
            try {
                return Files.readString(tsvOutput, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                        "No se pudo leer salida TSV temporal de OCR.", ex);
            }
        } catch (PdfOcrException ex) {
            throw ex;
        } finally {
            deleteQuietly(outputBase);
            deleteQuietly(tsvOutput);
        }
    }

    private static Path tesseractOutputBase() throws PdfOcrException {
        try {
            Path outputBase = Files.createTempFile("docupodcast-pdf-ocr-tsv-", "");
            Files.deleteIfExists(outputBase);
            return outputBase;
        } catch (IOException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.OCR_FAILED,
                    "No se pudo preparar salida temporal TSV para OCR.", ex);
        }
    }

    private static List<String> languageCandidates(String languages) {
        String requested = languages == null || languages.isBlank()
                ? PdfOcrRequest.DEFAULT_LANGUAGES
                : languages.strip();
        Set<String> candidates = new LinkedHashSet<>();
        addLanguageCandidate(candidates, requested);
        String normalized = requested.toLowerCase(java.util.Locale.ROOT).replace(' ', '+');
        if (normalized.contains("spa") && normalized.contains("eng")) {
            addLanguageCandidate(candidates, "eng+spa");
            addLanguageCandidate(candidates, "spa+eng");
        }
        addLanguageCandidate(candidates, "eng");
        addLanguageCandidate(candidates, "spa");
        return List.copyOf(candidates);
    }

    private static void addLanguageCandidate(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.strip().replaceAll("\\s+", "+");
        if (!normalized.isBlank()) {
            candidates.add(normalized);
        }
    }

    private String resolvedTesseractCommand() {
        try {
            String command = tesseractCommand.get();
            return command == null || command.isBlank() ? "tesseract" : command.strip();
        } catch (RuntimeException ex) {
            return "tesseract";
        }
    }

    private static Optional<Path> resolvedTessdataDirectory(String command) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }
        try {
            Path executable = Path.of(command.strip());
            if (!Files.isRegularFile(executable)) {
                return Optional.empty();
            }
            ArrayList<Path> candidates = new ArrayList<>();
            Path parent = executable.getParent();
            if (parent != null) {
                candidates.add(parent.resolve("tessdata").normalize());
                Path root = parent.getParent();
                if (root != null) {
                    candidates.add(root.resolve("tessdata").normalize());
                }
            }
            return candidates.stream().filter(Files::isDirectory).findFirst();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static java.util.Optional<String> readCache(Path cacheFile) throws PdfOcrException {
        if (cacheFile == null || !Files.isRegularFile(cacheFile)) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Files.readString(cacheFile, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.CACHE_FAILED,
                    "No se pudo leer cache OCR local.", ex);
        }
    }

    private static void writeCache(Path cacheFile, String tsv) throws PdfOcrException {
        if (cacheFile == null) {
            return;
        }
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(cacheFile, tsv == null ? "" : tsv, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.CACHE_FAILED,
                    "No se pudo escribir cache OCR local.", ex);
        }
    }

    private static void writePlainTextPageCache(PdfOcrRequest request, PdfOcrPageResult parsed) {
        if (request == null || parsed == null) {
            return;
        }
        Path directory = plainTextCacheDirectory(request.cacheDirectory());
        if (request.cacheDirectory() == null) {
            installPlainTextCacheCleanupHook();
        }
        try {
            Files.createDirectories(directory);
            String text = parsed.lines().stream()
                    .map(line -> line.text() == null ? "" : line.text().strip())
                    .filter(line -> !line.isBlank())
                    .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
            Files.writeString(plainTextCacheFile(request, directory), text, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Auxiliary text cache must never block OCR.
        }
    }

    private static Path plainTextCacheDirectory(Path cacheDirectory) {
        return cacheDirectory == null
                ? TEMP_PLAIN_TEXT_CACHE_DIRECTORY
                : cacheDirectory.resolve("text").normalize();
    }

    private static Path plainTextCacheFile(PdfOcrRequest request, Path directory) throws IOException {
        Path source = request.sourcePdf().toAbsolutePath().normalize();
        String languages = safeFileToken(request.languages());
        String seed = OCR_CACHE_VERSION + "|" + source + "|" + Files.getLastModifiedTime(source).toMillis() + "|"
                + request.pageNumber() + "|" + request.dpi() + "|" + request.languages();
        String hash = sha256(seed).substring(0, 16);
        String name = "pdf-ocr-" + hash
                + "-p" + String.format(java.util.Locale.ROOT, "%05d", request.pageNumber())
                + "-dpi" + request.dpi()
                + "-" + languages + ".txt";
        return directory.resolve(name);
    }

    private static String safeFileToken(String value) {
        String raw = value == null || value.isBlank() ? PdfOcrRequest.DEFAULT_LANGUAGES : value.strip();
        String token = raw.replaceAll("[^A-Za-z0-9+_-]+", "_");
        return token.isBlank() ? "ocr" : token;
    }

    private static void installPlainTextCacheCleanupHook() {
        if (!CLEANUP_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> deletePlainTextCacheDirectory(TEMP_PLAIN_TEXT_CACHE_DIRECTORY),
                "docupodcast-pdf-ocr-text-cleanup"));
    }

    private static void deletePlainTextCacheDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup on process exit.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup on process exit.
        }
    }

    private static Path cacheFile(PdfOcrRequest request) throws PdfOcrException {
        if (request.cacheDirectory() == null) {
            return null;
        }
        try {
            Path source = request.sourcePdf().toAbsolutePath().normalize();
            String seed = OCR_CACHE_VERSION + "|" + source + "|" + Files.getLastModifiedTime(source).toMillis() + "|"
                    + request.pageNumber() + "|" + request.dpi() + "|" + request.languages();
            return request.cacheDirectory().resolve("pdf-ocr-" + sha256(seed) + ".tsv");
        } catch (IOException ex) {
            throw new PdfOcrException(PdfOcrErrorCode.CACHE_FAILED,
                    "No se pudo calcular cache OCR local.", ex);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort temp cleanup.
        }
    }
}
