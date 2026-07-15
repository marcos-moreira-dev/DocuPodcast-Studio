package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

/** Persists only the user's local acknowledgment; it never authenticates with Hugging Face. */
public final class FluxLicenseAcceptanceStore {
    public static final String MODEL_ID = "black-forest-labs/FLUX.1-dev";
    public static final String LICENSE_URL = "https://huggingface.co/black-forest-labs/FLUX.1-dev";
    private static final String FILE_NAME = ".flux-license-acceptance.properties";

    public Optional<ModelLicenseAcceptance> load(Path applicationRoot) {
        Path path = file(applicationRoot);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            Instant acceptedAt = Instant.parse(properties.getProperty("acceptedAt", ""));
            return Optional.of(new ModelLicenseAcceptance(
                    properties.getProperty("modelId", ""),
                    properties.getProperty("licenseUrl", ""),
                    acceptedAt));
        } catch (IOException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    public ModelLicenseAcceptance accept(Path applicationRoot) throws IOException {
        ModelLicenseAcceptance acceptance = new ModelLicenseAcceptance(MODEL_ID, LICENSE_URL, Instant.now());
        Path path = file(applicationRoot);
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty("modelId", acceptance.modelId());
        properties.setProperty("licenseUrl", acceptance.licenseUrl());
        properties.setProperty("acceptedAt", acceptance.acceptedAt().toString());
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "DocuPodcast Studio FLUX.1-dev local license acknowledgment");
        }
        return acceptance;
    }

    public void revoke(Path applicationRoot) throws IOException {
        Files.deleteIfExists(file(applicationRoot));
    }

    public boolean accepted(Path applicationRoot) {
        return load(applicationRoot)
                .map(value -> value.validFor(MODEL_ID, LICENSE_URL))
                .orElse(false);
    }

    private static Path file(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize()
                : applicationRoot.toAbsolutePath().normalize();
        return root.resolve("models/image").resolve(FILE_NAME);
    }
}
