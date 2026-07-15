package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualAudioSegmentJobUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void appliesManualRecordingInsideExistingJobWithoutDroppingOtherSegments() throws Exception {
        InMemoryRepository repository = new InMemoryRepository();
        ManualAudioSegmentJobUseCase useCase = new ManualAudioSegmentJobUseCase(repository, wav -> 2.5);
        AudioJobSnapshot existing = snapshot("JOB-001", List.of(
                AudioSegmentSnapshot.pending("SEG-001", "Uno"),
                AudioSegmentSnapshot.pending("SEG-002", "Dos").completed("jobs/JOB-001/audio/SEG-002.wav", 1.0)));
        Path source = tempDir.resolve("recording.wav");
        writeSilentPcmWav(source);

        AudioJobSnapshot updated = useCase.apply(tempDir, existing, "Doc", "SEG-001", "Uno",
                source, List.of(AudioSegmentSnapshot.pending("SEG-001", "Uno"), AudioSegmentSnapshot.pending("SEG-002", "Dos")));

        assertTrue(Files.isRegularFile(tempDir.resolve("jobs/JOB-001/audio/SEG-001-manual.wav")));
        assertTrue(Files.isRegularFile(tempDir.resolve("jobs/JOB-001/audio-manifest.json")));
        assertEquals(2, updated.completedSegments());
        assertEquals("jobs/JOB-001/audio/SEG-001-manual.wav", updated.segments().getFirst().audioRelativePath());
        assertEquals("jobs/JOB-001/audio/SEG-002.wav", updated.segments().get(1).audioRelativePath());
        assertEquals(updated, repository.saved);
    }

    @Test
    void deletingManualRecordingRestoresGeneratedWavWhenItExists() throws Exception {
        InMemoryRepository repository = new InMemoryRepository();
        ManualAudioSegmentJobUseCase useCase = new ManualAudioSegmentJobUseCase(repository, wav -> 1.75);
        Path manual = tempDir.resolve("jobs/JOB-001/audio/SEG-001-manual.wav");
        Path generated = tempDir.resolve("jobs/JOB-001/audio/SEG-001.wav");
        Files.createDirectories(manual.getParent());
        writeSilentPcmWav(manual);
        writeSilentPcmWav(generated);
        AudioJobSnapshot existing = snapshot("JOB-001", List.of(
                new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-001-manual.wav", 2.0, 1, "")));

        AudioJobSnapshot updated = useCase.delete(tempDir, existing, "SEG-001", "Uno",
                List.of(AudioSegmentSnapshot.pending("SEG-001", "Uno")));

        assertTrue(Files.notExists(manual));
        assertEquals("jobs/JOB-001/audio/SEG-001.wav", updated.segments().getFirst().audioRelativePath());
        assertEquals(1.75, updated.segments().getFirst().durationSeconds(), 0.001);
        assertEquals(updated, repository.saved);
    }

    private static AudioJobSnapshot snapshot(String jobId, List<AudioSegmentSnapshot> segments) {
        int completed = (int) segments.stream().filter(AudioSegmentSnapshot::completed).count();
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        return new AudioJobSnapshot(jobId, "Doc", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                completed, segments.size(), 0, segments.isEmpty() ? 0.0 : completed / (double) segments.size(),
                "", "", 0L, "", "jobs/" + jobId, "", "", segments, now, now);
    }

    private static void writeSilentPcmWav(Path wav) throws Exception {
        Files.createDirectories(wav.getParent());
        int sampleRate = 16_000;
        int dataBytes = 320;
        int byteRate = sampleRate * 2;
        try (var out = Files.newOutputStream(wav)) {
            writeAscii(out, "RIFF"); writeIntLE(out, 36L + dataBytes); writeAscii(out, "WAVE");
            writeAscii(out, "fmt "); writeIntLE(out, 16L); writeShortLE(out, 1); writeShortLE(out, 1);
            writeIntLE(out, sampleRate); writeIntLE(out, byteRate); writeShortLE(out, 2); writeShortLE(out, 16);
            writeAscii(out, "data"); writeIntLE(out, dataBytes); out.write(new byte[dataBytes]);
        }
    }

    private static void writeAscii(java.io.OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeShortLE(java.io.OutputStream out, int value) throws IOException {
        out.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array());
    }

    private static void writeIntLE(java.io.OutputStream out, long value) throws IOException {
        out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array());
    }

    private static final class InMemoryRepository implements AudioJobRepository {
        private AudioJobSnapshot saved;

        @Override
        public void save(Path projectDirectory, AudioJobSnapshot snapshot) {
            saved = snapshot;
        }

        @Override
        public Optional<AudioJobSnapshot> load(Path projectDirectory, String jobId) {
            return Optional.ofNullable(saved).filter(snapshot -> snapshot.jobId().equals(jobId));
        }

        @Override
        public List<AudioJobSnapshot> list(Path projectDirectory) {
            return saved == null ? List.of() : List.of(saved);
        }

        @Override
        public void deleteAll(Path projectDirectory) {
            saved = null;
        }
    }
}
