package com.marcosmoreiradev.docupodcaststudio.application.image;

/** Queue metadata for an enhancement job. */
public record ImageEnhancementJob(
        String id,
        ImageEnhancementRequest request,
        ImageEnhancementResult result,
        String status,
        boolean running) {
    public ImageEnhancementJob {
        id = id == null ? "" : id;
        status = status == null ? "" : status;
    }

    public ImageEnhancementJob withStatus(String newStatus, boolean newRunning) {
        return new ImageEnhancementJob(id, request, result, newStatus, newRunning);
    }

    public ImageEnhancementJob withResult(ImageEnhancementResult newResult, String newStatus) {
        return new ImageEnhancementJob(id, request, newResult, newStatus, false);
    }
}
