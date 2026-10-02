package com.marcosmoreiradev.docupodcaststudio.domain.batch;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Parent aggregate. Children remain ordinary Documentary Studio projects. */
public record DocumentVideoBatchProject(
        int formatVersion,
        String id,
        String title,
        String sourceRoot,
        String projectRoot,
        String descriptorRelativePath,
        DocumentVideoBatchProfile profile,
        List<DocumentVideoBatchItem> items,
        int ignoredFileCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public DocumentVideoBatchProject {
        if (formatVersion <= 0) formatVersion = CURRENT_FORMAT_VERSION;
        id = Objects.requireNonNull(id, "id");
        title = Objects.requireNonNull(title, "title");
        sourceRoot = Objects.requireNonNull(sourceRoot, "sourceRoot");
        projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        descriptorRelativePath = Objects.requireNonNull(descriptorRelativePath, "descriptorRelativePath");
        profile = profile == null ? DocumentVideoBatchProfile.defaults() : profile;
        items = items == null ? List.of() : List.copyOf(items);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }
}
