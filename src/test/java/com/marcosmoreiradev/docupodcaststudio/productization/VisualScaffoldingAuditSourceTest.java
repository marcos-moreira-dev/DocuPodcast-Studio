package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualScaffoldingAuditSourceTest {
    @Test
    void visualScaffoldingAuditProtectsMainReaderFromTechnicalWarehouse() throws Exception {
        String audit = read("docs/productizacion/AUDITORIA_SCAFFOLDING_VISUAL_OPERATIVO.md");
        assertTrue(audit.contains("Operación principal"));
        assertTrue(audit.contains("Configuración"));
        assertTrue(audit.contains("Infraestructura interna"));
        assertTrue(audit.contains("sin paneles anidados innecesarios"));
        assertTrue(audit.contains("sin card dentro de card porque sí"));
        assertTrue(audit.contains("Documento"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
