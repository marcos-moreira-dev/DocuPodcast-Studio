package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tanda 3: project/source lifecycle transitions are explicit before later ribbon work. */
final class ProjectLifecycleTanda3SourceTest {
    @Test
    void newProjectFlowMakesInitialSourceChoiceExplicit() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ProjectInitialSourceDialog.java");

        assertTrue(shell.contains("ProjectInitialSourceDialog"));
        assertTrue(shell.contains("createProjectFromSetup(ProjectNameDialog.ProjectSetup setup)"));
        assertTrue(shell.contains("Decision.CREATE_WITHOUT_SOURCE"));
        assertTrue(shell.contains("chooseSourceDocument(\"Elegir fuente inicial del proyecto\")"));
        assertTrue(shell.contains("runSourceDocumentImport(sourceFile,"));
        assertTrue(shell.contains("() -> viewModel.createNewProject(setup.title(), setup.mode())"));
        assertTrue(dialog.contains("SELECT_SOURCE"));
        assertTrue(dialog.contains("CREATE_WITHOUT_SOURCE"));
        assertTrue(dialog.contains("CANCEL"));
    }

    @Test
    void openingAnotherSourceRequiresReplacementAndDirtyStateDecision() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ProjectSourceReplacementDialog.java");

        assertTrue(shell.contains("prepareForSourceDocumentImport()"));
        assertTrue(shell.contains("viewModel.currentSourceDocumentPath()"));
        assertTrue(shell.contains("projectSourceReplacementDialog.confirm(owner(), currentSource.get())"));
        assertTrue(shell.contains("return confirmSourceReplacementDirtyState()"));
        assertTrue(shell.contains("viewModel.openProject(projectFile.get())"));
        int theatreStart = shell.indexOf("public void handleImportTheatreGrammar()");
        int narrativeStart = shell.indexOf("public void handleImportNarrativeVideoGrammar()");
        int theatreGuard = shell.indexOf("if (!prepareForSourceDocumentImport())", theatreStart);
        int narrativeGuard = shell.indexOf("if (!prepareForSourceDocumentImport())", narrativeStart);
        assertTrue(theatreStart > 0);
        assertTrue(narrativeStart > 0);
        assertTrue(theatreGuard > theatreStart);
        assertTrue(narrativeGuard > narrativeStart);
        assertTrue(theatreGuard < shell.indexOf("grammarWorkflow.importTheatreGrammar", theatreStart));
        assertTrue(narrativeGuard < shell.indexOf("grammarWorkflow.importNarrativeVideoGrammar", narrativeStart));
        assertTrue(dialog.contains("Reemplazar fuente"));
        assertTrue(dialog.contains("lectura preparada, storyboard y audio derivado"));
    }

    @Test
    void sourceIntakePreservesExplicitTitleAndClearsDerivedSessionArtifacts() throws Exception {
        String intake = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentIntakeCoordinator.java");
        String session = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/ProjectSession.java");

        assertFalse(intake.contains(".withTitle(classified.title())"));
        assertTrue(intake.contains("session.setImportedDocument(classified)"));
        assertTrue(intake.contains("session.clearNarrationScript()"));
        assertTrue(intake.contains("session.clearStoryboard()"));
        assertTrue(session.contains("public void clearNarrationScript()"));
        assertTrue(session.contains("public void clearStoryboard()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
