package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectIntegrityRepairSourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void projectIntegrityUseCaseIsNonBinaryAndBrainSide() throws IOException {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/InspectProjectIntegrityUseCase.java");
        assertTrue(useCase.contains("ProjectIntegrityReport"));
        String report = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectIntegrityReport.java");
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectIntegrityStatus.java");
        assertTrue(report.contains("ProjectIntegrityStatus"));
        assertTrue(status.contains("CON_ADVERTENCIAS"));
        assertTrue(status.contains("REQUIERE_REPARACION"));
        assertTrue(useCase.contains("ASSET_CHECKSUM_MISMATCH"));
        assertTrue(useCase.contains("LAYER_TARGET_AUDIO_MISSING"));
        assertTrue(useCase.contains("InspectAudioJobMaintenanceUseCase"));
        assertFalse(useCase.contains("javafx"));
    }

    @Test
    void servicesExposeIntegrityWithoutGrowingShellViewModel() throws IOException {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ProjectApplicationServices.java");
        assertTrue(services.contains("InspectProjectIntegrityUseCase inspectProjectIntegrity"));

        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        assertTrue(factory.contains("new InspectProjectIntegrityUseCase(infrastructure.audioJobRepository())"));

        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertFalse(shell.contains("ASSET_CHECKSUM_MISMATCH"));
        assertFalse(shell.contains("ProjectIntegrityReport"));
    }

    @Test
    void documentationKeepsT77AsBrainIntegrityBeforeFrontend() throws IOException {
        String doc = read("docs/106_TANDA_77_INTEGRIDAD_REPARACION_PROYECTO.md");
        assertTrue(doc.contains("OK / CON_ADVERTENCIAS / REQUIERE_REPARACION"));
        assertTrue(doc.contains("fuente, documento, capas, assets, checksums, jobs, storyboard"));
        assertTrue(doc.contains("no rediseña la interfaz"));

        String roadmap = read("docs/productizacion/ROADMAP_POST_T77_INTEGRIDAD.md");
        assertTrue(roadmap.contains("T78"));
        assertTrue(roadmap.contains("T80A"));
        assertTrue(roadmap.contains("T80B"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8);
    }
}
