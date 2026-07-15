
package com.marcosmoreiradev.docupodcaststudio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMemoryDocumentationTest {

    @Test
    void rootMemoryFolderDocumentsImplementationPlanAndWordFirstDecision() throws Exception {
        Path root = Path.of("00_MEMORIA_PROYECTO");
        assertTrue(Files.isDirectory(root), "Debe existir la carpeta raíz de memoria viva.");
        assertTrue(Files.exists(root.resolve("20_TANDAS_IMPLEMENTACION_DETALLADAS.md")),
                "Debe existir el plan detallado de implementación.");
        String wordFirst = Files.readString(root.resolve("07_WORD_DOCX_FIRST.md"));
        assertTrue(wordFirst.contains("DOCX"), "La memoria debe fijar DOCX como entrada prioritaria.");
    }
    @Test
    void pomRequiresTemurinToolchain() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("maven-toolchains-plugin"));
        assertTrue(pom.contains("<version>21</version>"));
        assertTrue(pom.contains("<vendor>temurin</vendor>"));
    }

}
