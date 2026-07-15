package com.marcosmoreiradev.docupodcaststudio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StrategicDocumentationTest {
    @Test
    void strategicDocumentationFolderExistsAndMentionsImplementationPlan() throws IOException {
        Path root = Path.of("DOCUMENTACION_ESTRATEGICA");
        assertTrue(Files.isDirectory(root), "Debe existir DOCUMENTACION_ESTRATEGICA en la raiz");
        String readme = Files.readString(root.resolve("00_LEEME_PRIMERO.md"));
        assertTrue(readme.contains("Documento fuente → lectura preparada → voces/audio/visuales"));
        assertTrue(Files.isDirectory(root.resolve("PLAN_IMPLEMENTACION_DETALLADO")));
    }

    @Test
    void toolchainDocumentationMentionsTemurin() throws IOException {
        String text = Files.readString(Path.of("DOCUMENTACION_ESTRATEGICA/05_ENTORNO_JAVA_21_TEMURIN_TOOLCHAIN.md"));
        assertTrue(text.contains("Eclipse Temurin"));
        assertTrue(text.contains("Maven Toolchain"));
    }
}
