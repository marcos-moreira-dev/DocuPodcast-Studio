package com.marcosmoreiradev.docupodcaststudio.application.download;

import java.nio.file.Path;

/** Result of a managed download attempt. */
public record DownloadResult(
        String downloadId,
        Path targetFile,
        boolean completed,
        boolean resumed,
        boolean cancelled,
        long bytesWritten,
        String userMessage,
        String technicalDetail
) {
    public DownloadResult {
        downloadId = downloadId == null ? "" : downloadId.strip();
        bytesWritten = Math.max(0, bytesWritten);
        userMessage = userMessage == null ? "" : userMessage.strip();
        technicalDetail = technicalDetail == null ? "" : technicalDetail.strip();
    }

    public static DownloadResult completed(DownloadRequest request, long bytesWritten, boolean resumed) {
        return new DownloadResult(request.downloadId(), request.targetFile(), true, resumed, false, bytesWritten,
                "Descarga completada: " + request.downloadId() + ".", "");
    }

    public static DownloadResult cancelled(DownloadRequest request, long bytesWritten) {
        return new DownloadResult(request.downloadId(), request.targetFile(), false, false, true, bytesWritten,
                "Descarga cancelada: " + request.downloadId() + ".", "");
    }
}
