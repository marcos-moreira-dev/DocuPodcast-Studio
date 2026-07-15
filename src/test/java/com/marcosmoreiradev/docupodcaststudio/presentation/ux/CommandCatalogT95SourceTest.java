package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrails for T95 command catalog before GUI rewrite. */
final class CommandCatalogT95SourceTest {
    @Test
    void commandCatalogExistsBeforeRibbonRewrite() throws Exception {
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String dispatcher = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandDispatcher.java"));
        String mapper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/WorkspaceCapabilityCommandMapper.java"));
        String action = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarAction.java"));

        assertTrue(registry.contains("AppCommandId.LISTEN_DOCUMENT"));
        assertTrue(registry.contains("AppCommandSurface.WORKSPACE_PLAYBAR"));
        assertTrue(registry.contains("AppCommandId.IMPORT_AUDIO_FOR_SELECTION"));
        assertTrue(registry.contains("AppCommandSurface.LEFT_SIDEBAR"));
        assertTrue(dispatcher.contains("dispatch(AppCommandId id)"));
        assertTrue(mapper.contains("case LISTEN_DOCUMENT -> AppCommandId.LISTEN_DOCUMENT"));
        assertTrue(action.contains("AppCommandId commandId"));
    }

    @Test
    void commandCatalogDoesNotReintroduceWhisperOrAudioToTextAsProductCommand() throws Exception {
        String ids = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));

        assertFalse(ids.contains("WHISPER"));
        assertFalse(ids.contains("SPEECH_TO_TEXT"));
        assertFalse(ids.contains("TRANSCRIBE"));
        assertFalse(registry.contains("Audio a texto"));
    }
}
