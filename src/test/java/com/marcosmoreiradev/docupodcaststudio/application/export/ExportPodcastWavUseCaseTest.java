package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportPodcastWavUseCaseTest {
    @TempDir
    Path temp;

    @Test
    void concatenatesCompletedSegmentWavsWhenNoFinalAudioExists() throws Exception {
        Path project = temp.resolve("project");
        Files.createDirectories(project.resolve("jobs/JOB-001/audio"));
        writeSilentWav(project.resolve("jobs/JOB-001/audio/SEG-001.wav"), 8000, 1, 16, 800);
        writeSilentWav(project.resolve("jobs/JOB-001/audio/SEG-002.wav"), 8000, 1, 16, 1600);
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Guion", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0, "OK",
                "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED,
                                "jobs/JOB-001/audio/SEG-001.wav", 0.10, 1, ""),
                        new AudioSegmentSnapshot("SEG-002", "Cierre", AudioSegmentStatus.COMPLETED,
                                "jobs/JOB-001/audio/SEG-002.wav", 0.20, 1, "")
                ), Instant.now(), Instant.now());

        PodcastFinalWavExportResult result = new ExportPodcastWavUseCase()
                .exportLatest(List.of(job), project, temp.resolve("podcast-final"));

        assertTrue(Files.exists(result.targetFile()));
        assertTrue(Files.exists(result.reportFile()));
        assertTrue(result.concatenatedFromSegments());
        assertEquals(2, result.segmentCount());
        assertEquals("podcast-final.wav", result.targetFile().getFileName().toString());
        assertTrue(Files.size(result.targetFile()) > 44);
        String report = Files.readString(result.reportFile());
        assertTrue(report.contains("WAV final concatenado desde segmentos"));
        assertTrue(report.contains("jobs/JOB-001/audio/SEG-001.wav"));
    }

    @Test
    void canExportPodcastWhenJobHasCompletedSegmentAudio() {
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-002", "Guion", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0, "OK",
                "jobs/JOB-002", "", "jobs/JOB-002/audio-manifest.json",
                List.of(new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-002/audio/SEG-001.wav", 0.10, 1, "")), Instant.now(), Instant.now());

        assertTrue(new ExportPodcastWavUseCase().canExport(List.of(job)));
    }


    @Test
    void exportsPlaybackManifestWithUserAudioClipCues() throws Exception {
        Path project = temp.resolve("project-with-clips");
        Files.createDirectories(project.resolve("media/audio"));
        writeSilentWav(project.resolve("media/audio/pajaros.wav"), 8000, 1, 16, 800);
        writeSilentWav(project.resolve("media/audio/campana.wav"), 8000, 1, 16, 800);
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-CLIPS", "JOB-CLIPS", List.of(
                new PlaybackCue("SEG-001", "SEG-001-U002", 0.0, 1.0,
                        "AUDIO-USER-001", "media/audio/pajaros.wav", "IMG-001", "Pájaros"),
                new PlaybackCue("SEG-002", "SEG-002-U001", 1.0, 2.0,
                        "AUDIO-USER-002", "media/audio/campana.wav", "", "Campana")
        ), "", Instant.EPOCH);

        PodcastFinalWavExportResult result = new ExportPodcastWavUseCase()
                .exportPlaybackManifest(manifest, project, temp.resolve("podcast-clips.wav"));

        assertTrue(Files.exists(result.targetFile()));
        assertEquals(2, result.segmentCount());
        assertTrue(result.sourceClipPaths().contains("media/audio/pajaros.wav"));
        String report = Files.readString(result.reportFile());
        assertTrue(report.contains("media/audio/campana.wav"));
    }

    @Test
    void exportsPlaybackManifestWithAcceleratedPunctuationPauses() throws Exception {
        Path project = temp.resolve("project-with-pauses");
        Files.createDirectories(project.resolve("jobs/JOB/audio"));
        writeSilentWav(project.resolve("jobs/JOB/audio/SEG-001-U001.wav"), 8000, 1, 16, 800);
        writeSilentWav(project.resolve("jobs/JOB/audio/SEG-002-U001.wav"), 8000, 1, 16, 800);
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB", "JOB", List.of(
                new PlaybackCue("SEG-001", "SEG-001-U001", 0.0, 0.1,
                        "AUD-1", "jobs/JOB/audio/SEG-001-U001.wav", "", "Uno", "Primer texto."),
                new PlaybackCue("SEG-002", "SEG-002-U001", 0.1, 0.2,
                        "AUD-2", "jobs/JOB/audio/SEG-002-U001.wav", "", "Dos", "Segundo texto.")
        ), "", Instant.EPOCH);

        PodcastFinalWavExportResult result = new ExportPodcastWavUseCase()
                .exportPlaybackManifest(manifest, project, temp.resolve("podcast-pauses.wav"), 1.75);

        assertEquals(2, result.segmentCount());
        assertEquals(0.70, result.durationSeconds(), 0.01);
        assertTrue(Files.size(result.targetFile()) > 44 + 3200);
    }

    private static void writeSilentWav(Path target, int sampleRate, int channels, int bitsPerSample, int samples) throws Exception {
        Files.createDirectories(target.getParent());
        int bytesPerSample = bitsPerSample / 8;
        int dataSize = samples * channels * bytesPerSample;
        int byteRate = sampleRate * channels * bytesPerSample;
        int blockAlign = channels * bytesPerSample;
        try (OutputStream out = Files.newOutputStream(target)) {
            writeAscii(out, "RIFF");
            writeInt(out, 36 + dataSize);
            writeAscii(out, "WAVE");
            writeAscii(out, "fmt ");
            writeInt(out, 16);
            writeShort(out, 1);
            writeShort(out, channels);
            writeInt(out, sampleRate);
            writeInt(out, byteRate);
            writeShort(out, blockAlign);
            writeShort(out, bitsPerSample);
            writeAscii(out, "data");
            writeInt(out, dataSize);
            out.write(new byte[dataSize]);
        }
    }

    private static void writeAscii(OutputStream out, String value) throws Exception {
        out.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeInt(OutputStream out, int value) throws Exception {
        out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private static void writeShort(OutputStream out, int value) throws Exception {
        out.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array());
    }
}
