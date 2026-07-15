package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmokeExploratorioMinimoSourceTest {
    @Test
    void smokeExploratorioMinimoHasProtocolChecklistReportScriptAndSampleDocx() throws Exception {
        assertExists("docs/80_TANDA_58C_SMOKE_EXPLORATORIO_MINIMO.md");
        assertExists("docs/testeo/SMOKE_EXPLORATORIO_MINIMO_TANDA_58C.md");
        assertExists("docs/testeo/checklists/58C_smoke_exploratorio_minimo.md");
        assertExists("docs/testeo/reportes/REPORTE_SMOKE_EXPLORATORIO_TANDA_58C.md");
        assertExists("docs/productizacion/ROADMAP_POST_T58B_CIERRE_PRODUCTO.md");
        assertExists("samples/smoke/documento-simple-t58c.docx");
        assertExists("scripts/17-smoke-exploratorio-minimo.bat");

        String protocol = Files.readString(Path.of("docs/testeo/SMOKE_EXPLORATORIO_MINIMO_TANDA_58C.md"));
        assertTrue(protocol.contains("crear/abrir proyecto") || protocol.contains("Crear un proyecto nuevo"));
        assertTrue(protocol.contains("Escuchar documento") || protocol.contains("escuchar/reproducir documento"));
        assertTrue(protocol.contains("Guardar") || protocol.contains("guardar"));
        assertTrue(protocol.contains("Reabrir") || protocol.contains("reabrir"));

        String checklist = Files.readString(Path.of("docs/testeo/checklists/58C_smoke_exploratorio_minimo.md"));
        assertTrue(checklist.contains("Escuchar documento"));
        assertTrue(checklist.contains("Guardar y reabrir"));
        assertTrue(checklist.contains("FUNCIONAL"));
        assertTrue(checklist.contains("UX"));

        String script = Files.readString(Path.of("scripts/17-smoke-exploratorio-minimo.bat"));
        assertTrue(script.contains("setlocal EnableExtensions EnableDelayedExpansion"));
        assertTrue(script.contains("samples\\smoke\\documento-simple-t58c.docx"));
        assertTrue(script.contains("docs\\testeo\\checklists\\58C_smoke_exploratorio_minimo.md"));
    }

    @Test
    void rootDocumentationKeepsT58CProtocolAndDoesNotAdvertiseOld55BAsCurrent() throws Exception {
        String readme = Files.readString(Path.of("README.md"));
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        String validation = Files.readString(Path.of("VALIDATION.md"));

        assertTrue(readme.contains("Tanda 58C"));
        assertTrue(handoff.contains("Tanda 58C"));
        assertTrue(validation.contains("Tanda vigente:"));

        assertFalse(readme.contains("Tanda vigente: 55B"));
        assertFalse(handoff.contains("Tanda vigente: 55B"));
    }

    private static void assertExists(String path) {
        assertTrue(Files.exists(Path.of(path)), path + " debe existir para ejecutar el smoke exploratorio minimo.");
    }
}
