package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapability;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceToolbarActionProviderTest {
    @Test
    void providerMapsOnlyProductWorkspacesToContextualCapabilityActions() {
        WorkspaceToolbarActionProvider provider = WorkspaceToolbarActionProvider.official();

        List<WorkspaceToolbarAction> documentActions = provider.actionsFor(WorkspaceKind.DOCUMENT_READER);
        assertTrue(documentActions.stream().anyMatch(action -> action.capability() == WorkspaceCapability.LISTEN_DOCUMENT));
        assertTrue(documentActions.stream().anyMatch(action -> action.label().equals("Escuchar documento")));
        assertTrue(documentActions.stream().anyMatch(action -> action.capability() == WorkspaceCapability.OPEN_VOICE_LIBRARY));

        List<WorkspaceToolbarAction> voiceActions = provider.actionsFor(WorkspaceKind.VOICE_LIBRARY);
        assertTrue(voiceActions.stream().anyMatch(action -> action.capability() == WorkspaceCapability.IMPORT_VOICE_SAMPLE));
        assertTrue(voiceActions.stream().noneMatch(action -> action.label().contains("Whisper")));
        assertTrue(voiceActions.stream().noneMatch(action -> action.label().equals("Audio a texto")));

        List<WorkspaceToolbarAction> legacyScriptActions = provider.actionsFor(WorkspaceKind.SCRIPT_EDITOR);
        assertTrue(legacyScriptActions.stream().anyMatch(action -> action.capability() == WorkspaceCapability.LISTEN_DOCUMENT));
        assertTrue(legacyScriptActions.stream().noneMatch(action -> action.label().contains("Markdown")));

        List<WorkspaceToolbarAction> legacyAudioActions = provider.actionsFor(WorkspaceKind.AUDIO_JOBS);
        assertTrue(legacyAudioActions.stream().anyMatch(action -> action.capability() == WorkspaceCapability.LISTEN_DOCUMENT));
        assertTrue(legacyAudioActions.stream().noneMatch(action -> action.capability() == WorkspaceCapability.CANCEL_AUDIO_JOB));
    }
}
