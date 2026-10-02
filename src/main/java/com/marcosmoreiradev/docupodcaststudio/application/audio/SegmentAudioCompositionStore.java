package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSourceFingerprint;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Materializes the effective WAV consumed by playback/export for each source
 * narration segment. TTS chunks remain immutable inputs and are never
 * regenerated merely because the segment-level composition is absent.
 */
final class SegmentAudioCompositionStore {
    private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS =
            new ConcurrentHashMap<>();

    Result materialize(List<AudioGenerationUnit> current,
                       ReusableAudioCoverage.Report coverage,
                       Path projectDirectory) {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path cache = root.resolve("jobs/segment-audio-cache").normalize();
        ReentrantLock lock = LOCKS.computeIfAbsent(cache, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return materializeLocked(current, coverage, root, cache);
        } finally {
            lock.unlock();
        }
    }

    /** Verifies persisted segment compositions without creating or modifying files. */
    Inspection inspect(List<AudioGenerationUnit> current,
                       ReusableAudioCoverage.Report coverage,
                       Path projectDirectory) {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path cache = root.resolve("jobs/segment-audio-cache").normalize();
        LinkedHashMap<String, List<AudioGenerationUnit>> bySegment = new LinkedHashMap<>();
        for (AudioGenerationUnit unit : current == null
                ? List.<AudioGenerationUnit>of() : current) {
            String segmentId = unit.sourceSegmentId().isBlank()
                    ? unit.id() : unit.sourceSegmentId();
            bySegment.computeIfAbsent(segmentId, ignored -> new ArrayList<>()).add(unit);
        }
        Map<String, ReusableAudioCoverage.Entry> entries = new LinkedHashMap<>();
        coverage.entries().forEach(entry -> entries.put(entry.unit().id(), entry));
        ArrayList<String> missing = new ArrayList<>();
        for (Map.Entry<String, List<AudioGenerationUnit>> group : bySegment.entrySet()) {
            String segmentId = group.getKey();
            List<AudioGenerationUnit> units = List.copyOf(group.getValue());
            List<ReusableAudioCoverage.Entry> chunks = units.stream()
                    .map(unit -> entries.get(unit.id())).toList();
            if (chunks.stream().anyMatch(entry -> entry == null || !entry.ready())) {
                missing.add(segmentId);
                continue;
            }
            try {
                ArrayList<Path> inputs = new ArrayList<>();
                for (ReusableAudioCoverage.Entry chunk : chunks) {
                    Path input = root.resolve(chunk.audio().audioRelativePath())
                            .toAbsolutePath().normalize();
                    if (!input.startsWith(root) || !Files.isRegularFile(input)
                            || Files.size(input) <= 44L) {
                        throw new IOException("chunk WAV ausente o inválido");
                    }
                    inputs.add(input);
                }
                String signature = signature(segmentId, units, inputs);
                String fileName = safe(segmentId) + ".wav";
                Path output = cache.resolve("audio").resolve(fileName).normalize();
                Path signatureFile = output.resolveSibling(fileName + ".sha256");
                boolean valid = Files.isRegularFile(output) && Files.size(output) > 44L
                        && Files.isRegularFile(signatureFile)
                        && signature.equals(Files.readString(signatureFile,
                        StandardCharsets.US_ASCII).strip());
                if (!valid) missing.add(segmentId);
            } catch (IOException failure) {
                missing.add(segmentId);
            }
        }
        boolean manifestExists = Files.isRegularFile(cache.resolve("audio-manifest.json"));
        return new Inspection(missing, manifestExists && !bySegment.isEmpty()
                && missing.isEmpty());
    }

