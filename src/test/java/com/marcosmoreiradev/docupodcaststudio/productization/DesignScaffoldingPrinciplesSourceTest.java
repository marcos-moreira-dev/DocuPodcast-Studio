package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DesignScaffoldingPrinciplesSourceTest {
    @Test
    void designPrinciplesDocumentDefinesProductCompassAndScaffoldingRules() throws Exception {
        String principles = Files.readString(Path.of("docs/productizacion/PRINCIPIOS_RECTORES_DISENO_SCAFFOLDING.md"));
        String roadmap = Files.readString(Path.of("docs/productizacion/ROADMAP_POST_T59_REDISSENO_GUIADO.md"));
        String readme = Files.readString(Path.of("README.md"));
        String validation = Files.readString(Path.of("VALIDATION.md"));

        assertTrue(principles.contains("Documento primero"));
        assertTrue(principles.contains("Menos scaffolding visible"));
        assertTrue(principles.contains("Complejidad progresiva"));
        assertTrue(principles.contains("Acciones por intención"));
        assertTrue(principles.contains("Componentes GUI transversales"));
        assertTrue(principles.contains("Contrato honesto de capacidades"));
        assertTrue(principles.contains("Refactor guiado por uso real"));
        assertTrue(principles.contains("abrir Word → leer cómodo → escuchar"));

        assertTrue(roadmap.contains("T59A — Componentes GUI transversales"));
        assertTrue(roadmap.contains("T59B — Limpieza UX Documento"));
        assertTrue(roadmap.contains("T60 — Refactor coordinadores"));
        assertTrue(roadmap.contains("tokens CSS huérfanos"));

        assertTrue(readme.contains("Tanda 81D"));
        assertTrue(validation.contains("Tanda vigente:"));
    }
}
