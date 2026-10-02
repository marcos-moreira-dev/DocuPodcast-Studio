package com.marcosmoreiradev.docupodcaststudio.infrastructure.reading;

import com.marcosmoreiradev.docupodcaststudio.application.reading.NarrationTranslationCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** Atomic, project-owned cache; values contain spoken text only, never PDF geometry. */
public final class FileNarrationTranslationCache implements NarrationTranslationCache {
    private static final String RELATIVE = "derived/narration-translation-v1.properties";
    private static final String FAILURES_RELATIVE =
            "derived/narration-translation-v1.failures.properties";

    @Override
    public synchronized Optional<String> find(Path projectRoot, String fingerprint)
            throws IOException {
        Path file = cacheFile(projectRoot);
        if (!Files.isRegularFile(file)) return Optional.empty();
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            values.load(input);
        }
        String translated = values.getProperty(fingerprint);
        return translated == null || translated.isBlank()
                ? Optional.empty() : Optional.of(translated);
    }

    @Override
    public synchronized void store(Path projectRoot, String fingerprint,
                                   String translatedText) throws IOException {
        Path file = cacheFile(projectRoot);
        Properties values = load(file);
        values.setProperty(fingerprint, translatedText);
        storeAtomically(file, values,
                "DocuPodcast derived narration translations V1");
    }

    @Override
    public synchronized Optional<Failure> findFailure(
            Path projectRoot, String fingerprint) throws IOException {
        Properties values = load(failureFile(projectRoot));
        String prefix = fingerprint + ".";
        String reason = values.getProperty(prefix + "reason");
        if (reason == null || reason.isBlank()) return Optional.empty();
        LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
        String diagnosticPrefix = prefix + "diagnostic.";
        values.stringPropertyNames().stream()
                .filter(key -> key.startsWith(diagnosticPrefix))
                .sorted()
                .forEach(key -> diagnostics.put(
                        key.substring(diagnosticPrefix.length()), values.getProperty(key, "")));
        Instant occurredAt;
        try {
            occurredAt = Instant.parse(values.getProperty(prefix + "occurredAt", ""));
        } catch (RuntimeException invalidTimestamp) {
            occurredAt = Instant.EPOCH;
        }
        return Optional.of(new Failure(
                values.getProperty(prefix + "segmentId", ""),
                values.getProperty(prefix + "targetLanguage", ""), reason,
                values.getProperty(prefix + "rawOutput", ""), occurredAt, diagnostics));
    }

    @Override
    public synchronized void storeFailure(
            Path projectRoot, String fingerprint, Failure failure) throws IOException {
        Path file = failureFile(projectRoot);
        Properties values = load(file);
        removeFailure(values, fingerprint);
        String prefix = fingerprint + ".";
        values.setProperty(prefix + "segmentId", failure.segmentId());
        values.setProperty(prefix + "targetLanguage", failure.targetLanguage());
        values.setProperty(prefix + "reason", failure.reason());
        values.setProperty(prefix + "rawOutput", failure.rawOutput());
        values.setProperty(prefix + "occurredAt", failure.occurredAt().toString());
        failure.diagnostics().forEach((key, value) -> values.setProperty(
                prefix + "diagnostic." + key, value == null ? "" : value));
        storeAtomically(file, values,
                "DocuPodcast narration translation failures V1");
    }

    @Override
    public synchronized void clearFailure(Path projectRoot, String fingerprint)
            throws IOException {
        Path file = failureFile(projectRoot);
        if (!Files.isRegularFile(file)) return;
        Properties values = load(file);
        if (!removeFailure(values, fingerprint)) return;
        storeAtomically(file, values,
                "DocuPodcast narration translation failures V1");
    }

    private static Properties load(Path file) throws IOException {
        Properties values = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                values.load(input);
            }
        }
        return values;
    }

    private static void storeAtomically(Path file, Properties values, String comment)
            throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(),
                "narration-translation-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, comment);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean removeFailure(Properties values, String fingerprint) {
        String prefix = fingerprint + ".";
        var keys = values.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix)).toList();
        keys.forEach(values::remove);
        return !keys.isEmpty();
    }

    private static Path cacheFile(Path projectRoot) throws IOException {
        return projectFile(projectRoot, RELATIVE);
    }

    private static Path failureFile(Path projectRoot) throws IOException {
        return projectFile(projectRoot, FAILURES_RELATIVE);
    }

    private static Path projectFile(Path projectRoot, String relative) throws IOException {
        if (projectRoot == null) throw new IOException("project root is required");
        Path root = projectRoot.toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) throw new IOException("translation cache escaped project root");
        return file;
    }
}
