package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndOpensDocuPodcastProjectWithAssets() throws Exception {
        DocuPodcastProject base = DocuPodcastProject.createNew("Notas de dinosaurios");
        DocuPodcastProject project = base.withMetadata(base.metadata().withKind(ProjectKind.DOCUMENT_ONLY))
                .withAsset(new ProjectAssetReference("SRC-001", ProjectAssetKind.SOURCE_DOCUMENT,
                        "Notas Word", "source/dinosaurios.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "Documento fuente Word/DOCX", "sha256:demo", "Fuente primaria del usuario"));

        Path file = tempDir.resolve("dinosaurios.docupodcast.json");
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        repository.save(project, file);
        DocuPodcastProject opened = repository.open(file);

        assertTrue(Files.exists(file));
        assertEquals("Notas de dinosaurios", opened.metadata().title());
        assertEquals(ProjectKind.DOCUMENT_ONLY, opened.metadata().kind());
        assertEquals(ProjectMode.DOCUMENTARY_STUDIO, opened.metadata().mode());
        assertEquals(1, opened.assets().size());
        assertEquals("source/dinosaurios.docx", opened.assets().byId("SRC-001").orElseThrow().relativePath());
        assertEquals("Documento académico Word", opened.readingProfile().name());
        assertTrue(opened.voiceLibrary().voiceById("VOC-NARRATOR").isPresent());
    }

    @Test
    void writtenJsonContainsStableTopLevelSections() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Proyecto mínimo");
        String json = new DocuPodcastProjectJsonWriter().write(project);

        assertTrue(json.contains("\"formatVersion\": 3"));
        assertTrue(json.contains("\"project\""));
        assertTrue(json.contains("\"mode\": \"DOCUMENTARY_STUDIO\""));
        assertTrue(json.contains("\"assets\""));
        assertTrue(json.contains("\"readingProfile\""));
        assertTrue(json.contains("\"voiceLibrary\""));
        assertTrue(json.contains("\"narrativeLayers\""));
        assertTrue(json.contains("\"study\""));
        assertTrue(json.contains("\"technicalProblems\""));
        assertTrue(json.contains("\"view\""));
    }

    @Test
    void savesAndReadsStudyTechnicalProblems() throws Exception {
        Instant now = Instant.parse("2026-06-27T12:00:00Z");
        TechnicalProblem problem = new TechnicalProblem(
                "PROB-001",
                "Metodo de Newton",
                List.of(
                        StudySourceReference.fullBlock("B0007", "Solve f(x)=0 by Newton method.", "12", "10,20,300,60"),
                        StudySourceReference.visualRegion("PDFREG-0001", "", "13", "15.000,25.000,220.000,330.000", "STUDY-CROP-001-01")),
                "Solve f(x)=0 by Newton method.",
                "x_{n+1}=x_n-f(x_n)/f'(x_n)",
                "STUDY-SOLUTION-001",
                now,
                now,
                "fixture");
        DocuPodcastProject project = DocuPodcastProject.createNew("Estudio")
                .withStudy(new StudyProjectLayer(List.of(problem)))
                .withAsset(new ProjectAssetReference("STUDY-SOLUTION-001", ProjectAssetKind.STUDY_SOLUTION_IMAGE,
                        "Solucion", "study/problems/prob-001/solution.png", "image/png",
                        "Solucion manuscrita", "", ""))
                .withAsset(new ProjectAssetReference("STUDY-CROP-001-01", ProjectAssetKind.STUDY_SOURCE_CROP,
                        "Fuente PDF", "study/problems/prob-001/source/PDFREG-0001.png", "image/png",
                        "Recorte PDF", "", ""));
        Path file = tempDir.resolve("study.docupodcast.json");
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        repository.save(project, file);
        DocuPodcastProject opened = repository.open(file);

        assertEquals(1, opened.study().technicalProblems().size());
        TechnicalProblem openedProblem = opened.study().technicalProblems().getFirst();
        assertEquals("PROB-001", openedProblem.id());
        assertEquals("B0007", openedProblem.sources().getFirst().blockId());
        assertEquals("12", openedProblem.sources().getFirst().sourcePage());
        assertEquals("PDFREG-0001", openedProblem.sources().get(1).blockId());
        assertEquals("13", openedProblem.sources().get(1).sourcePage());
        assertEquals("15.000,25.000,220.000,330.000", openedProblem.sources().get(1).bbox());
        assertEquals("STUDY-CROP-001-01", openedProblem.sources().get(1).sourceCropAssetId());
        assertEquals("STUDY-SOLUTION-001", openedProblem.solutionImageAssetId());
        assertEquals(ProjectAssetKind.STUDY_SOLUTION_IMAGE, opened.assets().byId("STUDY-SOLUTION-001").orElseThrow().kind());
        assertEquals(ProjectAssetKind.STUDY_SOURCE_CROP, opened.assets().byId("STUDY-CROP-001-01").orElseThrow().kind());
    }

    @Test
    void savesAndReadsCustomizedReadingProfile() throws Exception {
        DocuPodcastProject base = DocuPodcastProject.createNew("Proyecto perfil");
        ReadingProfile customized = new ReadingProfile(
                base.readingProfile().id(),
                "Perfil notas legales",
                "Ajuste de prueba",
                base.readingProfile().headingRules(),
                ImageNarrationPolicy.IGNORE_IMAGES,
                base.readingProfile().tablePolicy()
        );
        Path file = tempDir.resolve("perfil.docupodcast.json");
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        repository.save(base.withReadingProfile(customized), file);
        DocuPodcastProject opened = repository.open(file);

        assertEquals("Perfil notas legales", opened.readingProfile().name());
        assertEquals(ImageNarrationPolicy.IGNORE_IMAGES, opened.readingProfile().imagePolicy());
    }

    @Test
    void savesAndReadsExplicitOfficialProjectMode() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO);
        Path file = tempDir.resolve("video.docupodcast.json");
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        repository.save(project, file);
        DocuPodcastProject opened = repository.open(file);

        assertEquals(ProjectMode.NARRATIVE_VIDEO, opened.metadata().mode());
        assertEquals(ProjectKind.EMPTY, opened.metadata().kind());
    }

    @Test
    void infersOfficialModeWhenLegacyJsonHasNoMode() throws Exception {
        DocuPodcastProject base = DocuPodcastProject.createNew("Legacy")
                .withMetadata(DocuPodcastProject.createNew("Legacy").metadata().withKind(ProjectKind.FULL_PROJECT))
                .withTheatre(new TheatreProjectLayer(
                        List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-1")),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of()));
        String json = removeModeLine(new DocuPodcastProjectJsonWriter().write(base));

        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertEquals(ProjectMode.THEATRE_PRODUCTION, opened.metadata().mode());
        assertEquals(ProjectKind.FULL_PROJECT, opened.metadata().kind());
    }

    @Test
    void legacyDocumentaryJsonWithoutModeReopensAsDocumentaryAndMaterializesModeOnSave() throws Exception {
        DocuPodcastProject base = DocuPodcastProject.createNew("Legacy documental")
                .withMetadata(DocuPodcastProject.createNew("Legacy documental").metadata().withKind(ProjectKind.DOCUMENT_ONLY))
                .withAsset(new ProjectAssetReference("SRC-LEGACY", ProjectAssetKind.SOURCE_DOCUMENT,
                        "Fuente legacy", "source/legacy.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "Documento fuente", "sha256:legacy", "Fuente primaria"));
        Path file = tempDir.resolve("legacy-documental.docupodcast.json");
        Files.writeString(file, removeModeLine(new DocuPodcastProjectJsonWriter().write(base)));
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        DocuPodcastProject opened = repository.open(file);
        repository.save(opened, file);
        String saved = Files.readString(file);

        assertEquals(ProjectMode.DOCUMENTARY_STUDIO, opened.metadata().mode());
        assertEquals(ProjectKind.DOCUMENT_ONLY, opened.metadata().kind());
        assertEquals("source/legacy.docx", opened.assets().byId("SRC-LEGACY").orElseThrow().relativePath());
        assertTrue(saved.contains("\"mode\": \"DOCUMENTARY_STUDIO\""));
    }

    @Test
    void legacyNarrativeJsonWithoutModeReopensAsNarrativeAndKeepsKind() throws Exception {
        DocuPodcastProject base = DocuPodcastProject.createNew("Legacy narrativo")
                .withMetadata(DocuPodcastProject.createNew("Legacy narrativo").metadata().withKind(ProjectKind.FULL_PROJECT))
                .withAsset(narrationScriptAsset());
        Path file = tempDir.resolve("legacy-narrativo.docupodcast.json");
        Files.writeString(file, removeModeLine(new DocuPodcastProjectJsonWriter().write(base)));
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        DocuPodcastProject opened = repository.open(file);
        repository.save(opened, file);
        String saved = Files.readString(file);

        assertEquals(ProjectMode.NARRATIVE_VIDEO, opened.metadata().mode());
        assertEquals(ProjectKind.FULL_PROJECT, opened.metadata().kind());
        assertEquals(0, opened.theatre().intervenciones().size());
        assertTrue(opened.assets().containsKind(ProjectAssetKind.NARRATION_SCRIPT));
        assertTrue(saved.contains("\"mode\": \"NARRATIVE_VIDEO\""));
    }

    @Test
    void legacyTheatreJsonWithoutModeReopensAsTheatreAndKeepsTheatreLayer() throws Exception {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-1")),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-LEGACY", "Actor legacy", List.of("ACTOR"), "")),
                List.of(), List.of(), List.of(),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-1", "Acto legacy", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-1", "Escena legacy", "", "ACT-1", "")),
                List.of(), List.of(), List.of(), List.of(), List.of());
        DocuPodcastProject base = DocuPodcastProject.createNew("Legacy teatro")
                .withMetadata(DocuPodcastProject.createNew("Legacy teatro").metadata().withKind(ProjectKind.FULL_PROJECT))
                .withAsset(narrationScriptAsset())
                .withTheatre(theatre);
        Path file = tempDir.resolve("legacy-teatro.docupodcast.json");
        Files.writeString(file, removeModeLine(new DocuPodcastProjectJsonWriter().write(base)));
        DocuPodcastProjectFileRepository repository = new DocuPodcastProjectFileRepository();

        DocuPodcastProject opened = repository.open(file);
        repository.save(opened, file);
        String saved = Files.readString(file);

        assertEquals(ProjectMode.THEATRE_PRODUCTION, opened.metadata().mode());
        assertEquals(ProjectKind.FULL_PROJECT, opened.metadata().kind());
        assertEquals("INTERVENCION-1", opened.theatre().intervenciones().getFirst().id());
        assertEquals("CHR-LEGACY", opened.theatre().characters().getFirst().id());
        assertTrue(opened.assets().containsKind(ProjectAssetKind.NARRATION_SCRIPT));
        assertTrue(saved.contains("\"mode\": \"THEATRE_PRODUCTION\""));
        assertTrue(saved.contains("\"theatre\""));
        assertTrue(saved.contains("\"characters\""));
    }

    private static ProjectAssetReference narrationScriptAsset() {
        return new ProjectAssetReference("SCRIPT-LEGACY", ProjectAssetKind.NARRATION_SCRIPT,
                "Guion legacy", "script/narration-script.json", "application/json",
                "Guion narrativo legado", "sha256:legacy-script", "Fixture de compatibilidad");
    }

    private static String removeModeLine(String json) {
        return json.replaceAll("(?m)^\\s+\\\"mode\\\": \\\"[A-Z_]+\\\",\\R", "");
    }

}
