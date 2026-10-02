package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.property.*;

import java.util.Objects;

/** Cross-experience session state. Feature-specific state does not belong here. */
public final class StudioSessionStore {
    private final ReadOnlyStringWrapper windowTitle = new ReadOnlyStringWrapper("DocuPodcast Studio — Inicio");
    private final ObjectProperty<WorkspaceKind> activeWorkspace = new SimpleObjectProperty<>(WorkspaceKind.WELCOME_HOME);
    private final ReadOnlyObjectWrapper<DocuPodcastProject> activeProject = new ReadOnlyObjectWrapper<>();
    private final StringProperty selectedScriptSegmentId = new SimpleStringProperty("");
    private final StringProperty selectedDocumentBlockId = new SimpleStringProperty("");
    private final ObjectProperty<DocumentTextRange> selectedDocumentTextRange = new SimpleObjectProperty<>();
    private final ObjectProperty<PdfRegionSelectionRef> selectedPdfRegion = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper projectOpen = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper(
            "Listo. Abre un Word/DOCX o crea un proyecto DocuPodcast.");

    public ReadOnlyStringProperty windowTitleProperty() { return windowTitle.getReadOnlyProperty(); }
    public ObjectProperty<WorkspaceKind> activeWorkspaceProperty() { return activeWorkspace; }
    public ReadOnlyObjectProperty<DocuPodcastProject> activeProjectProperty() { return activeProject.getReadOnlyProperty(); }
    public ReadOnlyStringProperty selectedScriptSegmentIdProperty() { return selectedScriptSegmentId; }
    public ReadOnlyStringProperty selectedDocumentBlockIdProperty() { return selectedDocumentBlockId; }
    public ReadOnlyObjectProperty<DocumentTextRange> selectedDocumentTextRangeProperty() { return selectedDocumentTextRange; }
    public ReadOnlyObjectProperty<PdfRegionSelectionRef> selectedPdfRegionProperty() { return selectedPdfRegion; }
    public ReadOnlyBooleanProperty projectOpenProperty() { return projectOpen.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty dirtyProperty() { return dirty.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }

    public Snapshot snapshot() {
        return new Snapshot(activeProject.get(), activeWorkspace.get(), selectedScriptSegmentId.get(),
                selectedDocumentBlockId.get(), selectedDocumentTextRange.get(), projectOpen.get(), dirty.get(),
                statusMessage.get(), selectedPdfRegion.get());
    }

    void updateProject(DocuPodcastProject project, boolean open, boolean dirty) {
        activeProject.set(project);
        projectOpen.set(open);
        this.dirty.set(dirty);
    }
    void clearProject() { updateProject(null, false, false); }
    void updateStatus(String message) { statusMessage.set(message == null || message.isBlank() ? "Listo." : message.strip()); }

    ReadOnlyStringWrapper windowTitleState() { return windowTitle; }
    ObjectProperty<WorkspaceKind> activeWorkspaceState() { return activeWorkspace; }
    StringProperty selectedScriptSegmentIdState() { return selectedScriptSegmentId; }
    StringProperty selectedDocumentBlockIdState() { return selectedDocumentBlockId; }
    ObjectProperty<DocumentTextRange> selectedDocumentTextRangeState() { return selectedDocumentTextRange; }
    ObjectProperty<PdfRegionSelectionRef> selectedPdfRegionState() { return selectedPdfRegion; }
    ReadOnlyBooleanWrapper projectOpenState() { return projectOpen; }
    ReadOnlyBooleanWrapper dirtyState() { return dirty; }
    ReadOnlyStringWrapper statusMessageState() { return statusMessage; }

    public record Snapshot(DocuPodcastProject activeProject, WorkspaceKind activeWorkspace,
                           String selectedScriptSegmentId, String selectedDocumentBlockId,
                           DocumentTextRange selectedDocumentTextRange, boolean projectOpen,
                           boolean dirty, String statusMessage,
                           PdfRegionSelectionRef selectedPdfRegion) {
        public Snapshot {
            activeWorkspace = Objects.requireNonNullElse(activeWorkspace, WorkspaceKind.WELCOME_HOME);
            selectedScriptSegmentId = Objects.toString(selectedScriptSegmentId, "");
            selectedDocumentBlockId = Objects.toString(selectedDocumentBlockId, "");
            statusMessage = Objects.toString(statusMessage, "");
        }
    }
}
