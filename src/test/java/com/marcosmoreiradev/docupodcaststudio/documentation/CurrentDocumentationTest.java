package com.marcosmoreiradev.docupodcaststudio.documentation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CurrentDocumentationTest {
    @Test
    void currentDocumentationHasOneIndexedHome() {
        List<Path> required = List.of(
                Path.of("README.md"),
                Path.of("docs/README.md"),
                Path.of("docs/architecture/overview.md"),
                Path.of("docs/architecture/runtime-layout.md"),
                Path.of("docs/development/build-test.md"),
                Path.of("docs/operations/local-engines.md"),
                Path.of("docs/operations/smoke-and-release.md"),
                Path.of("docs/product/current-status.md")
        );
        required.forEach(path -> assertTrue(Files.isRegularFile(path), "Missing current documentation: " + path));
    }

    @Test
    void historicalDocumentationRootsStayInGitHistory() {
        List<Path> historicalRoots = List.of(
                Path.of("00_MEMORIA_PROYECTO"),
                Path.of("DOCUMENTACION"),
                Path.of("DOCUMENTACION_ACTUAL"),
                Path.of("DOCUMENTACION_ESTRATEGICA")
        );
        historicalRoots.forEach(path -> assertFalse(Files.exists(path), "Historical root returned: " + path));
    }
}
