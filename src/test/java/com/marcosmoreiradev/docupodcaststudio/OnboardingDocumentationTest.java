package com.marcosmoreiradev.docupodcaststudio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingDocumentationTest {

    @Test
    void handoffDocumentationExists() {
        assertTrue(Files.exists(Path.of("AI_HANDOFF.md")), "AI_HANDOFF.md debe existir");
        assertTrue(Files.exists(Path.of("DOCUMENTACION/00_LEEME_PRIMERO.md")), "La documentacion raiz debe existir");
        assertTrue(Files.exists(Path.of("DOCUMENTACION/23_PLAN_IMPLEMENTACION_POR_TANDAS.md")), "Debe existir el plan de implementacion");
        assertTrue(Files.exists(Path.of("toolchains.example.xml")), "Debe existir ejemplo de Maven Toolchain");
    }
}
