package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackManifestTest {
    @Test
    void connectsSegmentAudioAndImageInOrderedCues() {
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-001", "JOB-001", List.of(
                new PlaybackCue("SEG-001", 0.0, 2.5, "AUD-SEG-001", "jobs/JOB-001/audio/SEG-001.wav", "IMG-001", "Intro"),
                new PlaybackCue("SEG-002", 2.5, 5.0, "AUD-SEG-002", "jobs/JOB-001/audio/SEG-002.wav", "", "Cierre")
        ), "jobs/JOB-001/final/podcast.wav", Instant.EPOCH);

        assertEquals(2, manifest.cueCount());
        assertEquals(5.0, manifest.totalDurationSeconds(), 0.001);
        assertTrue(manifest.cueAt(3.0).orElseThrow().segmentId().equals("SEG-002"));
        assertEquals("IMG-001", manifest.cueForSegment("SEG-001").orElseThrow().imageAssetId());
    }

    @Test
    void acceptsMultipleUnitCuesForSameSegmentWhenUnitIdsAreUnique() {
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-001", "JOB-001", List.of(
                new PlaybackCue("SEG-001", "SEG-001-U001", 0.0, 1.0, "AUD-1", "jobs/JOB/audio/1.wav", "", "Uno"),
                new PlaybackCue("SEG-001", "SEG-001-U002", 1.0, 2.0, "AUD-2", "jobs/JOB/audio/2.wav", "", "Dos")
        ), "", Instant.EPOCH);

        assertEquals(2, manifest.cuesForSegment("SEG-001").size());
        assertEquals("SEG-001-U002", manifest.cueForUnit("SEG-001-U002").orElseThrow().unitId());
        assertEquals("SEG-001-U002", manifest.nextCueAfterUnit("SEG-001-U001").orElseThrow().unitId());
    }

    @Test
    void createsSegmentOnlyManifestWithAllRenderUnitCues() {
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-001", "JOB-001", List.of(
                new PlaybackCue("SEG-001", "SEG-001-U001", 0.0, 1.0, "AUD-1", "jobs/JOB/audio/1.wav", "", "Uno"),
                new PlaybackCue("SEG-001", "SEG-001-U002", 1.0, 2.0, "AUD-2", "jobs/JOB/audio/2.wav", "", "Dos"),
                new PlaybackCue("SEG-002", "SEG-002-U001", 2.0, 3.0, "AUD-3", "jobs/JOB/audio/3.wav", "", "Tres")
        ), "", Instant.EPOCH);

        PlaybackManifest only = manifest.onlySegment("SEG-001");

        assertEquals(2, only.cueCount());
        assertEquals("SEG-001-U001", only.firstCue().orElseThrow().unitId());
        assertTrue(only.cueForSegment("SEG-002").isEmpty());
    }

    @Test
    void rejectsDuplicatedSegmentCue() {
        assertThrows(IllegalArgumentException.class, () -> new PlaybackManifest("PLAYBACK-JOB-001", "JOB-001", List.of(
                new PlaybackCue("SEG-001", 0.0, 1.0, "AUD-1", "jobs/JOB/audio/1.wav", "", "Uno"),
                new PlaybackCue("SEG-001", 1.0, 2.0, "AUD-2", "jobs/JOB/audio/2.wav", "", "Dos")
        ), "", Instant.EPOCH));
    }
}
