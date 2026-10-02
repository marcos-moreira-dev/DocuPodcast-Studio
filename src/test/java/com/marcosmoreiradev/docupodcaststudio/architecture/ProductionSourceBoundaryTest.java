package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionSourceBoundaryTest {
    private static final List<Path> PRODUCTION_ROOTS = List.of(
            Path.of("src/main/java"),
            Path.of("studio-media-api/src/main/java"),
            Path.of("studio-ink/src/main/java"),
            Path.of("studio-local-media-adapters/src/main/java"),
            Path.of("studio-launcher/src/main/java"));

    @Test
    void productionDoesNotWriteDirectlyToStandardStreams() throws IOException {
        assertNoSourceContains("System." + "out", "System." + "err");
    }

    @Test
    void desktopDoesNotConstructStaticInkCatalogsOrProviderRegistries() throws IOException {
        assertNoSourceContains(Path.of("src/main/java"),
                "DrawingFeatureCatalog." + "official()",
                "InkInputProviderRegistry." + "localDefaults()",
                "InkInputProviderFactory." + "createDefault()");
    }

    @Test
    void presentationDoesNotOwnTransportProcessesOrProviderClients() throws IOException {
        assertNoSourceContains(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"),
                "java.net.http",
                "ProcessBuilder",
                "org.apache.pdfbox",
                "ImageIO.",
                "ComfyUiVisualEngineClient",
                "ComfyUiWorkflowSpec");
    }

    @Test
    void presentationRemainsProviderNeutral() throws IOException {
        assertNoSourceContains(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"),
                "Piper", "piper", "XTTS", "Xtts", "xtts", "ComfyUI", "ComfyUi", "comfyui",
                "WAN", "LTX", "RIFE", "Rife", "rife", "FFmpeg", "Ffmpeg", "ffmpeg", "Tesseract", "tesseract");
    }

    @Test
    void experienceControllersDoNotPerformDirectFilesystemIo() throws IOException {
        assertNoSourceContains(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/narrative"),
                "Files.");
    }

    @Test
    void productionCompositionCannotCreateAParallelResourceAuthority() throws IOException {
        assertNoSourceContains(
                Path.of("src/main/java"),
                "LocalResourceScheduler.safeDefaults()",
                "new LocalResourceScheduler(",
                "new PriorityResourceScheduler(");
        assertNoSourceContains(
                Path.of("studio-local-media-adapters/src/main/java"),
                "LocalResourceScheduler.safeDefaults()",
                "new LocalResourceScheduler(",
                "new PriorityResourceScheduler(");

        String bootstrap = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/bootstrap/ApplicationBootstrap.java"));
        assertTrue(bootstrap.contains(
                        "PriorityResourceScheduler.incrementalReaderDefaults()"),
                "the application must compose the one global physical scheduler explicitly");
    }

    private static void assertNoSourceContains(String... forbidden) throws IOException {
        for (Path root : PRODUCTION_ROOTS) {
            assertNoSourceContains(root, forbidden);
        }
    }

    private static void assertNoSourceContains(Path root, String... forbidden) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path, forbidden).stream())
                    .toList();
            assertTrue(violations.isEmpty(), () -> "Production boundary violations:\n" + String.join("\n", violations));
        }
    }

    private static List<String> violations(Path source, String[] forbidden) {
        try {
            String text = Files.readString(source);
            return Stream.of(forbidden)
                    .filter(text::contains)
                    .map(token -> source + " contains " + token)
                    .toList();
        } catch (IOException exception) {
            return List.of(source + " could not be read: " + exception.getMessage());
        }
    }
}
