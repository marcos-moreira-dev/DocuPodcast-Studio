package com.marcosmoreiradev.docupodcaststudio.application.download;

/** Progress event for managed downloads. */
public record DownloadProgress(
        String downloadId,
        long downloadedBytes,
        long totalBytes,
        String message
) {
    public DownloadProgress {
        downloadId = downloadId == null ? "" : downloadId.strip();
        downloadedBytes = Math.max(0, downloadedBytes);
        totalBytes = Math.max(-1, totalBytes);
        message = message == null ? "" : message.strip();
    }

    public double ratio() {
        return totalBytes <= 0 ? -1.0 : Math.min(1.0, downloadedBytes / (double) totalBytes);
    }
}
