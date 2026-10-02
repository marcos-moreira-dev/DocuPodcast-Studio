package com.marcosmoreiradev.docupodcaststudio.presentation.document;

/** One sentence-level fragment shown in the right Visual rail. */
public record DocumentFragmentRailPresentation(
        String unitId,
        String segmentId,
        String blockId,
        int startOffset,
        int endOffset,
        String title,
        String preview,
        String imageAssetId,
        String imageFileUri,
        String officialImageAssetId,
        String officialImageFileUri,
        String generatedImageAssetId,
        String generatedImageFileUri,
        String drawnFrameAssetId,
        String drawnFrameFileUri,
        String sceneryImageAssetId,
        String sceneryImageFileUri,
        String activeVisualVariant
) {
    public DocumentFragmentRailPresentation(
            String unitId,
            String segmentId,
            String blockId,
            int startOffset,
            int endOffset,
            String title,
            String preview,
            String imageAssetId,
            String imageFileUri) {
        this(unitId, segmentId, blockId, startOffset, endOffset, title, preview, imageAssetId, imageFileUri,
                "", "", "", "", "", "", "", "", "");
    }

    public DocumentFragmentRailPresentation {
        unitId = normalize(unitId);
        segmentId = normalize(segmentId);
        blockId = normalize(blockId);
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        title = normalize(title).isBlank() ? previewTitle(preview) : normalize(title);
        preview = normalize(preview);
        imageAssetId = normalize(imageAssetId);
        imageFileUri = normalize(imageFileUri);
        officialImageAssetId = normalize(officialImageAssetId);
        officialImageFileUri = normalize(officialImageFileUri);
        generatedImageAssetId = normalize(generatedImageAssetId);
        generatedImageFileUri = normalize(generatedImageFileUri);
        drawnFrameAssetId = normalize(drawnFrameAssetId);
        drawnFrameFileUri = normalize(drawnFrameFileUri);
        sceneryImageAssetId = normalize(sceneryImageAssetId);
        sceneryImageFileUri = normalize(sceneryImageFileUri);
        activeVisualVariant = normalize(activeVisualVariant);
    }

    public boolean imageReady() { return !imageAssetId.isBlank() && !imageFileUri.isBlank(); }

    public boolean hasDrawnFrame() { return !drawnFrameAssetId.isBlank() && !drawnFrameFileUri.isBlank(); }

    public boolean hasGeneratedImage() { return !generatedImageAssetId.isBlank() && !generatedImageFileUri.isBlank(); }

    public boolean hasOfficialImage() { return !officialImageAssetId.isBlank() && !officialImageFileUri.isBlank(); }

    public boolean hasSceneryImage() { return !sceneryImageAssetId.isBlank() && !sceneryImageFileUri.isBlank(); }

    public int visualVariantCount() {
        return (hasOfficialImage() ? 1 : 0) + (hasGeneratedImage() ? 1 : 0)
                + (hasDrawnFrame() ? 1 : 0) + (hasSceneryImage() ? 1 : 0);
    }

    public boolean drawnVariantActive() { return "drawn".equalsIgnoreCase(activeVisualVariant); }

    public boolean generatedVariantActive() { return "generated".equalsIgnoreCase(activeVisualVariant); }

    public boolean sceneryVariantActive() { return "scenery".equalsIgnoreCase(activeVisualVariant); }

    public String relationLabel() { return unitId.isBlank() ? "Fragmento" : "Fragmento " + unitId; }

    public String thumbnailLabel() {
        if (imageReady() && drawnVariantActive()) return "Frame dibujado";
        if (imageReady() && generatedVariantActive()) return "Imagen IA";
        if (imageReady() && sceneryVariantActive()) return "Escena";
        if (imageReady()) return "Imagen";
        return "Boceto pendiente";
    }

    public String activeVariantLabel() {
        if (imageReady() && drawnVariantActive()) return "Frame dibujado";
        if (imageReady() && generatedVariantActive()) return "Imagen generada por IA";
        if (imageReady() && sceneryVariantActive()) return "Personajes y escenografía";
        if (imageReady()) return "Imagen";
        return "Boceto pendiente";
    }

    public String cardStateCssClass() {
        return imageReady() ? "storyboard-scene-ready" : "storyboard-scene-incomplete";
    }

    private static String previewTitle(String text) {
        String value = normalize(text);
        if (value.length() <= 48) return value.isBlank() ? "Fragmento" : value;
        return value.substring(0, 48).strip() + "...";
    }

    private static String normalize(String value) { return value == null ? "" : value.strip(); }
}
