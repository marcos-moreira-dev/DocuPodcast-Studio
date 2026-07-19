package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.util.Map;

/** Inspected model package used by a shared visual-generation profile. */
public record VisualModelManifest(
        String id,
        VisualGenerationProfile profile,
        Map<String, Path> components,
        Map<String, String> sha256,
        String licenseId,
        boolean licenseAccepted,
        long approximateBytes
) {
    public VisualModelManifest {
        id = clean(id);
        profile = profile == null ? VisualGenerationProfile.DIAGNOSTIC_SD15 : profile;
        components = components == null ? Map.of() : Map.copyOf(components);
        sha256 = sha256 == null ? Map.of() : Map.copyOf(sha256);
        licenseId = clean(licenseId);
        approximateBytes = Math.max(0L, approximateBytes);
    }

    public boolean complete() {
        return !components.isEmpty()
                && components.values().stream().allMatch(path -> path != null && java.nio.file.Files.isRegularFile(path))
                && (!profile.explicitLicenseAcceptance() || licenseAccepted);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
