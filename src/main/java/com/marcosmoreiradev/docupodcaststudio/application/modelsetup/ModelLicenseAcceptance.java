package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.time.Instant;

/** User-owned acknowledgment for a gated model license. */
public record ModelLicenseAcceptance(String modelId, String licenseUrl, Instant acceptedAt) {
    public ModelLicenseAcceptance {
        modelId = modelId == null ? "" : modelId.strip();
        licenseUrl = licenseUrl == null ? "" : licenseUrl.strip();
    }

    public boolean validFor(String expectedModelId, String expectedLicenseUrl) {
        return acceptedAt != null
                && modelId.equals(expectedModelId)
                && licenseUrl.equals(expectedLicenseUrl);
    }
}
