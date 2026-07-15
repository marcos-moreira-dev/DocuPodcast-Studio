package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoGrammarTemplate;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.grammar.FileSystemProjectSemanticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportProjectGrammarMarkdownUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void narrativeImportMaterializesProjectSemanticsJsonWithFragmentBindings() throws Exception {
        ImportProjectGrammarMarkdownUseCase useCase =
                new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository());
        Path markdown = tempDir.resolve("video.md");
        Files.writeString(markdown, NarrativeVideoGrammarTemplate.markdown());
        Path projectFile = tempDir.resolve("video.docupodcast.json");
        DocuPodcastProject project = DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO);
        NarrationScriptDocument script = script();

        var parsed = useCase.parseNarrative(markdown);
        GrammarImportReport report = useCase.materializeNarrative(project, projectFile, markdown, parsed.plan(), script, parsed.report());

        Path sidecar = tempDir.resolve("project-semantics.json");
        String json = Files.readString(sidecar);
        assertTrue(report.semanticsMaterialized());
        assertTrue(json.contains("\"projectMode\": \"NARRATIVE_VIDEO\""));
        assertTrue(json.contains("\"grammarKind\": \"NARRATIVE_VIDEO\""));
        assertTrue(json.contains("\"grammarVersion\": \"narrative-video-v1\""));
        assertTrue(json.contains("\"fragmentId\": \"FRG-B001\""));
        assertTrue(json.contains("\"visualPrompt\""));
        assertTrue(json.contains("\"bridgePrompt\""));
    }

    @Test
    void unsavedProjectReportsPendingSemanticsWithoutFailingImport() throws Exception {
        ImportProjectGrammarMarkdownUseCase useCase =
                new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository());
        Path markdown = tempDir.resolve("video.md");
        Files.writeString(markdown, NarrativeVideoGrammarTemplate.markdown());
        var parsed = useCase.parseNarrative(markdown);

        GrammarImportReport report = useCase.materializeNarrative(
                DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO),
                null,
                markdown,
                parsed.plan(),
                script(),
                parsed.report());

        assertFalse(report.semanticsMaterialized());
        assertTrue(report.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SEMANTICS_NOT_MATERIALIZED")));
        assertFalse(Files.exists(tempDir.resolve("project-semantics.json")));
    }

    @Test
    void theatreImportReportsUnknownCharacterSceneAndToneButKeepsPartialImport() throws Exception {
        ImportProjectGrammarMarkdownUseCase useCase =
                new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository());
        Path markdown = tempDir.resolve("teatro.md");
        Files.writeString(markdown, """
                # Obra

                ## Acto: Acto 1

                ### Escena: Inicio

                CAPITAN BIGOTE: Linea teatral.
                > tono=NO_EXISTE | imagen=
                """);

        var parsed = useCase.parseTheatre(markdown);

        assertEquals(1, parsed.plan().interventions().size());
        assertTrue(parsed.report().diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("THEATRE_UNKNOWN_CHARACTER")));
        assertTrue(parsed.report().diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("THEATRE_UNKNOWN_TONE")));
    }

    @Test
    void theatreImportMaterializesCameraBackdropAndContextMetadata() throws Exception {
        ImportProjectGrammarMarkdownUseCase useCase =
                new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository());
        Path markdown = tempDir.resolve("teatro.md");
        Files.writeString(markdown, """
                # Obra

                - Personaje: CAPITAN BIGOTE | tono=SERIOUS

                ## Acto: Acto 1

                ### Escena: Inicio
                > fondo_escenario=fondos/hangar.png

                CAPITAN BIGOTE: Linea teatral.
                > plano=CERCA_CENTRO_NIVEL | aplicar_plano=false | fondo=fondos/hangar.png | contexto_ia=Camara fija para continuidad teatral.
                """);
        Path projectFile = tempDir.resolve("teatro.docupodcast.json");
        DocuPodcastProject project = DocuPodcastProject.createNew("Teatro", ProjectMode.THEATRE_PRODUCTION);

        var parsed = useCase.parseTheatre(markdown);
        GrammarImportReport report = useCase.materializeTheatre(project, projectFile, markdown, parsed.plan(), script(), parsed.report());

        String json = Files.readString(tempDir.resolve("project-semantics.json"));
        assertTrue(report.semanticsMaterialized());
        assertTrue(json.contains("\"grammarKind\": \"THEATRE_PRODUCTION\""));
        assertTrue(json.contains("\"cameraCue\": \"CERCA_CENTRO_NIVEL\""));
        assertTrue(json.contains("\"theatreApplyCamera\": \"false\""));
        assertTrue(json.contains("\"stageBackdrop\": \"fondos/hangar.png\""));
        assertTrue(json.contains("\"aiContextText\": \"Camara fija para continuidad teatral.\""));
        assertTrue(json.contains("\"cameraCueCount\": \"1\""));
        assertTrue(json.contains("\"backdropCueCount\": \"2\""));
        assertTrue(json.contains("\"contextOverrideCount\": \"1\""));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Lectura", "es", "Doc", List.of(
                new NarrationSegment("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno",
                        "Texto narrado uno", List.of("B001"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL", Map.of()),
                new NarrationSegment("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos",
                        "Texto narrado dos", List.of("B002"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL", Map.of())
        ));
    }
}
