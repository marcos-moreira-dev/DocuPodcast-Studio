package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class NoInlineStyleSourceTest {
    @Test
    void presentationUsesCssClassesInsteadOfInlineStyles() throws Exception {
        try (var stream = Files.walk(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"))) {
            for (Path path : stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("setStyle("), path + " usa estilos inline; crear clase CSS o componente transversal.");
            }
        }
    }
}
