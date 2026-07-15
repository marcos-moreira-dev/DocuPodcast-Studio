package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrails that keep product dialogs out of the shell monolith. */
final class ProductDialogsSourceTest {
    @Test
    void shellDelegatesProductDialogsAndDoesNotCreateAlertsDirectly() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(shell.contains("ProjectNameDialog"));
        assertTrue(shell.contains("ActiveAudioJobDialog"));
        assertTrue(shell.contains("UnsavedChangesDialog"));
        assertTrue(shell.contains("ExportAiResourcesResultDialog"));
        assertTrue(shell.contains("alertPresenter.showFailure"));

        assertFalse(shell.contains("new Alert("), "El shell no debe crear Alert directamente.");
        assertFalse(shell.contains("new TextInputDialog"), "El dialogo de nombre de proyecto debe estar encapsulado.");
        assertFalse(shell.contains("new ButtonType"), "La decision de cambios sin guardar debe estar encapsulada.");
    }

    @Test
    void productDialogClassesExistForCurrentShellFlows() throws Exception {
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ProjectNameDialog.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ActiveAudioJobDialog.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/UnsavedChangesDialog.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ExportAiResourcesResultDialog.java")));
    }
}
