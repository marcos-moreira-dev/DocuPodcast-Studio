package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Objects;

/** Visual production state for one canonical fragment. */
public record VisualFragmentState(
        FragmentId fragmentId,
        int order,
        String segmentId,
        String sourceBlockId,
        String title,
        String text,
        boolean narratable,
        VisualSlotState mainImage,
        VisualSlotState bridgeImage,
        List<VisualSlotState> documentSupport,
        List<VisualSlotState> theatreVisuals,
        List<VisualDiagnostic> diagnostics
) {
    public VisualFragmentState {
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        order = Math.max(0, order);
        segmentId = normalize(segmentId);
        sourceBlockId = normalize(sourceBlockId);
        title = normalize(title).isBlank() ? fragmentId.value() : normalize(title);
        text = normalize(text);
        mainImage = mainImage == null ? VisualSlotState.empty(FragmentAssetRole.MAIN_IMAGE) : mainImage;
        bridgeImage = bridgeImage == null ? VisualSlotState.empty(FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT) : bridgeImage;
        documentSupport = documentSupport == null ? List.of() : List.copyOf(documentSupport);
        theatreVisuals = theatreVisuals == null ? List.of() : List.copyOf(theatreVisuals);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public boolean mainImageReady() {
        return mainImage.ready();
    }

    public boolean bridgeImageReady() {
        return bridgeImage.ready();
    }

    public boolean hasDocumentSupport() {
        return documentSupport.stream().anyMatch(VisualSlotState::assigned);
    }

    public boolean hasTheatreVisuals() {
        return theatreVisuals.stream().anyMatch(VisualSlotState::assigned);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
