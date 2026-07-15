package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import java.util.List;

/**
 * Lightweight projection for one image shown in the document mini storyboard rail.
 *
 * <p>The rail is intentionally document-first: clicking an assigned image should highlight
 * the original text range/block, while an unassigned image stays visible as "sin texto asignado".</p>
 */
public record DocumentRailImagePresentation(
        String assetId,
        String displayName,
        String relativePath,
        String imageFileUri,
        String assignedSegmentId,
        String assignedBlockId,
        String assignedTextPreview,
        int assignmentCount,
        List<String> assignedSegmentIds
) {
    public DocumentRailImagePresentation(String assetId,
                                         String displayName,
                                         String relativePath,
                                         String imageFileUri,
                                         String assignedSegmentId,
                                         String assignedBlockId,
                                         String assignedTextPreview) {
        this(assetId, displayName, relativePath, imageFileUri, assignedSegmentId, assignedBlockId, assignedTextPreview,
                normalize(assignedSegmentId).isBlank() && normalize(assignedBlockId).isBlank() ? 0 : 1,
                normalize(assignedSegmentId).isBlank() ? List.of() : List.of(normalize(assignedSegmentId)));
    }

    public DocumentRailImagePresentation {
        assetId = normalize(assetId);
        displayName = normalize(displayName).isBlank() ? assetId : normalize(displayName);
        relativePath = normalize(relativePath);
        imageFileUri = normalize(imageFileUri);
        assignedSegmentId = normalize(assignedSegmentId);
        assignedBlockId = normalize(assignedBlockId);
        assignedTextPreview = normalize(assignedTextPreview);
        assignmentCount = Math.max(assignmentCount, assignedToTextFields(assignedSegmentId, assignedBlockId) ? 1 : 0);
        assignedSegmentIds = assignedSegmentIds == null ? List.of() : List.copyOf(assignedSegmentIds);
    }

    public boolean assignedToText() {
        return !assignedSegmentId.isBlank() || !assignedBlockId.isBlank();
    }

    public String assignmentLabel() {
        if (!assignedToText()) {
            return "Sin texto asignado";
        }
        String target = assignedSegmentId.isBlank() ? assignedBlockId : assignedSegmentId;
        if (assignmentCount > 1) {
            return "Asignada a " + target + " y " + (assignmentCount - 1) + " textos más";
        }
        return "Asignada a " + target;
    }

    public String previewLabel() {
        if (!assignedToText()) {
            return "Selecciona texto del documento y asocia esta imagen cuando quieras usarla como frame.";
        }
        String preview = assignedTextPreview.isBlank() ? "Texto asignado sin vista previa." : assignedTextPreview;
        if (assignmentCount > 1) {
            return preview + " · Imagen reutilizada en " + assignmentCount + " fragmentos.";
        }
        return preview;
    }

    private static boolean assignedToTextFields(String assignedSegmentId, String assignedBlockId) {
        return !normalize(assignedSegmentId).isBlank() || !normalize(assignedBlockId).isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
