package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiVisualEngineClientTest {
    @TempDir
    Path tempDir;

    @Test
    void generateQueuesDownloadsAndResizesPngFromComfyUiLikeServer() throws Exception {
        AtomicReference<String> submittedPrompt = new AtomicReference<>("");
        HttpServer server = startFakeComfyUi(submittedPrompt, png(768, 432));
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            ComfyUiVisualEngineClient client = new ComfyUiVisualEngineClient(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
            VisualEngineRequest request = new VisualEngineRequest(
                    "teatro de aviacion con luces calidas",
                    "texto, marca de agua",
                    "v1-5-pruned-emaonly-fp16.safetensors",
                    14,
                    6.5,
                    1,
                    1920,
                    1080,
                    tempDir.resolve("generated"),
                    "Escena IA 01");

            VisualEngineResult result = client.generate(baseUrl, Duration.ofSeconds(3), request,
                    GenerationAttemptPolicy.defaults(), GenerationTaskKind.IMAGE_CANDIDATE, null);

            assertTrue(Files.isRegularFile(result.outputPath()));
            BufferedImage output = ImageIO.read(result.outputPath().toFile());
            assertEquals(1920, output.getWidth());
            assertEquals(1080, output.getHeight());
            assertEquals("prompt-1", result.promptId());
            assertTrue(submittedPrompt.get().contains("teatro de aviacion con luces calidas"));
            assertTrue(submittedPrompt.get().contains("\"width\":768"));
            assertTrue(submittedPrompt.get().contains("\"height\":432"));
            assertTrue(submittedPrompt.get().contains("\"filename_prefix\":\"escena-ia-01\""));
            assertTrue(result.diagnostic().contains("baseGeneration=768x432"));
            assertTrue(result.diagnostic().contains("resize=java2d-bicubic"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestKeepsSafeFallbackWhenFilenamePrefixHasNoUsableCharacters() {
        VisualEngineRequest request = new VisualEngineRequest(
                "prompt",
                "",
                "checkpoint.safetensors",
                1,
                1.0,
                1,
                512,
                512,
                tempDir,
                "???");

        assertEquals("docupodcast-visual", request.filenamePrefix());
    }

    @Test
    void waitDurationPreservesSixHourGenerationDeadline() {
        Duration sixHours = Duration.ofHours(6);

        assertEquals(sixHours, ComfyUiVisualEngineClient.waitDuration(
                sixHours,
                ComfyUiWorkflowSpec.sd15()));
        assertEquals(sixHours, ComfyUiVisualEngineClient.waitDuration(
                sixHours,
                ComfyUiWorkflowSpec.fluxForTarget(
                        "flux.safetensors",
                        "ae.safetensors",
                        "clip_l.safetensors",
                        "t5xxl.safetensors",
                        1920,
                        1080)));
    }

    @Test
    void systemStatsParsesBackendNameIndexAndRuntimeVersions() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/system_stats", exchange -> respond(exchange, 200, "application/json", """
                {
                  "system": {
                    "os": "nt",
                    "python_version": "3.11.9",
                    "pytorch_version": "2.5.1+cu121"
                  },
                  "devices": [
                    {"name": "cuda:0 NVIDIA GeForce GTX 1650", "type": "cuda", "index": 0}
                  ]
                }
                """.getBytes()));
        server.start();
        try {
            ComfyUiVisualEngineClient client = new ComfyUiVisualEngineClient(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

            ComfyUiSystemStats stats = client.systemStats(
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));

            assertEquals("3.11.9", stats.pythonVersion());
            assertEquals("2.5.1+cu121", stats.pytorchVersion());
            assertEquals("cuda", stats.devices().getFirst().type());
            assertEquals(0, stats.devices().getFirst().index());
            assertTrue(stats.devices().getFirst().name().contains("GTX 1650"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startFakeComfyUi(AtomicReference<String> submittedPrompt, byte[] png) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/system_stats", exchange -> respond(exchange, 200, "application/json", "{}".getBytes()));
        server.createContext("/prompt", exchange -> {
            submittedPrompt.set(new String(exchange.getRequestBody().readAllBytes()));
            respond(exchange, 200, "application/json", "{\"prompt_id\":\"prompt-1\"}".getBytes());
        });
        server.createContext("/history", exchange -> respond(exchange, 200, "application/json",
                "{\"prompt-1\":{\"outputs\":{\"7\":{\"images\":[{\"filename\":\"candidate.png\",\"subfolder\":\"\",\"type\":\"output\"}]}}}}"
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

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
