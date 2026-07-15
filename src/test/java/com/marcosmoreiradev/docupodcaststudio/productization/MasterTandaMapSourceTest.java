package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the consolidated map that decides when alignment work ends and RC cleanup begins. */
final class MasterTandaMapSourceTest {
    @Test
    void currentDocumentationLinksToConsolidatedTandaMap() throws Exception {
        String readme = read("DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md");
        String index = read("DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/00_INDICE.md");

        assertTrue(readme.contains("13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md"));
        assertTrue(index.contains("../13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md"));
    }

    @Test
    void consolidatedMapDefinesAlignmentGateAndRemainingTandas() throws Exception {
        String map = read("DOCUMENTACION_ACTUAL/13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md");

        assertTrue(map.contains("| 4 | Nucleo teatral consolidado |"));
        assertTrue(map.contains("| 10 | Compatibilidad y cierre de alineacion |"));
        assertTrue(map.contains("| 16 | Anti-placeholder, diagnostico, packaging y RC gate |"));
        assertTrue(map.contains("La alineacion contra Word/respaldo termina al cerrar la Tanda 10"));
        assertTrue(map.contains("No quedan capacidades solo en respaldo sin decision documentada"));
    }

    @Test
    void documentationCleanupIsBlockedUntilTestCleanup() throws Exception {
        String map = read("DOCUMENTACION_ACTUAL/13_MAPA_MAESTRO_CONSOLIDADO_TANDAS.md");

        assertTrue(map.contains("No ejecutar limpieza documental antes de limpiar tests"));
        assertTrue(map.contains("TEST-CLEAN1"));
        assertTrue(map.contains("DOCS-CLEAN1"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
