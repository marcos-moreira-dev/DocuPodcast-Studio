package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemWorkflowTest {
    @TempDir
    Path tempDir;

    @Test
    void savesPdfSourceCropAsStudyAssetAndSourceReference() throws Exception {
        Path crop = tempDir.resolve("preview.png");
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, Color.BLACK.getRGB());
        ImageIO.write(image, "png", crop.toFile());
        DocumentBlock block = DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH,
                "Solve f(x)=0.", "PDF bbox text",
                Map.of("sourcePage", "12", "bbox", "10,20,300,90"));
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio"));

        var problem = new StudyProblemWorkflow().save(session, java.util.Optional.of(tempDir),
                java.util.List.of(block), "Newton", "x1", null, Map.of("B0001", crop));

        assertEquals("STUDY-CROP-001-01", problem.sources().getFirst().sourceCropAssetId());
        assertEquals(ProjectAssetKind.STUDY_SOURCE_CROP,
                session.project().assets().byId("STUDY-CROP-001-01").orElseThrow().kind());
        assertTrue(Files.isRegularFile(tempDir.resolve("study/problems/prob-001/source/B0001.png")));
    }

    @Test
    void savesPdfVisualRegionAsStudyCropAssetAndSourceReference() throws Exception {
        Path crop = tempDir.resolve("pdf-region.png");
        BufferedImage image = new BufferedImage(12, 10, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 2, Color.BLACK.getRGB());
        ImageIO.write(image, "png", crop.toFile());
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio"));

        var problem = new StudyProblemWorkflow().saveFromSources(session, java.util.Optional.of(tempDir),
                java.util.List.of(StudyProblemSourceDraft.visualRegion(
                        "PDFREG-0001",
                        "",
                        "44",
                        "12.000,20.000,240.000,360.000",
                        crop)),
                "Integral", "", null);

        assertEquals("PDFREG-0001", problem.sources().getFirst().blockId());
        assertEquals("", problem.sources().getFirst().selectedText());
        assertEquals("44", problem.sources().getFirst().sourcePage());
        assertEquals("12.000,20.000,240.000,360.000", problem.sources().getFirst().bbox());
        assertEquals("STUDY-CROP-001-01", problem.sources().getFirst().sourceCropAssetId());
        assertEquals(ProjectAssetKind.STUDY_SOURCE_CROP,
                session.project().assets().byId("STUDY-CROP-001-01").orElseThrow().kind());
        assertTrue(Files.isRegularFile(tempDir.resolve("study/problems/prob-001/source/PDFREG-0001.png")));
    }

    @Test
    void savesExternalImageAsStudyProblemImageAssetAndSourceReference() throws Exception {
        Path imagePath = tempDir.resolve("external.png");
        BufferedImage image = new BufferedImage(14, 9, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 3, Color.BLUE.getRGB());
        ImageIO.write(image, "png", imagePath.toFile());
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio"));

        var problem = new StudyProblemWorkflow().saveFromSources(session, java.util.Optional.of(tempDir),
                java.util.List.of(StudyProblemSourceDraft.externalImage("EXTIMG-0001", imagePath)),
                "Imagen externa", "", null);

        assertEquals("EXTIMG-0001", problem.sources().getFirst().blockId());
        assertEquals("STUDY-IMAGE-001-01", problem.sources().getFirst().sourceCropAssetId());
        assertEquals(ProjectAssetKind.STUDY_PROBLEM_IMAGE,
                session.project().assets().byId("STUDY-IMAGE-001-01").orElseThrow().kind());
        assertTrue(Files.isRegularFile(tempDir.resolve("study/problems/prob-001/source/EXTIMG-0001.png")));
    }

    @Test
    void updatesSolutionTextNotesAndCreatesSolutionImageAsset() throws Exception {
        var problem = new com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem(
                "PROB-001",
                "Newton",
                java.util.List.of(com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40")),
                "Solve f(x)=0.",
                "",
                "",
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                "");
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio")
                .withStudy(new com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer(java.util.List.of(problem))));
        WritableImage image = new WritableImage(2, 2);
        image.getPixelWriter().setArgb(0, 0, 0xff000000);

        var updated = new StudyProblemWorkflow().updateSolution(session, java.util.Optional.of(tempDir),
                "PROB-001", "x1 = 1", image, "repasar derivada");

        assertEquals("x1 = 1", updated.solutionText());
        assertEquals("repasar derivada", updated.notes());
        assertEquals("STUDY-SOLUTION-001", updated.solutionImageAssetId());
        assertTrue(Files.isRegularFile(tempDir.resolve("study/problems/prob-001/solution.png")));
        assertEquals("B0001", updated.sources().getFirst().blockId());
    }

    @Test
    void deletesProblemAssetsAndGeneratedFiles() throws Exception {
        Path crop = tempDir.resolve("study/problems/prob-001/source/B0001.png");
        Path solution = tempDir.resolve("study/problems/prob-001/solution.png");
        Files.createDirectories(crop.getParent());
        Files.write(crop, new byte[]{1});
        Files.write(solution, new byte[]{2});
        var problem = new com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem(
                "PROB-001",
                "Newton",
                java.util.List.of(com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40", "STUDY-CROP-001-01")),
                "Solve f(x)=0.",
                "",
                "STUDY-SOLUTION-001",
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                "");
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio")
                .withStudy(new com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer(java.util.List.of(problem)))
                .withAsset(new com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference("STUDY-CROP-001-01", ProjectAssetKind.STUDY_SOURCE_CROP,
                        "Crop", "study/problems/prob-001/source/B0001.png", "image/png", "crop", "", ""))
                .withAsset(new com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference("STUDY-SOLUTION-001", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-001/solution.png", "image/png", "solution", "", "")));

        new StudyProblemWorkflow().delete(session, java.util.Optional.of(tempDir), "PROB-001");

        assertTrue(session.project().study().technicalProblems().isEmpty());
        assertTrue(session.project().assets().references().isEmpty());
        assertFalse(Files.exists(crop));
        assertFalse(Files.exists(solution));
    }

    @Test
    void exportsSolutionPngAndText() throws Exception {
        Path solution = tempDir.resolve("study/problems/prob-001/solution.png");
        Files.createDirectories(solution.getParent());
        Files.write(solution, new byte[]{7});
        var problem = new com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem(
                "PROB-001",
                "Newton",
                java.util.List.of(com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40")),
                "Solve f(x)=0.",
                "x1 = 1",
                "STUDY-SOLUTION-001",
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                "");
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio")
                .withStudy(new com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer(java.util.List.of(problem)))
                .withAsset(new com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference("STUDY-SOLUTION-001", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-001/solution.png", "image/png", "solution", "", "")));

        Path exportedPng = new StudyProblemWorkflow().exportSolutionImage(session, java.util.Optional.of(tempDir), "PROB-001", tempDir.resolve("out.png"));
        Path exportedTxt = new StudyProblemWorkflow().exportSolutionText(session, "PROB-001", tempDir.resolve("out.txt"));

        assertTrue(Files.isRegularFile(exportedPng));
        assertEquals("x1 = 1", Files.readString(exportedTxt));
    }

    @Test
    void exportsAllSolutionImagesWithManifestAndSkipsProblemsWithoutPng() throws Exception {
        Path solution = tempDir.resolve("study/problems/prob-001/solution.png");
        Files.createDirectories(solution.getParent());
        Files.write(solution, new byte[]{7});
        var withPng = new com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem(
                "PROB-001",
                "Newton",
                java.util.List.of(com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40")),
                "Solve f(x)=0.",
                "",
                "STUDY-SOLUTION-001",
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                "");
        var withoutPng = new com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem(
                "PROB-002",
                "Sin lienzo",
                java.util.List.of(com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference.fullBlock("B0002", "Pregunta", "13", "")),
                "Pregunta",
                "",
                "",
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                java.time.Instant.parse("2026-06-27T10:00:00Z"),
                "");
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Estudio")
                .withStudy(new com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer(java.util.List.of(withPng, withoutPng)))
                .withAsset(new com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference("STUDY-SOLUTION-001", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-001/solution.png", "image/png", "solution", "", "")));

        Path target = tempDir.resolve("exports");
        var report = new StudyProblemWorkflow().exportAllSolutionImages(session, java.util.Optional.of(tempDir), target);

        assertEquals(1, report.exported());
        assertEquals(1, report.skipped());
        assertEquals(0, report.failed());
        assertTrue(Files.list(target).anyMatch(path -> path.getFileName().toString().endsWith("newton.png")));
        String manifest = Files.readString(target.resolve("manifest.txt"));
        assertTrue(manifest.contains("EXPORTADO PROB-001"));
        assertTrue(manifest.contains("OMITIDO PROB-002"));
    }
}