    private Result materializeLocked(List<AudioGenerationUnit> current,
                                     ReusableAudioCoverage.Report coverage,
                                     Path root, Path cache) {
        LinkedHashMap<String, List<AudioGenerationUnit>> bySegment = new LinkedHashMap<>();
        for (AudioGenerationUnit unit : current == null
                ? List.<AudioGenerationUnit>of() : current) {
            String segmentId = unit.sourceSegmentId().isBlank()
                    ? unit.id() : unit.sourceSegmentId();
            bySegment.computeIfAbsent(segmentId, ignored -> new ArrayList<>()).add(unit);
        }
        Map<String, ReusableAudioCoverage.Entry> entries = new LinkedHashMap<>();
        coverage.entries().forEach(entry -> entries.put(entry.unit().id(), entry));
        ArrayList<AudioSegmentSnapshot> segmentAudio = new ArrayList<>();
        ArrayList<ManifestEntry> manifest = new ArrayList<>();
        ArrayList<String> failures = new ArrayList<>();
        int composed = 0;
        int reused = 0;
        try {
            Files.createDirectories(cache.resolve("audio"));
            for (Map.Entry<String, List<AudioGenerationUnit>> group : bySegment.entrySet()) {
                String segmentId = group.getKey();
                List<AudioGenerationUnit> units = List.copyOf(group.getValue());
                List<ReusableAudioCoverage.Entry> chunks = units.stream()
                        .map(unit -> entries.get(unit.id())).toList();
                if (chunks.stream().anyMatch(entry -> entry == null || !entry.ready())) {
                    failures.add(segmentId + ": faltan chunks vigentes");
                    continue;
                }
                try {
                    Composition composition = compose(root, cache, segmentId, units, chunks);
                    segmentAudio.add(composition.snapshot());
                    manifest.add(composition.manifest());
                    if (composition.reused()) reused++; else composed++;
                } catch (Exception failure) {
                    failures.add(segmentId + ": " + rootMessage(failure));
                }
            }
            Path manifestPath = cache.resolve("audio-manifest.json");
            writeAtomic(manifestPath, manifestJson(manifest));
            return new Result(segmentAudio, composed, reused, failures,
                    portable(root, manifestPath));
        } catch (IOException failure) {
            failures.add("manifest: " + rootMessage(failure));
            return new Result(segmentAudio, composed, reused, failures, "");
        }
    }

