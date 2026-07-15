package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** ESTANDARES-PENDIENTES-RC1 protects the handoff map for the remaining RC standards. */
final class StandardsPendingRc1SourceTest {
    @Test
    void currentDocumentationContainsDetailedRemainingStandards() throws Exception {
        String doc = read("DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md");
        assertTrue(doc.contains("RUNTIME-PATHS-RF1"));
        assertTrue(doc.contains("EXTERNAL-PROCESS-RUNNER-RF1"));
        assertTrue(doc.contains("MODEL-ARTIFACT-CONTRACT-RF1"));
        assertTrue(doc.contains("MANAGED-DOWNLOAD-RF1"));
        assertTrue(doc.contains("ENGINE-READINESS-UI-HF1"));
        assertTrue(doc.contains("XTTS-PYTORCH-CUDA-INSTALL-HF1"));
        assertTrue(doc.contains("PERSISTENCE-RC1"));
        assertTrue(doc.contains("RC-GATE1"));
        assertTrue(doc.contains("Todo lo visible en DocuPodcast Studio debe justificar su presencia"));
        assertTrue(doc.contains("Si la app cambia lo que el usuario pidió, debe mostrar message box"));
    }

    @Test
    void planIndexPointsToStandardsContinuationFile() throws Exception {
        String index = read("DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/00_INDICE.md");
        String plan = read("DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md");
        assertTrue(index.contains("08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md"));
        assertTrue(plan.contains("RUNTIME-PATHS-RF1"));
        assertTrue(plan.contains("EXTERNAL-PROCESS-RUNNER-RF1"));
        assertTrue(plan.contains("Nada visible sin propósito operativo"));
    }

    @Test
    void cssAliasForExampleReadinessIsDeclared() throws Exception {
        String tokens = read("src/main/resources/css/tokens.css");
        String examples = read("src/main/resources/css/examples/examples.css");
        assertTrue(examples.contains("-fx-text-fill: -dp-text-secondary;"));
        assertTrue(tokens.contains("-dp-text-secondary:"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
