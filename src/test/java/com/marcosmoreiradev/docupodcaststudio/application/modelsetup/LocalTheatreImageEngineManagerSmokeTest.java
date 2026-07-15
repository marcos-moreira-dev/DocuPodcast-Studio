package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalTheatreImageEngineManagerSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void smokeTestDownloadsRealPngFromComfyUiLikeServer() throws Exception {
        prepareRuntimeAndModel();
        AtomicReference<String> submittedPrompt = new AtomicReference<>("");
        HttpServer server = startFakeComfyUi(submittedPrompt, png(1920, 1080));
        try {
            int port = server.getAddress().getPort();
            OperationalSettings settings = settingsFor("http://127.0.0.1:" + port);
            LocalTheatreImageEngineManager manager = new LocalTheatreImageEngineManager(
                    new InspectLocalTheatreImageSetupReadinessUseCase(),
                    ExternalProcessRunner.unavailable("Imagen IA teatral"),
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

            ImageEngineSmokeReport report = manager.smoke(settings, tempDir,
                    "aviadores comicos revisan un avion dorado en un escenario teatral", 17);

            assertTrue(report.success(), report.userMessage() + " " + report.diagnostic());
            assertNotNull(report.outputImage());
            assertTrue(Files.isRegularFile(report.outputImage()));
            assertEquals(1920, report.width());
            assertEquals(1080, report.height());
            assertEquals("1080p", report.outputProfile());
            assertEquals("16:9", report.aspectRatio());
            assertTrue(submittedPrompt.get().contains("aviadores comicos revisan un avion dorado"));
            assertTrue(submittedPrompt.get().contains("\"steps\":17"));
            assertTrue(submittedPrompt.get().contains("\"width\":768"));
            assertTrue(submittedPrompt.get().contains("\"height\":432"));
            assertTrue(report.diagnostic().contains("baseGeneration=768x432"));
            assertTrue(report.diagnostic().contains("resize=java2d-bicubic"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeRequestUsesSelectedOutputProfileAndAspectRatio() throws Exception {
        prepareRuntimeAndModel();
        AtomicReference<String> submittedPrompt = new AtomicReference<>("");
        HttpServer server = startFakeComfyUi(submittedPrompt, png(1280, 720));
        try {
            int port = server.getAddress().getPort();
            OperationalSettings settings = settingsFor("http://127.0.0.1:" + port);
            LocalTheatreImageEngineManager manager = new LocalTheatreImageEngineManager(
                    new InspectLocalTheatreImageSetupReadinessUseCase(),
                    ExternalProcessRunner.unavailable("Imagen IA teatral"),
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

            ImageEngineSmokeRequest request = new ImageEngineSmokeRequest(
                    TheatreImageGenerationPreset.TEST_4GB_SD15,
                    ImageEnhancementOutputProfile.HD_720,
                    TheatreImageAspectRatio.WIDE_16_9,
                    "hangar teatral",
                    12,
                    tempDir.resolve("custom-smoke-output"));
            ImageEngineSmokeReport report = manager.smoke(settings, tempDir, request);

            assertTrue(report.success(), report.userMessage() + " " + report.diagnostic());
            assertEquals(1280, report.width());
            assertEquals(720, report.height());
            assertEquals("720p", report.outputProfile());
            assertEquals("16:9", report.aspectRatio());
            assertTrue(report.outputImage().startsWith(tempDir.resolve("custom-smoke-output")));
            assertTrue(submittedPrompt.get().contains("\"width\":768"));
            assertTrue(submittedPrompt.get().contains("\"height\":432"));
            assertTrue(report.diagnostic().contains("baseGeneration=768x432"));
            assertTrue(report.diagnostic().contains("resize=java2d-bicubic"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeRequestFailsClearlyWhenFluxLicenseOrComponentsAreMissing() throws Exception {
        prepareRuntimeAndModel();
        AtomicReference<String> submittedPrompt = new AtomicReference<>("");
        HttpServer server = startFakeComfyUi(submittedPrompt, png(1920, 1080));
        try {
            int port = server.getAddress().getPort();
            OperationalSettings settings = settingsFor("http://127.0.0.1:" + port);
            LocalTheatreImageEngineManager manager = new LocalTheatreImageEngineManager(
                    new InspectLocalTheatreImageSetupReadinessUseCase(),
                    ExternalProcessRunner.unavailable("Imagen IA teatral"),
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

            ImageEngineSmokeRequest request = new ImageEngineSmokeRequest(
                    TheatreImageGenerationPreset.HIGH_QUALITY_FLUX,
                    ImageEnhancementOutputProfile.FHD_1080,
                    TheatreImageAspectRatio.WIDE_16_9,
                    "flux workflow test",
                    12,
                    tempDir.resolve("unsupported-workflow"));
            ImageEngineSmokeReport report = manager.smoke(settings, tempDir, request);

            assertTrue(!report.success(), report.userMessage());
            assertTrue(report.userMessage().contains("licencia FLUX.1-dev"));
            assertTrue(report.diagnostic().contains("licenseAccepted=false"));
            assertTrue(submittedPrompt.get().isBlank());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void smokeRequestCalculatesExpectedWideDimensionsForEveryOutputProfile() {
        assertWideDimensions(ImageEnhancementOutputProfile.HD_720, 1280, 720);
        assertWideDimensions(ImageEnhancementOutputProfile.FHD_1080, 1920, 1080);
        assertWideDimensions(ImageEnhancementOutputProfile.QHD_2K, 2560, 1440);
        assertWideDimensions(ImageEnhancementOutputProfile.UHD_4K, 3840, 2160);
    }

    @Test
    void detachedLaunchCommandQuotesWindowsPathsWithSpaces() {
        List<String> command = LocalTheatreImageEngineManager.detachedLaunchCommand(List.of(
                "C:\\Users\\MARCOS MOREIRA\\Downloads\\g\\tools\\image\\start-image-engine.bat",
                "--port",
                "8188"));

        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            assertEquals(List.of("cmd", "/d", "/c", "start", "\"\"", "/min"), command.subList(0, 6));
            assertEquals("C:\\Users\\MARCOS MOREIRA\\Downloads\\g\\tools\\image\\start-image-engine.bat", command.get(6));
            assertEquals("--port", command.get(7));
            assertEquals("8188", command.get(8));
        } else {
            assertEquals(List.of("sh", "-c"), command.subList(0, 2));
            assertTrue(command.get(2).contains("'C:\\Users\\MARCOS MOREIRA\\Downloads\\g\\tools\\image\\start-image-engine.bat'"));
        }
    }

    @Test
    void readinessWaitUsesConfiguredImageTimeoutBounds() {
        assertEquals(Duration.ofSeconds(60), LocalTheatreImageEngineManager.readinessWait(null));
        assertEquals(Duration.ofSeconds(60), LocalTheatreImageEngineManager.readinessWait(settingsFor("http://127.0.0.1:8188", 20)));
        assertEquals(Duration.ofSeconds(120), LocalTheatreImageEngineManager.readinessWait(settingsFor("http://127.0.0.1:8188", 120)));
        assertEquals(Duration.ofSeconds(240), LocalTheatreImageEngineManager.readinessWait(settingsFor("http://127.0.0.1:8188", 300)));
    }

    @Test
    void managedLauncherIsRenewedWithRelativeQuotedPaths() throws Exception {
        Path runtime = tempDir.resolve("tools/image");
        Files.createDirectories(runtime.resolve("ComfyUI"));
        Files.createDirectories(runtime.resolve("venv/Scripts"));
        Files.writeString(runtime.resolve("ComfyUI/main.py"), "print('comfy')\n");
        Files.writeString(runtime.resolve("venv/Scripts/python.exe"), "");
        Files.writeString(runtime.resolve("start-image-engine.bat"),
                "@echo off\r\n\"C:\\Users\\MARCOS MOREIRA\\broken python.exe\" main.py %*\r\n");

        Path launcher = ImportLocalTheatreImageRuntimeUseCase.ensureManagedLauncher(runtime, ModelSetupProgressListener.noop());
        String script = Files.readString(launcher);

        assertTrue(script.contains("cd /d \"%~dp0ComfyUI\""));
        assertTrue(script.contains("if \"%~1\"==\"\""));
        assertTrue(script.contains("\"%~dp0venv\\Scripts\\python.exe\" \"main.py\" --lowvram --disable-auto-launch --port 8188"));
        assertTrue(script.contains("\"%~dp0venv\\Scripts\\python.exe\" \"main.py\" --disable-auto-launch %*"));
        assertTrue(!script.contains(" -s "));
        assertTrue(!script.contains("C:\\Users\\MARCOS MOREIRA"));
    }

    private static void assertWideDimensions(ImageEnhancementOutputProfile profile, int expectedWidth, int expectedHeight) {
        ImageEngineSmokeRequest request = new ImageEngineSmokeRequest(
                TheatreImageGenerationPreset.TEST_4GB_SD15,
                profile,
                TheatreImageAspectRatio.WIDE_16_9,
                "dimension test",
                12,
                null);
        assertEquals(expectedWidth, request.targetWidth());
        assertEquals(expectedHeight, request.targetHeight());
    }

    private void prepareRuntimeAndModel() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/image"));
        Files.writeString(tempDir.resolve("tools/image/start-image-engine.bat"), "@echo off\r\n");
        Path image = tempDir.resolve("models/image");
        Files.createDirectories(image.resolve("workflows"));
        writeSparse(image.resolve("v1-5-pruned-emaonly-fp16.safetensors"), 1_200_000);
        Files.writeString(image.resolve("workflows/workflow-sd15-reference.json"), """
                {
                  "1": {"class_type": "CheckpointLoaderSimple"},
                  "2": {"class_type": "CLIPTextEncode"},
                  "3": {"class_type": "KSampler"},
                  "4": {"class_type": "VAEDecode"},
                  "5": {"class_type": "SaveImage"}
                }
                """);
        Files.writeString(image.resolve("model-manifest.json"), "{}");
    }

    private HttpServer startFakeComfyUi(AtomicReference<String> submittedPrompt, byte[] png) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/system_stats", exchange -> respond(exchange, 200, "application/json", "{}".getBytes()));
        server.createContext("/prompt", exchange -> {
            submittedPrompt.set(new String(exchange.getRequestBody().readAllBytes()));
            respond(exchange, 200, "application/json", "{\"prompt_id\":\"smoke-1\"}".getBytes());
        });
        server.createContext("/history", exchange -> respond(exchange, 200, "application/json",
                "{\"smoke-1\":{\"outputs\":{\"7\":{\"images\":[{\"filename\":\"smoke.png\",\"subfolder\":\"\",\"type\":\"output\"}]}}}}"
                        .getBytes()));
        server.createContext("/view", exchange -> respond(exchange, 200, "image/png", png));
        server.start();
        return server;
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static OperationalSettings settingsFor(String baseUrl) {
        return settingsFor(baseUrl, 20);
    }

    private static OperationalSettings settingsFor(String baseUrl, int timeoutSeconds) {
        return new OperationalSettings(null, null, null, null,
                new ImageGenerationSettings("managed-local", baseUrl, "AUTO", "TEST_4GB_SD15",
                        "v1-5-pruned-emaonly-fp16.safetensors", "models/image/adapters", timeoutSeconds, true),
                null, null, null, null);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void writeSparse(Path path, long size) throws Exception {
        Files.createDirectories(path.getParent());
        try (SeekableByteChannel channel = Files.newByteChannel(path,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.position(size - 1);
            channel.write(ByteBuffer.wrap(new byte[] { 0 }));
        }
    }
}
