package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AudioManifestTest {
    @Test
    void calculatesClipCountAndDuration() {
        AudioManifest manifest = new AudioManifest("JOB-001", List.of(
                new AudioClipReference("AUD-001", "SEG-001", "jobs/JOB-001/audio/SEG-001.wav", 1.5, 120),
                new AudioClipReference("AUD-002", "SEG-002", "jobs/JOB-001/audio/SEG-002.wav", 2.0, 200)
        ), "jobs/JOB-001/final/podcast.wav", null);

        assertEquals(2, manifest.clipCount());
        assertEquals(3.5, manifest.totalDurationSeconds(), 0.001);
    }

    @Test
    void rejectsDuplicateSegmentReferences() {
        assertThrows(IllegalArgumentException.class, () -> new AudioManifest("JOB-001", List.of(
                new AudioClipReference("AUD-001", "SEG-001", "jobs/JOB-001/audio/a.wav", 1.0, 10),
                new AudioClipReference("AUD-002", "SEG-001", "jobs/JOB-001/audio/b.wav", 1.0, 10)
        ), "jobs/JOB-001/final/podcast.wav", null));
    }
}
