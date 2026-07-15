package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CssModularitySourceTest {
    @Test
    void lightStylesheetIsAssemblerAndComponentCssIsSplitIntoModules() throws Exception {
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String actions = Files.readString(Path.of("src/main/resources/css/components/actions.css"));
        String cards = Files.readString(Path.of("src/main/resources/css/components/cards.css"));
        String settings = Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));

        assertTrue(imports.contains("components/actions.css"));
        assertTrue(imports.contains("components/cards.css"));
        assertTrue(imports.contains("components/settings-shell.css"));
        assertTrue(imports.contains("compat-legacy.css"));
        assertFalse(imports.contains(".document-block"), "docupodcast-light.css debe quedar como ensamblador, no como módulo vivo gigante.");
        assertTrue(actions.contains("ui-primary-action-strip"));
        assertTrue(cards.contains("ui-empty-state"));
        assertTrue(settings.contains("ui-settings-page"));
    }

    @Test
    void cssFilesStayWithinHumanReviewBudget() throws Exception {
        List<Path> cssFiles;
        try (var stream = Files.walk(Path.of("src/main/resources/css"))) {
            cssFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".css"))
                    .toList();
        }
        for (Path path : cssFiles) {
            long lines;
            try (var lineStream = Files.lines(path)) {
                lines = lineStream.count();
            }
            assertTrue(lines <= 650, path + " supera el presupuesto humano de CSS modular: " + lines + " líneas");
        }
    }
}
