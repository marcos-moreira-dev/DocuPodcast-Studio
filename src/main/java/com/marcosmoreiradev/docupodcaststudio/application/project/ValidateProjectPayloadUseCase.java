package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates that the declared project kind is consistent with the payload available today.
 */
public final class ValidateProjectPayloadUseCase {
    public ProjectValidationResult validate(DocuPodcastProject project) {
        Objects.requireNonNull(project, "project");
        List<String> messages = new ArrayList<>();
        ProjectKind kind = project.metadata().kind();
        switch (kind) {
            case EMPTY -> { }
            case DOCUMENT_ONLY -> requireAny(project, messages, "DOCUMENT_ONLY requires a source/imported document",
                    ProjectAssetKind.SOURCE_DOCUMENT, ProjectAssetKind.IMPORTED_DOCUMENT);
            case NARRATION_SCRIPT -> requireAny(project, messages, "NARRATION_SCRIPT requires a narration script asset",
                    ProjectAssetKind.NARRATION_SCRIPT);
            case STORYBOARD -> {
                requireAny(project, messages, "STORYBOARD requires a narration script asset", ProjectAssetKind.NARRATION_SCRIPT);
                requireAny(project, messages, "STORYBOARD requires a storyboard manifest asset", ProjectAssetKind.STORYBOARD_MANIFEST);
            }
            case AUDIO_PROJECT -> {
                requireAny(project, messages, "AUDIO_PROJECT requires a narration script asset", ProjectAssetKind.NARRATION_SCRIPT);
                requireAny(project, messages, "AUDIO_PROJECT requires audio clips, final audio or audio manifest",
                        ProjectAssetKind.AUDIO_CLIP, ProjectAssetKind.AUDIO_FINAL, ProjectAssetKind.AUDIO_MANIFEST);
            }
            case FULL_PROJECT -> requireAny(project, messages, "FULL_PROJECT requires at least a narration script asset",
                    ProjectAssetKind.NARRATION_SCRIPT);
        }
        return messages.isEmpty() ? ProjectValidationResult.ok() : ProjectValidationResult.invalid(messages);
    }

    private static void requireAny(DocuPodcastProject project, List<String> messages, String message, ProjectAssetKind... kinds) {
        for (ProjectAssetKind kind : kinds) {
            if (project.assets().containsKind(kind)) {
                return;
            }
        }
        messages.add(message);
    }
}