    private static Composition compose(Path root, Path cache, String segmentId,
                                       List<AudioGenerationUnit> units,
                                       List<ReusableAudioCoverage.Entry> chunks)
            throws Exception {
        ArrayList<Path> inputs = new ArrayList<>();
        for (ReusableAudioCoverage.Entry chunk : chunks) {
            Path input = root.resolve(chunk.audio().audioRelativePath())
                    .toAbsolutePath().normalize();
            if (!input.startsWith(root) || !Files.isRegularFile(input)
                    || Files.size(input) <= 44L) {
                throw new IOException("chunk WAV ausente o invalido: " + input);
            }
            inputs.add(input);
        }
        String signature = signature(segmentId, units, inputs);
        String fileName = safe(segmentId) + ".wav";
        Path output = cache.resolve("audio").resolve(fileName).normalize();
        Path signatureFile = output.resolveSibling(fileName + ".sha256");
        boolean reusable = Files.isRegularFile(output) && Files.size(output) > 44L
                && Files.isRegularFile(signatureFile)
                && signature.equals(Files.readString(signatureFile,
                StandardCharsets.US_ASCII).strip());
        if (!reusable) {
            Path temporary = output.resolveSibling(fileName + ".tmp");
            Files.deleteIfExists(temporary);
            try {
                mergeWav(inputs, temporary);
                moveAtomic(temporary, output);
                writeAtomic(signatureFile, signature + System.lineSeparator());
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        double duration = chunks.stream().mapToDouble(entry ->
                entry.audio().durationSeconds()).sum();
        AudioGenerationUnit first = units.getFirst();
        AudioSourceFingerprint downstreamFingerprint = AudioSourceFingerprint.composed(
                units.stream().map(AudioGenerationUnit::sourceFingerprint).toList());
        AudioSegmentSnapshot snapshot = AudioSegmentSnapshot.pending(
                        segmentId, first.effectiveTitle(), downstreamFingerprint)
                .completed(portable(root, output), duration);
        ManifestEntry manifest = new ManifestEntry(segmentId,
                units.stream().map(AudioGenerationUnit::id).toList(), signature,
                snapshot.audioRelativePath(), Files.size(output), fileSha256(output), duration,
                downstreamFingerprint);
        return new Composition(snapshot, manifest, reusable);
    }

    private static void mergeWav(List<Path> inputs, Path output) throws Exception {
        if (inputs.isEmpty()) throw new IOException("No hay chunks WAV para componer.");
        Files.createDirectories(output.getParent());
        if (inputs.size() == 1) {
            Files.copy(inputs.getFirst(), output, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        ArrayList<AudioInputStream> streams = new ArrayList<>();
        try {
            for (Path input : inputs) {
                streams.add(AudioSystem.getAudioInputStream(input.toFile()));
            }
            AudioFormat format = streams.getFirst().getFormat();
            for (AudioInputStream stream : streams) {
                if (!format.matches(stream.getFormat())) {
                    throw new IOException("Los chunks WAV del segmento tienen formatos incompatibles.");
                }
            }
            long frames = streams.stream().mapToLong(AudioInputStream::getFrameLength).sum();
            SequenceInputStream sequence = new SequenceInputStream(
                    Collections.enumeration(streams));
            try (AudioInputStream combined = new AudioInputStream(sequence, format, frames)) {
                AudioSystem.write(combined, AudioFileFormat.Type.WAVE, output.toFile());
            }
        } finally {
            for (AudioInputStream stream : streams) {
                try { stream.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static String signature(String segmentId,
                                    List<AudioGenerationUnit> units,
                                    List<Path> inputs) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        update(digest, segmentId);
        byte[] buffer = new byte[8192];
        for (int index = 0; index < units.size(); index++) {
            AudioGenerationUnit unit = units.get(index);
            update(digest, unit.id());
            update(digest, unit.sourceFingerprint().toString());
            try (InputStream input = Files.newInputStream(inputs.get(index))) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String fileSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String manifestJson(List<ManifestEntry> entries) {
        StringBuilder json = new StringBuilder("{\n  \"version\": 2,\n  \"segments\": [\n");
        for (int index = 0; index < entries.size(); index++) {
            ManifestEntry entry = entries.get(index);
            if (index > 0) json.append(",\n");
            json.append("    {\"segmentId\": \"").append(escape(entry.segmentId()))
                    .append("\", \"unitIds\": [");
            for (int unit = 0; unit < entry.unitIds().size(); unit++) {
                if (unit > 0) json.append(", ");
                json.append('"').append(escape(entry.unitIds().get(unit))).append('"');
            }
            json.append("], \"signature\": \"").append(entry.signature())
                    .append("\", \"audioRelativePath\": \"")
                    .append(escape(entry.audioRelativePath()))
                    .append("\", \"fileSize\": ").append(entry.fileSize())
                    .append(", \"fileSha256\": \"").append(entry.fileSha256())
                    .append("\", \"textSha256\": \"").append(entry.fingerprint().textSha256())
                    .append("\", \"voiceConfigurationSha256\": \"")
                    .append(entry.fingerprint().voiceConfigurationSha256())
                    .append("\", \"preprocessingSha256\": \"")
                    .append(entry.fingerprint().preprocessingSha256()).append('"')
                    .append(", \"durationSeconds\": ")
                    .append(String.format(java.util.Locale.ROOT, "%.6f", entry.durationSeconds()))
                    .append('}');
        }
        return json.append("\n  ]\n}\n").toString();
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        moveAtomic(temporary, target);
    }

    private static void moveAtomic(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String portable(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString()
                .replace('\\', '/');
    }

    private static String safe(String value) {
        String result = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "-");
        return result.isBlank() ? "segment" : result;
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null) current = current.getCause();
        return current == null || current.getMessage() == null
                ? "error de composicion" : current.getMessage();
    }

    record Result(List<AudioSegmentSnapshot> segmentAudio, int composed,
                  int reused, List<String> failures, String manifestRelativePath) {
        Result {
            segmentAudio = segmentAudio == null ? List.of() : List.copyOf(segmentAudio);
            failures = failures == null ? List.of() : List.copyOf(failures);
            manifestRelativePath = manifestRelativePath == null ? "" : manifestRelativePath;
        }

        boolean complete(int expectedSegments) {
            return failures.isEmpty() && expectedSegments > 0
                    && segmentAudio.size() == expectedSegments;
        }
    }

    record Inspection(List<String> missingSegmentIds, boolean complete) {
        Inspection {
            missingSegmentIds = missingSegmentIds == null
                    ? List.of() : List.copyOf(missingSegmentIds);
        }
    }

    private record Composition(AudioSegmentSnapshot snapshot,
                               ManifestEntry manifest, boolean reused) { }

    private record ManifestEntry(String segmentId, List<String> unitIds,
                                 String signature, String audioRelativePath,
                                 long fileSize, String fileSha256, double durationSeconds,
                                 AudioSourceFingerprint fingerprint) { }
}
