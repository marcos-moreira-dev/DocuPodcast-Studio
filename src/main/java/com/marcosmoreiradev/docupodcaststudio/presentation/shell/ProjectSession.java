package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable UI session for the active project tab.
 *
 * <p>This is presentation state, not the persisted project itself. The
 * persisted root remains {@link DocuPodcastProject}; this session tracks the
 * current file path, dirty flag and transient imported document/script previews.</p>
 */
public final class ProjectSession {
    private DocuPodcastProject project;
    private Path projectFile;
    private boolean dirty;
    private ProjectDocumentSource documentSource;
    private NarrationScriptDocument narrationScript;
    private StoryboardDocument storyboard;

    private ProjectSession(DocuPodcastProject project, Path projectFile, boolean dirty) {
        this.project = Objects.requireNonNull(project, "project");
        this.projectFile = projectFile;
        this.dirty = dirty;
    }

    public static ProjectSession newUnsaved(DocuPodcastProject project) {
        return new ProjectSession(project, null, true);
    }

    public static ProjectSession opened(DocuPodcastProject project, Path projectFile) {
        return new ProjectSession(project, Objects.requireNonNull(projectFile, "projectFile"), false);
    }

    public DocuPodcastProject project() {
        return project;
    }

    public Optional<Path> projectFile() {
        return Optional.ofNullable(projectFile);
    }

    public boolean dirty() {
        return dirty;
    }

    public boolean saveable() {
        return projectFile != null;
    }

    public String title() {
        return project.metadata().title();
    }

    public Optional<ReadableDocument> importedDocument() {
        return documentSource instanceof BlockDocumentSource block
                ? Optional.of(block.document()) : Optional.empty();
    }

    public Optional<ProjectDocumentSource> documentSource() {
        return Optional.ofNullable(documentSource);
    }

    public Optional<PreparedPdfSource> preparedPdfSource() {
        return documentSource instanceof PreparedPdfSource pdf ? Optional.of(pdf) : Optional.empty();
    }

    public Optional<NarrationScriptDocument> narrationScript() {
        return Optional.ofNullable(narrationScript);
    }

    public Optional<StoryboardDocument> storyboard() {
        return Optional.ofNullable(storyboard);
    }

    public void replaceProject(DocuPodcastProject project, boolean markDirty) {
        this.project = Objects.requireNonNull(project, "project");
        this.dirty = markDirty;
    }

    /**
     * Updates lightweight persisted view state without changing the content dirty flag.
     * Used by navigation so moving through workspaces does not look like an edited document.
     */
    public void replaceProjectPreservingDirty(DocuPodcastProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean(Path savedFile) {
        this.projectFile = Objects.requireNonNull(savedFile, "savedFile");
        this.dirty = false;
    }


    public void hydrateImportedDocument(ReadableDocument importedDocument) {
        this.documentSource = new BlockDocumentSource(
                Objects.requireNonNull(importedDocument, "importedDocument"));
    }

    public void hydrateDocumentSource(ProjectDocumentSource source) {
        this.documentSource = Objects.requireNonNull(source, "source");
    }

    public void hydrateNarrationScript(NarrationScriptDocument narrationScript) {
        this.narrationScript = Objects.requireNonNull(narrationScript, "narrationScript");
    }

    public void hydrateStoryboard(StoryboardDocument storyboard) {
        this.storyboard = Objects.requireNonNull(storyboard, "storyboard");
    }

    public void setImportedDocument(ReadableDocument importedDocument) {
        this.documentSource = new BlockDocumentSource(
                Objects.requireNonNull(importedDocument, "importedDocument"));
        this.dirty = true;
    }

    public void setDocumentSource(ProjectDocumentSource source) {
        this.documentSource = Objects.requireNonNull(source, "source");
        this.dirty = true;
    }

    public void setNarrationScript(NarrationScriptDocument narrationScript) {
        this.narrationScript = Objects.requireNonNull(narrationScript, "narrationScript");
        this.dirty = true;
    }

    public void setStoryboard(StoryboardDocument storyboard) {
        this.storyboard = Objects.requireNonNull(storyboard, "storyboard");
        this.dirty = true;
    }

    public void clearStoryboard() {
        this.storyboard = null;
        this.dirty = true;
    }

    public void clearNarrationScript() {
        this.narrationScript = null;
        this.dirty = true;
    }
}
