package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.net.URI;

/** User-facing preflight for an explicit heavy image package download. */
public record ImagePackageDownloadPreflight(
        ImageModelPackageProfile profile,
        URI uri,
        long expectedBytes,
        int statusCode,
        boolean downloadable,
        boolean requiresAuthentication,
        String userMessage
) {
    public ImagePackageDownloadPreflight {
        profile = profile == null ? ImageModelPackageProfile.TEST_4GB_SD15 : profile;
        expectedBytes = expectedBytes <= 0 ? profile.approximateBytes() : expectedBytes;
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
