package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineCertificationRecord;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineCertificationStore;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Properties;

/** Atomic properties-backed certification repository outside document projects. */
final class FileEngineCertificationStore implements EngineCertificationStore {
    private final Path root;

    FileEngineCertificationStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<EngineCertificationRecord> find(EngineId engineId, String modelId)
            throws IOException {
        Path file = file(engineId, modelId);
        if (!Files.isRegularFile(file)) return Optional.empty();
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
        }
        LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
        properties.stringPropertyNames().stream()
                .filter(name -> name.startsWith("diagnostic."))
                .sorted()
                .forEach(name -> diagnostics.put(name.substring("diagnostic.".length()),
                        properties.getProperty(name, "")));
        try {
            return Optional.of(new EngineCertificationRecord(
                    engineId,
                    properties.getProperty("runtimeVersion", ""),
                    properties.getProperty("modelId", ""),
                    properties.getProperty("hardwareFingerprint", ""),
                    properties.getProperty("smokeTestId", ""),
                    Instant.parse(properties.getProperty("certifiedAt")),
                    Boolean.parseBoolean(properties.getProperty("visualInputVerified", "false")),
                    diagnostics));
        } catch (RuntimeException corrupt) {
            return Optional.empty();
        }
    }

    @Override
    public void save(EngineCertificationRecord record) throws IOException {
        Files.createDirectories(root);
        Properties properties = new Properties();
        properties.setProperty("engineId", record.engineId().value());
        properties.setProperty("runtimeVersion", record.runtimeVersion());
        properties.setProperty("modelId", record.modelId());
        properties.setProperty("hardwareFingerprint", record.hardwareFingerprint());
        properties.setProperty("smokeTestId", record.smokeTestId());
        properties.setProperty("certifiedAt", record.certifiedAt().toString());
        properties.setProperty("visualInputVerified",
                Boolean.toString(record.visualInputVerified()));
        record.diagnostics().forEach((key, value) ->
                properties.setProperty("diagnostic." + safe(key), value));
        Path target = file(record.engineId(), record.modelId());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (var output = Files.newOutputStream(temporary)) {
            properties.store(output, "DocuPodcast engine certification");
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void invalidate(EngineId engineId, String modelId) throws IOException {
        Files.deleteIfExists(file(engineId, modelId));
    }

    private Path file(EngineId engineId, String modelId) {
        return root.resolve(safe(engineId.value()) + "--" + safe(modelId) + ".properties");
    }

    private static String safe(String value) {
        return java.util.Objects.toString(value, "").replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
