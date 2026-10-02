package com.marcosmoreiradev.docupodcaststudio.domain.batch;

import java.time.Instant;
import java.util.Objects;

public record DocumentVideoBatchItem(
        String id,
        int order,
        String title,
        String sourceRelativePath,
        String copiedSourceRelativePath,
        String childProjectRelativePath,
        String outputVideoRelativePath,
        String sha256,
        long sourceBytes,
        int embeddedMediaCount,
        BatchItemState state,
        BatchItemStage stage,
        double progress,
        String message,
        Instant updatedAt
) {
    public DocumentVideoBatchItem {
        id = Objects.requireNonNull(id, "id");
        title = Objects.requireNonNull(title, "title");
        sourceRelativePath = Objects.requireNonNull(sourceRelativePath, "sourceRelativePath");
        copiedSourceRelativePath = Objects.requireNonNull(copiedSourceRelativePath, "copiedSourceRelativePath");
        childProjectRelativePath = Objects.requireNonNull(childProjectRelativePath, "childProjectRelativePath");
        outputVideoRelativePath = Objects.requireNonNull(outputVideoRelativePath, "outputVideoRelativePath");
        sha256 = Objects.requireNonNull(sha256, "sha256");
        state = state == null ? BatchItemState.PENDING : state;
        stage = stage == null ? BatchItemStage.DISCOVERED : stage;
        progress = Math.max(0.0, Math.min(1.0, progress));
        message = message == null ? "" : message;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    /** Generic output accessor; the persisted legacy field remains readable for old video batches. */
    public String outputRelativePath() { return outputVideoRelativePath; }

    public DocumentVideoBatchItem withState(BatchItemState next, BatchItemStage nextStage, double nextProgress,
                                             String nextMessage) {
        return new DocumentVideoBatchItem(id, order, title, sourceRelativePath, copiedSourceRelativePath,
                childProjectRelativePath, outputVideoRelativePath, sha256, sourceBytes, embeddedMediaCount,
                next, nextStage, nextProgress, nextMessage, Instant.now());
    }
}
