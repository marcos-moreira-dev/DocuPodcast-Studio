package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;

/**
 * Presentation projection for one project-side narrative layer shown in the
 * document rail. It keeps the UI language simple while the assignment remains a
 * precise project annotation, never text written into the imported Word/DOCX.
 */
public record DocumentLayerAssignmentPresentation(
        String id,
        String kindLabel,
        String displayName,
        String targetLabel,
        String rangeLabel,
        String cssClass,
        boolean primaryLayer
) {
    public DocumentLayerAssignmentPresentation {
        id = normalize(id);
        kindLabel = normalize(kindLabel);
        displayName = normalize(displayName);
        targetLabel = normalize(targetLabel);
        rangeLabel = normalize(rangeLabel);
        cssClass = normalize(cssClass).isBlank() ? "document-layer-assignment" : normalize(cssClass);
    }

    public static DocumentLayerAssignmentPresentation from(NarrativeLayerAssignment assignment) {
        String range = assignment.documentRange() == null
                ? assignment.textRange().segmentId() + " " + assignment.textRange().startOffset() + ".." + assignment.textRange().endOffset()
                : assignment.documentRange().displayLabel();
        String css = assignment.primaryNarrationLayer()
                ? "document-layer-assignment-primary"
                : "document-layer-assignment-stackable";
        return new DocumentLayerAssignmentPresentation(
                assignment.id(),
                assignment.kind().displayName(),
                assignment.displayName(),
                assignment.targetId(),
                range,
                css,
                assignment.primaryNarrationLayer());
    }

    public String accessibilityLabel() {
        return kindLabel + ": " + displayName + " — " + rangeLabel;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
