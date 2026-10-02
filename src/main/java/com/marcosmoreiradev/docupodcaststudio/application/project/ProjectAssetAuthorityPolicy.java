package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import java.util.EnumSet;

/** Separates canonical/user-owned assets from reproducible delivery artifacts. */
public final class ProjectAssetAuthorityPolicy {
    private static final EnumSet<ProjectAssetKind> RECOVERABLE = EnumSet.of(
            ProjectAssetKind.THUMBNAIL, ProjectAssetKind.STUDY_SOURCE_CROP,
            ProjectAssetKind.AUDIO_FINAL, ProjectAssetKind.AUDIO_MANIFEST,
            ProjectAssetKind.PLAYBACK_MANIFEST, ProjectAssetKind.GENERATION_LOG,
            ProjectAssetKind.EXPORT);

    public boolean recoverable(ProjectAssetKind kind) {
        return kind != null && RECOVERABLE.contains(kind);
    }

    public boolean authoritative(ProjectAssetKind kind) { return !recoverable(kind); }
}
