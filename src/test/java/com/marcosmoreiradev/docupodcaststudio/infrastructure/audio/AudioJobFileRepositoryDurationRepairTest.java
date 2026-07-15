package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioJobFileRepositoryDurationRepairTest {
    @Test
    void repairsPersistedEstimatedDurationFromExistingWav() throws Exception {
        Path project = Files.createTempDirectory("audio-job-duration-repair");
        Path job = project.resolve("jobs/JOB-1");
        Files.createDirectories(job.resolve("audio"));
        writeSilentPcmWav(job.resolve("audio/SEG-1.wav"), 16_000, 1, 16, 32_000);
        String now = Instant.now().toString();
        Files.writeString(job.resolve("job.json"), """
                {
                  "jobId": "JOB-1",
                  "documentName": "Doc",
                  "state": "COMPLETED",
                  "stage": "EXPORT_READY",
                  "completedSegments": 1,
                  "totalSegments": 1,
                  "failedSegments": 0,
                  "progress": 1.0,
                  "currentSegmentId": "",
                  "currentSegmentTitle": "",
                  "estimatedRemainingSeconds": 0,
                  "message": "ok",
                  "jobRelativeDirectory": "jobs/JOB-1",
                  "finalAudioPath": "",
                  "manifestPath": "jobs/JOB-1/audio-manifest.json",
                  "createdAt": "%s",
                  "updatedAt": "%s"
                }
                """.formatted(now, now), StandardCharsets.UTF_8);
        Files.writeString(job.resolve("segments-status.json"), """
                {
                  "segments": [
                    {
                      "segmentId": "SEG-1",
                      "title": "SEG-1",
                      "status": "COMPLETED",
                      "audioRelativePath": "jobs/JOB-1/audio/SEG-1.wav",
                      "durationSeconds": 0.25,
                      "attempts": 1,
                      "errorMessage": ""
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);
        double duration = new AudioJobFileRepository().list(project).getFirst().segments().getFirst().durationSeconds();
        assertEquals(1.0, duration, 0.01);
    }

    private static void writeSilentPcmWav(Path wav, int sampleRate, int channels, int bitsPerSample, int dataBytes) throws Exception {
        int blockAlign = channels * bitsPerSample / 8;
        int byteRate = sampleRate * blockAlign;
        try (OutputStream out = Files.newOutputStream(wav)) {
            writeAscii(out, "RIFF"); writeIntLE(out, 36L + dataBytes); writeAscii(out, "WAVE");
            writeAscii(out, "fmt "); writeIntLE(out, 16L); writeShortLE(out, 1); writeShortLE(out, channels);
            writeIntLE(out, sampleRate); writeIntLE(out, byteRate); writeShortLE(out, blockAlign); writeShortLE(out, bitsPerSample);
            writeAscii(out, "data"); writeIntLE(out, dataBytes); out.write(new byte[dataBytes]);
        }
    }

    private static void writeAscii(OutputStream out, String value) throws Exception { out.write(value.getBytes(StandardCharsets.US_ASCII)); }
    private static void writeShortLE(OutputStream out, int value) throws Exception { out.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array()); }
    private static void writeIntLE(OutputStream out, long value) throws Exception { out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array()); }
}
