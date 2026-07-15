package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandAuditRc1SourceTest {
    @Test
    void visibleCommandsAreAuditedAgainstRegisteredHandlers() throws Exception {
        String inspector = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAuditInspector.java"));
        String dispatcher = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandDispatcher.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));

        assertTrue(inspector.contains("visibleByDefault"));
        assertTrue(inspector.contains("requireNoVisibleCommandGaps"));
        assertTrue(dispatcher.contains("registeredCommandIds()"));
        assertTrue(shell.contains("CommandAuditInspector.requireNoVisibleCommandGaps(commandRegistry, commandDispatcher)"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_STORYBOARD"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_AUDIO_JOBS"));
    }
}
