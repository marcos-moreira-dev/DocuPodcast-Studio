package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrails for visible actions that are not yet complete product capabilities. */
final class VisibleActionContractSourceTest {
    @Test
    void welcomeIsDesktopStartPageNotMarkdownNarrationShortcut() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/welcome.css"));

        assertTrue(welcome.contains("ActionButtonFactory.secondary(title, action)"),
                "La bienvenida debe ofrecer acciones reales mediante componentes compartidos.");
        assertTrue(welcome.contains("Abrir documento"));
        assertTrue(welcome.contains("Abre la fuente") || welcome.contains("Abre"));
        assertFalse(welcome.contains("welcome-product-landing"));
        assertFalse(welcome.contains("Importar narración Markdown"),
                "T81A elimina la narracion Markdown del frente normal; Markdown entra como documento fuente.");
        assertFalse(welcome.contains("docupodcast-script-v1"),
                "El landing no debe hablar de formato interno de guion narrable.");
        assertFalse(welcome.contains("pendiente de importador real"));
        assertFalse(css.contains(".welcome-action {"),
                "La bienvenida no debe conservar estilos de falso boton para labels no interactivos.");
        assertFalse(css.contains(".welcome-action:hover"),
                "La bienvenida no debe conservar hover de falso boton para labels no interactivos.");
        assertFalse(css.contains(".welcome-step:hover"),
                "Los pasos de bienvenida son guia, no botones interactivos.");
    }


    @Test
    void roadmapToolbarActionsDoNotExposeUnimplementedAudioToTextPromise() throws Exception {
        String provider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(shell.contains(".register(AppCommandId.ASSIGN_AI_VOICE_TO_SELECTION, viewModel::prepareAiVoiceForSelectedText)"));
        assertTrue(viewModel.contains("public void prepareAiVoiceForSelectedText()"));
        assertTrue(viewModel.contains("Preparado para asignar voz IA/TTS"));

        assertTrue(provider.contains("Grabar voz"));
        assertTrue(shell.contains(".register(AppCommandId.ASSIGN_HUMAN_RECORDING_TO_SELECTION, viewModel::prepareHumanVoiceForSelectedText)"));
        assertTrue(viewModel.contains("public void prepareHumanVoiceForSelectedText()"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(action.commandId())"));

        assertFalse(provider.contains("Audio a texto"));
        assertFalse(toolbar.contains("handleTranscribeAudioToText"));
        assertFalse(shell.contains("Transcribir audio a texto"));
        assertFalse(viewModel.contains("prepareSpeechToTextForSelectedText"));
    }

}
