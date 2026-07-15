package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Versioned inventory of engine artifacts required or accepted by DocuPodcast Studio. */
public record EngineArtifactManifest(String formatVersion, List<EngineArtifactDescriptor> artifacts) {
    public EngineArtifactManifest {
        formatVersion = formatVersion == null || formatVersion.isBlank() ? "engine-artifacts-v1" : formatVersion.trim();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }

    public List<EngineArtifactDescriptor> requiredForFinalRc() {
        return artifacts.stream().filter(EngineArtifactDescriptor::requiredForFinalRc).toList();
    }

    public List<EngineArtifactDescriptor> pendingChecksumsForFinalRc() {
        return requiredForFinalRc().stream().filter(artifact -> !artifact.hasConcreteSha256()).toList();
    }

    public List<EngineArtifactDescriptor> pendingLicensesForFinalRc() {
        return requiredForFinalRc().stream().filter(EngineArtifactDescriptor::licensePending).toList();
    }
}
