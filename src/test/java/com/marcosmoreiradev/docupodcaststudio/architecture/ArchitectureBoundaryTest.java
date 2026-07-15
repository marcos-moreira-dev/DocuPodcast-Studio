package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level architecture guardrails inspired by the DMS anti-facade tests. */
final class ArchitectureBoundaryTest {
    private static final Path MAIN = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio");

    @Test
    void domainDoesNotDependOnOuterLayersOrJavaFx() throws Exception {
        List<String> forbidden = List.of(
                "import com.marcosmoreiradev.docupodcaststudio.application.",
                "import com.marcosmoreiradev.docupodcaststudio.infrastructure.",
                "import com.marcosmoreiradev.docupodcaststudio.presentation.",
                "import javafx."
        );
        assertNoImportsUnder(MAIN.resolve("domain"), forbidden);
    }

    @Test
    void applicationDoesNotDependOnInfrastructurePresentationOrJavaFx() throws Exception {
        List<String> forbidden = List.of(
                "import com.marcosmoreiradev.docupodcaststudio.infrastructure.",
                "import com.marcosmoreiradev.docupodcaststudio.presentation.",
                "import javafx."
        );
        assertNoImportsUnder(MAIN.resolve("application"), forbidden);
    }

    @Test
    void presentationDoesNotImportInfrastructureAdaptersDirectly() throws Exception {
        assertNoImportsUnder(MAIN.resolve("presentation"), List.of("import com.marcosmoreiradev.docupodcaststudio.infrastructure."));
    }

    @Test
    void javaFxIsScopedToPresentationBootstrapAndApplicationEntrypoint() throws Exception {
        try (Stream<Path> stream = Files.walk(MAIN)) {
            List<Path> offenders = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "import javafx."))
                    .filter(path -> !isAllowedJavaFxHost(path))
                    .toList();
            assertTrue(offenders.isEmpty(), "JavaFX imports fuera de presentation/bootstrap/app: " + offenders);
        }
    }

    private static void assertNoImportsUnder(Path root, List<String> forbiddenImports) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> javaFiles = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            assertFalse(javaFiles.isEmpty(), "Guardarrail activo: se esperaban fuentes en " + root);
            List<String> offenders = javaFiles.stream()
                    .flatMap(path -> forbiddenImports.stream()
                            .filter(pattern -> contains(path, pattern))
                            .map(pattern -> root.relativize(path) + " contiene " + pattern))
                    .toList();
            assertTrue(offenders.isEmpty(), String.join(System.lineSeparator(), offenders));
        }
    }

    private static boolean contains(Path path, String pattern) {
        try {
            return Files.readString(path).contains(pattern);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + path, exception);
        }
    }

    private static boolean isAllowedJavaFxHost(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/presentation/")
                || normalized.contains("/bootstrap/")
                || normalized.endsWith("/DocuPodcastStudioApp.java");
    }
}
