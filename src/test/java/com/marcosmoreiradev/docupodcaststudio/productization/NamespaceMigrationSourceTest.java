package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NamespaceMigrationSourceTest {
    private static final long MAX_TEXT_FILE_BYTES = 2_000_000;
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java", ".md", ".xml", ".properties", ".css", ".fxml",
            ".json", ".bat", ".ps1", ".txt", ".yml", ".yaml"
    );

    @Test
    void projectUsesMarcosMoreiraDevNamespace() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("<groupId>com.marcosmoreiradev</groupId>"));
        assertTrue(moduleInfo.contains("module com.marcosmoreiradev.docupodcaststudio"));
        assertTrue(Files.isDirectory(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio")));
        assertTrue(Files.isDirectory(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio")));
        assertFalse(projectTextContainsLegacyNamespace());
    }

    private boolean projectTextContainsLegacyNamespace() throws IOException {
        String legacy = "com." + "omar" + ".docupodcaststudio";
        String legacyPath = "com/" + "omar" + "/docupodcaststudio";
        try (Stream<Path> paths = Files.walk(Path.of("."))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(NamespaceMigrationSourceTest::isSearchableProjectText)
                    .anyMatch(path -> contains(path, legacy, legacyPath));
        }
    }

    private static boolean isSearchableProjectText(Path path) {
        String normalized = path.normalize().toString().replace('\\', '/');
        if (normalized.contains("/target/")
                || normalized.contains("/.git/")
                || normalized.contains("/models/")
                || normalized.contains("/tools/")
                || normalized.contains("/jobs/")
                || normalized.contains("/runtime/")
                || normalized.contains("/exports/")) {
            return false;
        }
        String lowerName = path.getFileName().toString().toLowerCase();
        boolean textExtension = TEXT_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!textExtension) {
            return false;
        }
        try {
            return Files.size(path) <= MAX_TEXT_FILE_BYTES;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean contains(Path path, String legacy, String legacyPath) {
        try {
            String content = Files.readString(path);
            return content.contains(legacy) || content.contains(legacyPath);
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
