package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Concatenates PCM WAV files with identical audio format into one deliverable WAV. */
final class PcmWavConcatenator {
    ConcatenationResult concatenate(List<Path> clips, Path target) throws IOException {
        return concatenate(clips, List.of(), target);
    }

    ConcatenationResult concatenate(List<Path> clips, List<Long> silenceMillisAfterClip, Path target) throws IOException {
        Objects.requireNonNull(target, "target");
        if (clips == null || clips.isEmpty()) {
            throw new IOException("No hay segmentos WAV para concatenar.");
        }
        ArrayList<WavClip> inspected = new ArrayList<>();
        for (Path clip : clips) {
            inspected.add(inspect(clip));
        }
        WavFormat format = inspected.getFirst().format();
        long totalDataBytes = 0L;
        double totalDurationSeconds = 0.0;
        ArrayList<Long> silenceBytes = new ArrayList<>();
        for (int i = 0; i < inspected.size(); i++) {
            WavClip clip = inspected.get(i);
            if (!format.equals(clip.format())) {
                throw new IOException("Los WAV de segmentos no comparten formato PCM: "
                        + inspected.getFirst().path().getFileName() + " vs " + clip.path().getFileName());
            }
            long silence = silenceDataBytes(format, silenceMillisAfterClip == null || i >= silenceMillisAfterClip.size()
                    ? 0L
                    : silenceMillisAfterClip.get(i));
            silenceBytes.add(silence);
            totalDataBytes += clip.dataSize();
            totalDataBytes += silence;
            totalDurationSeconds += clip.durationSeconds();
            totalDurationSeconds += silence / Math.max(1.0, format.byteRate());
        }
        if (totalDataBytes > Integer.MAX_VALUE - 44L) {
            throw new IOException("El audio final supera el tamaño WAV soportado por esta exportación.");
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try (OutputStream out = Files.newOutputStream(target)) {
            writeHeader(out, format, totalDataBytes);
            byte[] buffer = new byte[8192];
            for (int i = 0; i < inspected.size(); i++) {
                WavClip clip = inspected.get(i);
                copyDataChunk(clip, out, buffer);
                writeSilence(out, silenceBytes.get(i), buffer);
            }
        }
        return new ConcatenationResult(inspected.size(), totalDurationSeconds, Files.size(target));
    }

    private static WavClip inspect(Path path) throws IOException {
        Path normalized = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("No existe segmento WAV regular: " + path);
        }
        try (RandomAccessFile wav = new RandomAccessFile(normalized.toFile(), "r")) {
            String riff = readAscii(wav, 4);
            long riffSize = readUInt32(wav);
            String wave = readAscii(wav, 4);
            if (!"RIFF".equals(riff) || !"WAVE".equals(wave) || riffSize < 36L) {
                throw new IOException("Archivo WAV inválido: " + path);
            }
            WavFormat format = null;
            long dataOffset = -1L;
            long dataSize = -1L;
            while (wav.getFilePointer() + 8 <= wav.length()) {
                String chunkId = readAscii(wav, 4);
                long chunkSize = readUInt32(wav);
                long chunkStart = wav.getFilePointer();
                if ("fmt ".equals(chunkId)) {
                    if (chunkSize < 16L) {
                        throw new IOException("Chunk fmt incompleto en WAV: " + path);
                    }
                    int audioFormat = readUInt16(wav);
                    int channels = readUInt16(wav);
                    long sampleRate = readUInt32(wav);
                    long byteRate = readUInt32(wav);
                    int blockAlign = readUInt16(wav);
                    int bitsPerSample = readUInt16(wav);
                    if (audioFormat != 1) {
                        throw new IOException("Solo se admite WAV PCM lineal. Formato encontrado: " + audioFormat);
                    }
                    format = new WavFormat(audioFormat, channels, sampleRate, byteRate, blockAlign, bitsPerSample);
                } else if ("data".equals(chunkId)) {
                    dataOffset = chunkStart;
                    dataSize = chunkSize;
                }
                long next = chunkStart + chunkSize + (chunkSize % 2L);
                if (next < chunkStart || next > wav.length() + 1L) {
                    throw new IOException("Chunk WAV inválido en " + path + ": " + chunkId);
                }
                wav.seek(Math.min(next, wav.length()));
                if (format != null && dataOffset >= 0L) {
                    break;
                }
            }
            if (format == null || dataOffset < 0L || dataSize <= 0L) {
                throw new IOException("WAV sin fmt/data válido: " + path);
            }
            double durationSeconds = dataSize / Math.max(1.0, format.byteRate());
            return new WavClip(normalized, format, dataOffset, dataSize, durationSeconds);
        }
    }

    private static void copyDataChunk(WavClip clip, OutputStream out, byte[] buffer) throws IOException {
        try (RandomAccessFile wav = new RandomAccessFile(clip.path().toFile(), "r")) {
            wav.seek(clip.dataOffset());
            long remaining = clip.dataSize();
            while (remaining > 0L) {
                int read = wav.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    throw new IOException("Fin inesperado leyendo datos WAV: " + clip.path());
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    private static void writeSilence(OutputStream out, long bytes, byte[] buffer) throws IOException {
        long remaining = Math.max(0L, bytes);
        java.util.Arrays.fill(buffer, (byte) 0);
        while (remaining > 0L) {
            int chunk = (int) Math.min(buffer.length, remaining);
            out.write(buffer, 0, chunk);
            remaining -= chunk;
        }
    }

    private static long silenceDataBytes(WavFormat format, long millis) {
        if (format == null || millis <= 0L) {
            return 0L;
        }
        long samples = Math.round(format.sampleRate() * (millis / 1000.0));
        return samples * Math.max(1, format.blockAlign());
    }

    private static void writeHeader(OutputStream out, WavFormat format, long dataSize) throws IOException {
        writeAscii(out, "RIFF");
        writeIntLE(out, 36L + dataSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLE(out, 16L);
        writeShortLE(out, format.audioFormat());
        writeShortLE(out, format.channels());
        writeIntLE(out, format.sampleRate());
        writeIntLE(out, format.byteRate());
        writeShortLE(out, format.blockAlign());
        writeShortLE(out, format.bitsPerSample());
        writeAscii(out, "data");
        writeIntLE(out, dataSize);
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

    private static void writeAscii(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeShortLE(OutputStream out, int value) throws IOException {
        out.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array());
    }

    private static void writeIntLE(OutputStream out, long value) throws IOException {
        out.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array());
    }

    record ConcatenationResult(int segmentCount, double durationSeconds, long sizeBytes) {
    }

    private record WavClip(Path path, WavFormat format, long dataOffset, long dataSize, double durationSeconds) {
    }

    private record WavFormat(int audioFormat, int channels, long sampleRate, long byteRate, int blockAlign, int bitsPerSample) {
    }
}
