package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisualComfyT100SourceTest {
    @Test
    void pdfVisualRegionContractsUseApplicationBoundaryAndKeepPdfBoxOutOfPresentation() throws Exception {
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectPdfSourcePolicy.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfViewportSelection.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/CapturePdfVisualRegionUseCase.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextLayer.java")));

        String presentation = allJavaUnder(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"));
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        assertTrue(pdfView.contains("PdfViewportSelection"));
        assertTrue(workspace.contains("capturePdfVisualRegion().capture"));
        assertFalse(presentation.contains("PdfBoxRenderEngine"));
        assertFalse(presentation.contains("PdfTextLayer"));
    }

    @Test
    void docsDeclarePdfModePolicyRegionSelectionAndComfyHighMemory() throws Exception {
        String pdf = read("docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md");
        String study = read("docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md");
        String comfy = read("docs/productizacion/IMAGEN_IA_TEATRAL_COMFYUI.md");

        assertTrue(pdf.contains("DOCUMENTARY_STUDIO"));
        assertTrue(pdf.contains("THEATRE_PRODUCTION"));
        assertTrue(pdf.contains("NARRATIVE_VIDEO"));
        assertTrue(pdf.contains("PdfViewportSelection"));
        assertTrue(pdf.contains("PdfTextLayer"));
        assertTrue(pdf.contains("No se debe aceptar como temario principal"));
        assertTrue(study.contains("arrastrar rectangulos sobre la pagina renderizada"));
        assertTrue(study.contains("STUDY_SOURCE_CROP"));
        assertTrue(comfy.contains("VRAM_RAM_OFFLOAD"));
        assertTrue(comfy.contains("ComfyUiRuntimeCapabilityProbe"));
        assertTrue(comfy.contains("Descargar modelo"));
        assertTrue(comfy.contains("Importar modelo local"));
    }

    @Test
    void comfyHighMemoryUsesProbeAndKeepsFluxAccessExplicit() throws Exception {
        String planner = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ComfyUiLaunchArgumentPlanner.java");
        String probe = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ComfyUiRuntimeCapabilityProbe.java");
        String profile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/ImageGenerationMemoryProfile.java");
        String card = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsCard.java");

        assertTrue(profile.contains("HIGH_MEMORY"));
        assertTrue(profile.contains("VRAM_RAM_OFFLOAD"));
        assertTrue(planner.contains("support.supports(flag)"));
        assertTrue(planner.contains("--highvram"));
        assertTrue(planner.contains("--cpu-vae"));
        assertTrue(probe.contains("--help"));
        assertTrue(card.contains("Descargar modelo"));
        assertTrue(card.contains("Importar modelo local"));
        assertTrue(card.contains("Importar runtime"));
        assertTrue(card.contains("requiere acceso o importacion"));
    }

    private static String allJavaUnder(Path root) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (var files = Files.walk(root)) {
            for (Path path : files.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                builder.append(Files.readString(path)).append('\n');
            }
        }
        return builder.toString();
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
