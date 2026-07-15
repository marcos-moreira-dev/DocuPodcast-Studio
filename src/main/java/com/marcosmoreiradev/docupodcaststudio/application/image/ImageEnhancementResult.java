package com.marcosmoreiradev.docupodcaststudio.application.image;

import java.nio.file.Path;

/** Result of a local image enhancement job. */
public record ImageEnhancementResult(
        boolean success,
        String jobId,
        Path inputCopy,
        Path intermediateImage,
        Path finalImage,
        Path manifest,
        String message,
        String providerId) {
    public ImageEnhancementResult {
        jobId = jobId == null ? "" : jobId;
        message = message == null ? "" : message;
        providerId = providerId == null ? "" : providerId;
    }

    public static ImageEnhancementResult success(ImageEnhancementRequest request,
                                                 Path inputCopy,
                                                 Path intermediateImage,
                                                 Path finalImage,
                                                 Path manifest,
                                                 String message) {
        return new ImageEnhancementResult(true, request.jobId(), inputCopy, intermediateImage, finalImage, manifest,
                message, request.providerId());
    }

    public static ImageEnhancementResult failure(ImageEnhancementRequest request, String message) {
        return new ImageEnhancementResult(false, request == null ? "" : request.jobId(), null, null, null, null,
                message, request == null ? "" : request.providerId());
    }
}
