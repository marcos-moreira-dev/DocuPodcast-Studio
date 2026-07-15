package com.marcosmoreiradev.docupodcaststudio.application.fragment;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.List;
import java.util.Objects;

/** Read-only coordinator for the canonical cross-media fragment projection. */
public final class FragmentWorkspaceProjectionCoordinator {
    private final BuildFragmentWorkspaceProjectionUseCase buildProjection;

    public FragmentWorkspaceProjectionCoordinator(BuildFragmentWorkspaceProjectionUseCase buildProjection) {
        this.buildProjection = Objects.requireNonNull(buildProjection, "buildProjection");
    }

    public FragmentWorkspaceProjection build(
            ReadableDocument document,
            NarrationScriptDocument script,
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> audioJobs,
            PlaybackManifest manifest
    ) {
        return buildProjection.build(document, script, project, storyboard, audioJobs, manifest);
    }

    public FragmentWorkspaceSummary summarize(
            ReadableDocument document,
            NarrationScriptDocument script,
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> audioJobs,
            PlaybackManifest manifest
    ) {
        FragmentWorkspaceProjection projection = build(document, script, project, storyboard, audioJobs, manifest);
        int audio = 0;
        int visual = 0;
        int theatre = 0;
        int playback = 0;
        for (var binding : projection.bindings()) {
            FragmentAssetRole role = binding.role();
            if (role == FragmentAssetRole.AUDIO_TTS || role == FragmentAssetRole.AUDIO_IMPORTED) {
                audio++;
            } else if (role == FragmentAssetRole.MAIN_IMAGE || role == FragmentAssetRole.DOCUMENT_IMAGE) {
                visual++;
            } else if (role == FragmentAssetRole.THEATRE_INTERVENTION || role == FragmentAssetRole.THEATRE_VISUAL) {
                theatre++;
            } else if (role == FragmentAssetRole.PLAYBACK_CUE) {
                playback++;
            }
        }
        return new FragmentWorkspaceSummary(projection.fragments().size(), audio, visual, theatre, playback);
    }
}
