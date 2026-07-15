package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarkdownGrammarTanda8SourceTest {
    @Test
    void grammarServicesAndSidecarAreWiredWithoutRootProjectSchemaChange() throws Exception {
        String applicationServices = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String project = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/DocuPodcastProject.java");
        String semanticsRepo = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/grammar/FileSystemProjectSemanticsRepository.java");

        assertTrue(applicationServices.contains("GrammarApplicationServices grammar"));
        assertTrue(factory.contains("new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository())"));
        assertTrue(semanticsRepo.contains("project-semantics.json"));
        assertFalse(project.contains("ProjectSemanticsDocument"));
    }

    @Test
    void shellDelegatesMarkdownGrammarFlowToCoordinatorAndKeepsCommandsStable() throws Exception {
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/GrammarWorkflowCoordinator.java");

        assertTrue(ids.contains("IMPORT_NARRATIVE_VIDEO_GRAMMAR"));
        assertTrue(ids.contains("EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE"));
        assertTrue(ids.contains("IMPORT_THEATRE_GRAMMAR"));
        assertTrue(ids.contains("EXPORT_THEATRE_GRAMMAR_TEMPLATE"));
        assertTrue(shell.contains("GrammarWorkflowCoordinator"));
        assertTrue(shell.contains("grammarWorkflow.importNarrativeVideoGrammar"));
        assertTrue(shell.contains("grammarWorkflow.importTheatreGrammar"));
        assertTrue(coordinator.contains("materializeNarrative"));
        assertTrue(coordinator.contains("materializeTheatre"));
        assertFalse(shell.contains("NarrativeVideoGrammarMarkdownParser.parse(sourceFile)"));
        assertFalse(shell.contains("TheatreGrammarMarkdownParser.parse(sourceFile)"));
    }

    @Test
    void theatreParserLogicLivesInApplicationWithPresentationFacadeOnly() throws Exception {
        String applicationParser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarMarkdownParser.java");
        String presentationParser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarMarkdownParser.java");

        assertTrue(applicationParser.contains("ACT_HEADING"));
        assertTrue(applicationParser.contains("SCENE_HEADING"));
        assertTrue(applicationParser.contains("MARKDOWN_LINK"));
        assertTrue(presentationParser.contains("Compatibility facade"));
        assertFalse(presentationParser.contains("private static final Pattern ACT_HEADING"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
