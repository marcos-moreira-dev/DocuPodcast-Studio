package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T99B guardrail: visible surfaces must invoke the command dispatcher instead of duplicating action logic. */
final class CommandDispatchCleanupT99BSourceTest {
    @Test
    void shellRegistersOneCommandDispatcherForVisibleActions() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String menuBlock = shell.substring(shell.indexOf("private MenuBar buildMenuBar()"), shell.indexOf("public void handleOpenProjectFolder()"));

        assertTrue(shell.contains("private final AppCommandDispatcher commandDispatcher"));
        assertTrue(shell.contains("registerCommandHandlers()"));
        assertTrue(shell.contains("public AppCommandDispatchResult dispatchCommand(AppCommandId commandId)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.LISTEN_DOCUMENT)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.EXPORT_PODCAST_WAV)"));
        assertTrue(menuBlock.contains("Soporte avanzado"));
        assertFalse(menuBlock.contains("setOnAction(event -> viewModel.runDocumentPrimaryAction")
                , "Menu items must not wire direct ViewModel handlers when a command exists.");
        assertFalse(menuBlock.contains("setOnAction(event -> handleImportWord")
                , "Menu items must route source document opening through AppCommandId.");
    }

    @Test
    void toolbarInvokesCommandIdsInsteadOfDuplicatingHandlers() throws Exception {
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));

        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.SHOW_WELCOME)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.LISTEN_DOCUMENT)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(action.commandId())"));
        assertFalse(toolbar.contains("private void wireAction("));
        assertFalse(toolbar.contains("viewModel.runDocumentPrimaryAction"));
        assertFalse(toolbar.contains("shellView.handleExportProjectBundle"));
    }

    @Test
    void welcomeSurfaceUsesCommandsForStartupActions() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String workspacesBlock = shell.substring(shell.indexOf("private void initialiseWorkspaces()"), shell.indexOf("private Node buildTop()"));

        assertTrue(workspacesBlock.contains("dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
        assertTrue(workspacesBlock.contains("dispatchCommand(AppCommandId.OPEN_PROJECT)"));
        assertTrue(workspacesBlock.contains("dispatchCommand(AppCommandId.NEW_PROJECT)"));
        assertFalse(workspacesBlock.contains("this::handleImportWord"));
    }
}
