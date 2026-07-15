package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductRoadmapContinuitySourceTest {
    @Test
    void roadmapDocumentsAllRemainingTandasAndTests() throws Exception {
        String roadmap = read("docs/productizacion/PLAN_MAESTRO_TANDAS_49_63.md");
        assertTrue(roadmap.contains("Tanda 49 — Auditoría de scaffolding visual + contrato FFmpeg embebido"));
        assertTrue(roadmap.contains("Tanda 50 — Modernización visual base"));
        assertTrue(roadmap.contains("Tanda 58 — Exportación de video simple 2K con FFmpeg embebido"));
        assertTrue(roadmap.contains("Tanda 63 — Packaging / Release Candidate"));
        assertTrue(roadmap.contains("Toda tanda futura debe agregar o actualizar tests")
                || read("docs/productizacion/MATRIZ_TESTS_TANDAS_RESTANTES.md").contains("Toda tanda futura debe agregar o actualizar tests"));
        assertTrue(read("docs/productizacion/MATRIZ_TESTS_TANDAS_RESTANTES.md").contains("SimpleVideo2KExportPolicyTest"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
