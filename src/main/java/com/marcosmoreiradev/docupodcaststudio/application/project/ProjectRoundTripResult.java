package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.List;
import java.util.Objects;

/** Result of persisting and reopening a DocuPodcast project from disk. */
public record ProjectRoundTripResult(
        DocuPodcastProject restoredProject,
        ProjectWorkspaceHydration hydration,
        List<AudioJobSnapshot> restoredAudioJobs,
        boolean expectedDocument,
        boolean expectedNarrationProjection,
        boolean expectedStoryboard,
        boolean expectedAudioJobs,
        int expectedNarrativeLayerCount,
        int expectedAssetCount,
        List<String> messages
) {
    public ProjectRoundTripResult {
        restoredProject = Objects.requireNonNull(restoredProject, "restoredProject");
        hydration = hydration == null ? ProjectWorkspaceHydration.empty() : hydration;
        restoredAudioJobs = restoredAudioJobs == null ? List.of() : List.copyOf(restoredAudioJobs);
        messages = messages == null ? List.of() : List.copyOf(messages);
        expectedNarrativeLayerCount = Math.max(0, expectedNarrativeLayerCount);
        expectedAssetCount = Math.max(0, expectedAssetCount);
    }

    public boolean documentRecovered() {
        return hydration.documentSource().isPresent();
    }

    public boolean narrationProjectionRecovered() {
        return hydration.narrationScript().isPresent();
    }

    public boolean storyboardRecovered() {
        return hydration.storyboard().isPresent();
    }

    public boolean audioJobsRecovered() {
        return !expectedAudioJobs || !restoredAudioJobs.isEmpty();
    }

    public boolean narrativeLayersRecovered() {
        return restoredProject.narrativeLayerAssignments().size() == expectedNarrativeLayerCount;
    }

    public boolean assetsRecovered() {
        return restoredProject.assets().size() >= expectedAssetCount;
    }

    public boolean successful() {
        return (!expectedDocument || documentRecovered())
                && (!expectedNarrationProjection || narrationProjectionRecovered())
                && (!expectedStoryboard || storyboardRecovered())
                && audioJobsRecovered()
                && narrativeLayersRecovered()
                && assetsRecovered();
    }

    public String summary() {
        return "Round-trip " + (successful() ? "aprobado" : "con observaciones")
                + ": documento=" + label(documentRecovered())
                + ", narracion=" + label(narrationProjectionRecovered())
                + ", storyboard=" + label(storyboardRecovered())
                + ", capas=" + restoredProject.narrativeLayerAssignments().size()
                + ", assets=" + restoredProject.assets().size()
                + ", jobsAudio=" + restoredAudioJobs.size();
    }

    private static String label(boolean value) {
        return value ? "ok" : "pendiente";
    }
}
