package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-1: Voces uses human list states and avoids optional/technical copy in the normal UI. */
final class VoiceUx4R1HumanListSourceTest {
    @Test
    void voiceWorkspaceUsesHumanVoiceListItemInsteadOfManualTableLikeCells() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String item = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceListItemView.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfilePresentationPolicy.java");
        String source = item + policy;
        assertTrue(view.contains("new VoiceListItemView(item, libraryProperty.get(), useVoice)"));
        assertTrue(view.contains("ActionButtonFactory.primary(\"Usar voz\""));
        assertTrue(view.contains("Voces creadas"));
        assertTrue(source.contains("Lista"));
        assertTrue(source.contains("Incompleta"));
        assertTrue(source.contains("Voz simple"));
        assertTrue(source.contains("Voz avanzada"));
        assertTrue(item.contains("registeredToneTags"));
        assertFalse(source.contains("Falta neutral"));
    }

    @Test
    void toneCopyDoesNotCallNonNeutralTonesOptionalInTheVisibleWorkspace() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        assertTrue(view.contains("Neutral necesaria"));
        assertTrue(view.contains("VoiceToneLabelPolicy.comboLabel(prompt.tone())"));
        assertFalse(view.contains("Opcional ·"));
        assertFalse(view.contains("tonos opcionales"));
    }

    @Test
    void voiceListCssKeepsTheListSobriaWithoutExcelTableLook() throws IOException {
        String css = read("src/main/resources/css/voice-library.css");
        assertTrue(css.contains(".voice-list-item"));
        assertTrue(css.contains(".voice-list-status-ready"));
        assertTrue(css.contains(".voice-list-status-pending"));
        assertTrue(css.contains(".voice-browser-list .list-cell"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
