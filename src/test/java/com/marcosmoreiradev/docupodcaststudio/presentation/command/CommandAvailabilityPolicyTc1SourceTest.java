package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC1 guardrail: command availability must have one shared policy for all major surfaces. */
final class CommandAvailabilityPolicyTc1SourceTest {
    @Test
    void commandAvailabilityPolicyIsSingleSourceForMenuRibbonAndLegacyCapabilities() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String legacyPolicy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceCapabilityPolicy.java"));

        assertTrue(policy.contains("public final class CommandAvailabilityPolicy"));
        assertTrue(policy.contains("disabledBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel)"));
        assertTrue(policy.contains("unavailableReason(AppCommandId commandId, DocuPodcastShellViewModel viewModel)"));
        assertTrue(policy.contains("unavailableReasonBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel)"));
        assertTrue(shell.contains("private final CommandAvailabilityPolicy commandAvailabilityPolicy"));
        assertTrue(shell.contains("item.disableProperty().bind(commandAvailabilityPolicy.disabledBinding(commandId, viewModel))"));
        assertTrue(ribbon.contains("private final CommandAvailabilityPolicy commandAvailabilityPolicy"));
        assertTrue(ribbon.contains("button.disableProperty().bind(commandAvailabilityPolicy.disabledBinding(command.commandId(), viewModel))"));
        assertTrue(ribbon.contains("commandAvailabilityPolicy.unavailableReasonBinding(command.commandId(), viewModel)"));
        assertTrue(legacyPolicy.contains("new CommandAvailabilityPolicy()"));
        assertTrue(legacyPolicy.contains("commandMapper.commandFor(capability)"));
        assertTrue(legacyPolicy.contains("commandAvailabilityPolicy.disabledBinding"));
    }

    @Test
    void menuAndRibbonDoNotKeepLocalAvailabilitySwitches() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String menuBlock = shell.substring(shell.indexOf("private MenuBar buildMenuBar()"), shell.indexOf("public void handleOpenProjectFolder()"));

        assertFalse(ribbon.contains("private BooleanBinding disabledBinding(AppCommandId commandId)"));
        assertFalse(ribbon.contains("switch (commandId)"));
        assertFalse(menuBlock.contains("viewModel.projectOpenProperty().not()"));
        assertFalse(menuBlock.contains("viewModel.currentDocumentProperty().isNull()"));
        assertFalse(menuBlock.contains("viewModel.saveableProjectOpenProperty().not()"));
    }

    @Test
    void policyCoversCoreCommandStatesWithHumanReasons() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));

        assertTrue(policy.contains("SAVE_PROJECT"));
        assertTrue(policy.contains("OPEN_PROJECT_FOLDER"));
        assertTrue(policy.contains("LISTEN_DOCUMENT -> viewModel.currentDocumentProperty().isNull()"));
        assertTrue(policy.contains("PLAY_SELECTION"));
        assertTrue(policy.contains("IMPORT_IMAGE_FOR_SELECTION"));
        assertTrue(policy.contains("GENERATE_AUDIO"));
        assertTrue(policy.contains("CANCEL_AUDIO_JOB"));
        assertTrue(policy.contains("Abre o crea un proyecto primero."));
        assertTrue(policy.contains("Abre una fuente documental primero."));
        assertTrue(policy.contains("Selecciona una oración o fragmento del documento."));
        assertFalse(policy.contains("Whisper"));
        assertFalse(policy.contains("Coqui"));
        assertFalse(policy.contains("Piper"));
    }
}
