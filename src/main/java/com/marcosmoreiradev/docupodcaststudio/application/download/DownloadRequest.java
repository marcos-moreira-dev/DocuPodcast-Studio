package com.marcosmoreiradev.docupodcaststudio.application.download;

import java.nio.file.Path;
import java.util.Map;

/** Declarative download request used by managed engine/tool downloads. */
public record DownloadRequest(
        String downloadId,
        String sourceUrl,
        Path targetFile,
        boolean resumeAllowed,
        Map<String, String> headers
) {
    public DownloadRequest {
        downloadId = normalize(downloadId);
        sourceUrl = normalize(sourceUrl);
        headers = Map.copyOf(headers == null ? Map.of() : headers);
        if (downloadId.isBlank()) {
            throw new IllegalArgumentException("downloadId is required");
        }
        if (sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl is required");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile is required");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
