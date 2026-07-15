package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes tiny valid PCM WAV files so the UI can exercise the audio pipeline without TTS. */
final class MockWavWriter {
    private static final int SAMPLE_RATE = 8_000;
    private static final short CHANNELS = 1;
    private static final short BITS_PER_SAMPLE = 16;

    void writeSilence(Path target, double durationSeconds) throws IOException {
        Files.createDirectories(target.getParent());
        double safeDuration = Math.max(0.15, Math.min(3.0, durationSeconds));
        int samples = Math.max(1, (int) Math.round(SAMPLE_RATE * safeDuration));
        int dataSize = samples * CHANNELS * (BITS_PER_SAMPLE / 8);
        int byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8);
        short blockAlign = (short) (CHANNELS * (BITS_PER_SAMPLE / 8));

        try (OutputStream out = Files.newOutputStream(target)) {
            writeAscii(out, "RIFF");
            writeInt(out, 36 + dataSize);
            writeAscii(out, "WAVE");
            writeAscii(out, "fmt ");
            writeInt(out, 16);
            writeShort(out, (short) 1); // PCM
            writeShort(out, CHANNELS);
            writeInt(out, SAMPLE_RATE);
            writeInt(out, byteRate);
            writeShort(out, blockAlign);
            writeShort(out, BITS_PER_SAMPLE);
            writeAscii(out, "data");
            writeInt(out, dataSize);
            byte[] silence = new byte[dataSize];
            out.write(silence);
        }
    }

    private static void writeAscii(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private static void writeShort(OutputStream out, short value) throws IOException {
        out.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }
}
