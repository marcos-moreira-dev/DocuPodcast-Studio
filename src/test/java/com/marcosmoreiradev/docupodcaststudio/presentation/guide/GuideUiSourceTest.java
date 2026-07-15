package com.marcosmoreiradev.docupodcaststudio.presentation.guide;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuideUiSourceTest {
    @Test
    void shellExposesGuideAsHumanHelpFromTheHelpMenuThroughCommandCatalog() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/guide/GuideDialog.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));

        assertTrue(shell.contains("Menu ayuda = new Menu(\"Ayuda\")"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_GUIDE)"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_ABOUT)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_GUIDE, this::handleOpenGuide)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_ABOUT, () -> handleOpenGuideTopic(GuideTopicId.GLOSSARY))"));
        assertTrue(registry.contains("Guía de uso"));
        assertTrue(registry.contains("Acerca de DocuPodcast Studio"));
        assertTrue(dialog.contains("Contenido"));
        assertTrue(dialog.contains("Buscar"));
    }
}
