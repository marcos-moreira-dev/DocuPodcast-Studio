package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioDurationProbe;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalDouble;

/** Reads the real duration of a WAV file from its fmt/data chunks. */
public final class WavAudioDurationProbe implements AudioDurationProbe {
    @Override
    public double durationSeconds(Path wavFile) throws IOException {
        Path target = Objects.requireNonNull(wavFile, "wavFile").toAbsolutePath().normalize();
        if (!Files.isRegularFile(target)) {
            throw new IOException("No existe WAV regular: " + wavFile);
        }
        try (RandomAccessFile wav = new RandomAccessFile(target.toFile(), "r")) {
            String riff = readAscii(wav, 4);
            long riffSize = readUInt32(wav);
            String wave = readAscii(wav, 4);
            if (!"RIFF".equals(riff) || !"WAVE".equals(wave) || riffSize < 36L) {
                throw new IOException("Archivo WAV inválido: " + wavFile);
            }
            long byteRate = -1L;
            long dataSize = -1L;
            while (wav.getFilePointer() + 8 <= wav.length()) {
                String chunkId = readAscii(wav, 4);
                long chunkSize = readUInt32(wav);
                long chunkStart = wav.getFilePointer();
                if ("fmt ".equals(chunkId)) {
                    if (chunkSize < 16L) {
                        throw new IOException("Chunk fmt incompleto en WAV: " + wavFile);
                    }
                    readUInt16(wav); // audioFormat
                    readUInt16(wav); // channels
                    readUInt32(wav); // sampleRate
                    byteRate = readUInt32(wav);
                    readUInt16(wav); // blockAlign
                    readUInt16(wav); // bitsPerSample
                } else if ("data".equals(chunkId)) {
                    dataSize = chunkSize;
                }
                long next = chunkStart + chunkSize + (chunkSize % 2L);
                if (next < chunkStart || next > wav.length() + 1L) {
                    throw new IOException("Chunk WAV inválido en " + wavFile + ": " + chunkId);
                }
                wav.seek(Math.min(next, wav.length()));
                if (byteRate > 0L && dataSize > 0L) {
                    break;
                }
            }
            if (byteRate <= 0L || dataSize <= 0L) {
                throw new IOException("WAV sin duración medible: " + wavFile);
            }
            return Math.max(0.001, dataSize / (double) byteRate);
        }
    }

    public OptionalDouble tryDurationSeconds(Path wavFile) {
        try {
            return OptionalDouble.of(durationSeconds(wavFile));
        } catch (IOException | RuntimeException ex) {
            return OptionalDouble.empty();
        }
    }

    private static String readAscii(RandomAccessFile file, int length) throws IOException {
        byte[] data = new byte[length];
        file.readFully(data);
        return new String(data, StandardCharsets.US_ASCII);
    }

    private static int readUInt16(RandomAccessFile file) throws IOException {
        byte[] data = new byte[Short.BYTES];
        file.readFully(data);
        return Short.toUnsignedInt(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    private static long readUInt32(RandomAccessFile file) throws IOException {
        byte[] data = new byte[Integer.BYTES];
        file.readFully(data);
        return Integer.toUnsignedLong(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }
}
