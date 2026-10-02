package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreDrawingVaultItem;
import java.util.List;

/** Presentation context for editing a drawn storyboard frame for one narration intervention. */
public record TheatreFrameSketchContext(
        String segmentId,
        String interventionId,
        String title,
        String interventionText,
        String officialImageUri,
        String drawnFrameUri,
        String drawnFrameStateUri,
        String activeVisualVariant,
        String cameraGuideUri,
        String cameraGuideLabel,
        List<ObjectPreview> sceneObjects,
        List<TheatreDrawingVaultItem> drawingVault
) {
    public TheatreFrameSketchContext {
        segmentId = normalize(segmentId);
        interventionId = normalize(interventionId);
        title = normalize(title);
        interventionText = normalize(interventionText);
        officialImageUri = normalize(officialImageUri);
        drawnFrameUri = normalize(drawnFrameUri);
        drawnFrameStateUri = normalize(drawnFrameStateUri);
        activeVisualVariant = normalize(activeVisualVariant);
        cameraGuideUri = normalize(cameraGuideUri);
        cameraGuideLabel = normalize(cameraGuideLabel);
        sceneObjects = sceneObjects == null ? List.of() : List.copyOf(sceneObjects);
        drawingVault = drawingVault == null ? List.of() : List.copyOf(drawingVault);
    }

    public TheatreFrameSketchContext(String segmentId, String interventionId, String title,
                                     String interventionText, String officialImageUri, String drawnFrameUri,
                                     String drawnFrameStateUri, String activeVisualVariant, String cameraGuideUri,
                                     String cameraGuideLabel, List<ObjectPreview> sceneObjects) {
        this(segmentId, interventionId, title, interventionText, officialImageUri, drawnFrameUri,
                drawnFrameStateUri, activeVisualVariant, cameraGuideUri, cameraGuideLabel, sceneObjects, List.of());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public record ObjectPreview(String id, String name, String description, String imageUri) {
        public ObjectPreview {
            id = normalize(id);
            name = normalize(name);
            description = normalize(description);
            imageUri = normalize(imageUri);
        }
    }
}
