package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildStudyProblemsProjectionUseCaseTest {
    @TempDir
    Path tempDir;

    private final BuildStudyProblemsProjectionUseCase useCase = new BuildStudyProblemsProjectionUseCase();

    @Test
    void emptyProjectProducesEmptyProjection() {
        StudyProblemsProjection projection = useCase.build(DocuPodcastProject.createNew("Estudio"), tempDir);

        assertTrue(projection.problems().isEmpty());
        assertTrue(projection.detailsById().isEmpty());
    }

    @Test
    void ordersProblemsByUpdatedAtDescending() {
        TechnicalProblem older = problem("PROB-001", "Viejo", Instant.parse("2026-06-27T10:00:00Z"), "", "");
        TechnicalProblem newer = problem("PROB-002", "Nuevo", Instant.parse("2026-06-27T11:00:00Z"), "texto", "");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(older, newer)));

        StudyProblemsProjection projection = useCase.build(project, tempDir);

        assertEquals("PROB-002", projection.problems().get(0).id());
        assertTrue(projection.problems().get(0).hasSolutionText());
        assertEquals("PROB-001", projection.problems().get(1).id());
        assertEquals(2, projection.totalProblemCount());
    }

    @Test
    void resolvesSafeSourceCropAndSolutionAssets() throws Exception {
        Path crop = tempDir.resolve("study/problems/prob-001/source/B0001.png");
        Path solution = tempDir.resolve("study/problems/prob-001/solution.png");
        Files.createDirectories(crop.getParent());
        Files.write(crop, new byte[]{1});
        Files.write(solution, new byte[]{2});
        TechnicalProblem problem = new TechnicalProblem(
                "PROB-001",
                "Newton",
                List.of(StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40", "STUDY-CROP-001-01")),
                "Solve f(x)=0.",
                "",
                "STUDY-SOLUTION-001",
                Instant.parse("2026-06-27T10:00:00Z"),
                Instant.parse("2026-06-27T10:00:00Z"),
                "");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(problem)))
                .withAsset(new ProjectAssetReference("STUDY-CROP-001-01", ProjectAssetKind.STUDY_SOURCE_CROP,
                        "Crop", "study/problems/prob-001/source/B0001.png", "image/png", "crop", "", ""))
                .withAsset(new ProjectAssetReference("STUDY-SOLUTION-001", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-001/solution.png", "image/png", "solution", "", ""));

        StudyProblemDetail detail = useCase.build(project, tempDir).detail("PROB-001").orElseThrow();

        assertTrue(detail.hasSourceCrops());
        assertTrue(detail.hasSolutionImage());
        assertEquals(crop.toAbsolutePath().normalize(), detail.sources().get(0).sourceCropPath());
        assertEquals(solution.toAbsolutePath().normalize(), detail.solutionImagePath());
    }

    @Test
    void missingAssetsDoNotBreakProjection() {
        TechnicalProblem problem = problem("PROB-001", "Newton", Instant.parse("2026-06-27T10:00:00Z"), "", "MISSING");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(problem)));

        StudyProblemDetail detail = useCase.build(project, tempDir).detail("PROB-001").orElseThrow();

        assertFalse(detail.hasSolutionImage());
        assertFalse(detail.hasSourceCrops());
    }

    @Test
    void queryFindsTitleStatementSourceSolutionAndNotes() {
        TechnicalProblem title = problem("PROB-001", "Newton rapido", "B0001", "Solve f(x)=0.", "12",
                "Body", "", "", "");
        TechnicalProblem statement = problem("PROB-002", "Otro", "B0002", "Interpolacion", "13",
                "Metodo de biseccion", "", "", "");
        TechnicalProblem source = problem("PROB-003", "Otro", "B0003", "Raiz cuadratica especial", "14",
                "Body", "", "", "");
        TechnicalProblem solution = problem("PROB-004", "Otro", "B0004", "Fuente", "15",
                "Body", "usar derivada central", "", "");
        TechnicalProblem notes = problem("PROB-005", "Otro", "B0005", "Fuente", "16",
                "Body", "", "", "repasar monotonia");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(title, statement, source, solution, notes)));

        assertEquals("PROB-001", only(project, new StudyProblemFilter("newton", null, null, "", StudyProblemStatusFilter.ALL)));
        assertEquals("PROB-002", only(project, new StudyProblemFilter("biseccion", null, null, "", StudyProblemStatusFilter.ALL)));
        assertEquals("PROB-003", only(project, new StudyProblemFilter("cuadratica", null, null, "", StudyProblemStatusFilter.ALL)));
        assertEquals("PROB-004", only(project, new StudyProblemFilter("derivada central", null, null, "", StudyProblemStatusFilter.ALL)));
        assertEquals("PROB-005", only(project, new StudyProblemFilter("monotonia", null, null, "", StudyProblemStatusFilter.ALL)));
    }

    @Test
    void filtersByPageRangeAndBlockQuery() {
        TechnicalProblem page3 = problem("PROB-001", "P3", "B0003", "Fuente", "3", "Body", "", "", "");
        TechnicalProblem page10 = problem("PROB-002", "P10", "B0012", "Fuente", "10", "Body", "", "", "");
        TechnicalProblem page20 = problem("PROB-003", "P20", "B0020", "Fuente", "20", "Body", "", "", "");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(page3, page10, page20)));

        StudyProblemsProjection byPage = useCase.build(project, tempDir,
                new StudyProblemFilter("", 5, 15, "", StudyProblemStatusFilter.ALL));
        StudyProblemsProjection byBlock = useCase.build(project, tempDir,
                new StudyProblemFilter("", null, null, "0012", StudyProblemStatusFilter.ALL));

        assertEquals(List.of("PROB-002"), byPage.problems().stream().map(StudyProblemListItem::id).toList());
        assertEquals(List.of("PROB-002"), byBlock.problems().stream().map(StudyProblemListItem::id).toList());
        assertEquals(3, byPage.totalProblemCount());
    }

    @Test
    void filtersBySolutionStateTextCanvasAndCrops() throws Exception {
        Path crop = tempDir.resolve("study/problems/prob-004/source/B0004.png");
        Path solution = tempDir.resolve("study/problems/prob-003/solution.png");
        Files.createDirectories(crop.getParent());
        Files.createDirectories(solution.getParent());
        Files.write(crop, new byte[]{1});
        Files.write(solution, new byte[]{2});
        TechnicalProblem unsolved = problem("PROB-001", "Sin solucion", "B0001", "Fuente", "1", "Body", "", "", "");
        TechnicalProblem text = problem("PROB-002", "Texto", "B0002", "Fuente", "2", "Body", "pasos", "", "");
        TechnicalProblem canvas = problem("PROB-003", "Lienzo", "B0003", "Fuente", "3", "Body", "", "STUDY-SOLUTION-003", "");
        TechnicalProblem crops = problem("PROB-004", "Crop", "B0004", "Fuente", "4", "Body", "", "", "", "STUDY-CROP-004-01");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(unsolved, text, canvas, crops)))
                .withAsset(new ProjectAssetReference("STUDY-SOLUTION-003", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-003/solution.png", "image/png", "solution", "", ""))
                .withAsset(new ProjectAssetReference("STUDY-CROP-004-01", ProjectAssetKind.STUDY_SOURCE_CROP,
                        "Crop", "study/problems/prob-004/source/B0004.png", "image/png", "crop", "", ""));

        assertEquals(List.of("PROB-004", "PROB-001"), ids(project, StudyProblemStatusFilter.UNSOLVED));
        assertEquals(List.of("PROB-002"), ids(project, StudyProblemStatusFilter.WITH_TEXT));
        assertEquals(List.of("PROB-003"), ids(project, StudyProblemStatusFilter.WITH_CANVAS));
        assertEquals(List.of("PROB-004"), ids(project, StudyProblemStatusFilter.WITH_CROPS));
    }

    private static TechnicalProblem problem(String id, String title, Instant updatedAt, String solutionText, String solutionImageAssetId) {
        return new TechnicalProblem(
                id,
                title,
                List.of(StudySourceReference.fullBlock("B0001", "Solve f(x)=0.", "12", "10,20,30,40", "STUDY-CROP-001-01")),
                "Solve f(x)=0.",
                solutionText,
                solutionImageAssetId,
                Instant.parse("2026-06-27T09:00:00Z"),
                updatedAt,
                "");
    }

    private TechnicalProblem problem(String id, String title, String blockId, String selectedText, String sourcePage,
                                     String problemText, String solutionText, String solutionImageAssetId, String notes) {
        return problem(id, title, blockId, selectedText, sourcePage, problemText, solutionText, solutionImageAssetId, notes, "");
    }

    private TechnicalProblem problem(String id, String title, String blockId, String selectedText, String sourcePage,
                                     String problemText, String solutionText, String solutionImageAssetId, String notes,
                                     String sourceCropAssetId) {
        return new TechnicalProblem(
                id,
                title,
                List.of(StudySourceReference.fullBlock(blockId, selectedText, sourcePage, "10,20,30,40", sourceCropAssetId)),
                problemText,
                solutionText,
                solutionImageAssetId,
                Instant.parse("2026-06-27T09:00:00Z"),
                Instant.parse("2026-06-27T10:00:00Z").plusSeconds(Integer.parseInt(id.substring(id.length() - 3))),
                notes);
    }

    private String only(DocuPodcastProject project, StudyProblemFilter filter) {
        StudyProblemsProjection projection = useCase.build(project, tempDir, filter);
        assertEquals(1, projection.problems().size());
        return projection.problems().getFirst().id();
    }

    private List<String> ids(DocuPodcastProject project, StudyProblemStatusFilter status) {
        return useCase.build(project, tempDir, new StudyProblemFilter("", null, null, "", status))
                .problems()
                .stream()
                .map(StudyProblemListItem::id)
                .toList();
    }
}
