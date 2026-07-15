package com.marcosmoreiradev.docupodcaststudio.infrastructure.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.MaterializedVoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryWorkspaceFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void materializesVoiceLibraryInsideVoicesDirectory() throws Exception {
        Path projectFile = tempDir.resolve("obra.docupodcast.json");
        VoiceLibraryWorkspaceFileRepository repository = new VoiceLibraryWorkspaceFileRepository();

        MaterializedVoiceLibrary result = repository.materialize(VoiceLibrary.defaults(), projectFile);

        assertEquals("voices/voice-library.json", result.relativePath());
        Path target = tempDir.resolve("voices/voice-library.json");
        assertTrue(Files.exists(target));
        String json = Files.readString(target);
        assertTrue(json.contains("VOC-NARRATOR"));
        assertTrue(json.contains("CHR-NARRATOR"));
        assertTrue(json.contains("STY-NEUTRAL"));
        assertTrue(json.contains("referenceSampleSets"));
    }

    @Test
    void materializesReferenceSamplesByTone() throws Exception {
        Path projectFile = tempDir.resolve("obra.docupodcast.json");
        VoiceLibrary library = VoiceLibrary.defaults()
                .withVoice(VoiceProfile.ownVoicePlaceholder())
                .withReferenceSample(new VoiceReferenceSample(
                "VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-HAPPY",
                "VOC-OWN-PLACEHOLDER",
                VoiceReferenceTone.HAPPY,
                "voices/samples/happy.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                0,
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        ));

        new VoiceLibraryWorkspaceFileRepository().materialize(library, projectFile);

        String json = Files.readString(tempDir.resolve("voices/voice-library.json"));
        assertTrue(json.contains("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-HAPPY"));
        assertTrue(json.contains("\"tone\": \"HAPPY\""));
        assertTrue(json.contains("voices/samples/happy.wav"));
    }
}
