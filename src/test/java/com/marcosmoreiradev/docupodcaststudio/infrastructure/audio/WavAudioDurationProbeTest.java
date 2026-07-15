package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WavAudioDurationProbeTest {
    @Test
    void readsDurationFromFmtAndDataChunks() throws Exception {
        Path wav = Files.createTempFile("duration-probe", ".wav");
        writeSilentPcmWav(wav, 16_000, 1, 16, 32_000);
        double duration = new WavAudioDurationProbe().durationSeconds(wav);
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
