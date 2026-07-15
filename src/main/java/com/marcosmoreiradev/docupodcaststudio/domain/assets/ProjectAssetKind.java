package com.marcosmoreiradev.docupodcaststudio.domain.assets;

/**
 * Asset categories referenced by a DocuPodcast project.
 *
 * <p>Assets are referenced by relative path in .docupodcast.json. Binary data is not embedded.</p>
 */
public enum ProjectAssetKind {
    SOURCE_DOCUMENT,
    IMPORTED_DOCUMENT,
    NARRATION_SCRIPT,
    IMAGE,
    THUMBNAIL,
    STUDY_SOURCE_CROP,
    STUDY_PROBLEM_IMAGE,
    STUDY_SOLUTION_IMAGE,
    VOICE_SAMPLE,
    VOICE_MODEL,
    VOICE_LIBRARY,
    VIDEO_SOURCE,
    AUDIO_CLIP,
    AUDIO_FINAL,
    AUDIO_MANIFEST,
    STORYBOARD_MANIFEST,
    PLAYBACK_MANIFEST,
    GENERATION_LOG,
    EXPORT,
    OTHER
}
