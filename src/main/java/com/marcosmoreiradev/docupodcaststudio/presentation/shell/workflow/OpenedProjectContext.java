package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectWorkspaceHydration;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

import java.util.Optional;

/** Result of opening and hydrating a DocuPodcast project workspace. */
public record OpenedProjectContext(
        DocuPodcastProject project,
        ProjectSession session,
        ProjectWorkspaceHydration hydration,
        WorkspaceKind activeWorkspace,
        String selectedScriptSegmentId,
        String lastStoryboardImageAssetId
) {
    public OpenedProjectContext {
        project = java.util.Objects.requireNonNull(project, "project");
        session = java.util.Objects.requireNonNull(session, "session");
        hydration = java.util.Objects.requireNonNull(hydration, "hydration");
        activeWorkspace = java.util.Objects.requireNonNull(activeWorkspace, "activeWorkspace");
        selectedScriptSegmentId = selectedScriptSegmentId == null ? "" : selectedScriptSegmentId;
        lastStoryboardImageAssetId = lastStoryboardImageAssetId == null ? "" : lastStoryboardImageAssetId;
    }

    public Optional<ReadableDocument> importedDocument() {
        return hydration.importedDocument();
    }

    public Optional<NarrationScriptDocument> narrationScript() {
        return hydration.narrationScript();
    }

    public Optional<StoryboardDocument> storyboard() {
        return hydration.storyboard();
    }
}
