package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Reads physical PCM WAV duration from its fmt/data chunks. */
public final class PcmWavDurationProbe implements AudioDurationProbe {
    @Override public double durationSeconds(Path wavFile) throws IOException {
        Path target = Objects.requireNonNull(wavFile, "wavFile").toAbsolutePath().normalize();
        if (!Files.isRegularFile(target)) throw new IOException("No existe WAV regular: " + wavFile);
        try (RandomAccessFile wav = new RandomAccessFile(target.toFile(), "r")) {
            String riff = ascii(wav, 4); long riffSize = uint32(wav); String wave = ascii(wav, 4);
            if (!"RIFF".equals(riff) || !"WAVE".equals(wave) || riffSize < 36L) {
                throw new IOException("Archivo WAV invalido: " + wavFile);
            }
            long byteRate = -1L, dataSize = -1L;
            while (wav.getFilePointer() + 8 <= wav.length()) {
                String id = ascii(wav, 4); long size = uint32(wav); long start = wav.getFilePointer();
                if ("fmt ".equals(id)) {
                    if (size < 16L) throw new IOException("Chunk fmt incompleto: " + wavFile);
                    uint16(wav); uint16(wav); uint32(wav); byteRate = uint32(wav); uint16(wav); uint16(wav);
                } else if ("data".equals(id)) dataSize = size;
                long next = start + size + size % 2L;
                if (next < start || next > wav.length() + 1L) throw new IOException("Chunk WAV invalido: " + id);
                wav.seek(Math.min(next, wav.length()));
                if (byteRate > 0L && dataSize > 0L) break;
            }
            if (byteRate <= 0L || dataSize <= 0L) throw new IOException("WAV sin duracion medible: " + wavFile);
            return Math.max(0.001, dataSize / (double) byteRate);
        }
    }
    private static String ascii(RandomAccessFile file, int length) throws IOException {
        byte[] data = new byte[length]; file.readFully(data); return new String(data, StandardCharsets.US_ASCII);
    }
    private static int uint16(RandomAccessFile file) throws IOException {
        byte[] data = new byte[2]; file.readFully(data);
        return Short.toUnsignedInt(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }
    private static long uint32(RandomAccessFile file) throws IOException {
        byte[] data = new byte[4]; file.readFully(data);
        return Integer.toUnsignedLong(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }
}
