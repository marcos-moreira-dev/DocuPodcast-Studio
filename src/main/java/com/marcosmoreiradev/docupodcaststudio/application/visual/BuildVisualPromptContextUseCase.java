package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the prompt context passed to visual generation without assigning assets implicitly. */
public final class BuildVisualPromptContextUseCase {
    public VisualGenerationRequest build(VisualFragmentState fragment, FragmentAssetRole role) {
        if (fragment == null) {
            return new VisualGenerationRequest("", "", role, "", "", "", "", Map.of());
        }
        FragmentAssetRole resolvedRole = role == FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT
                ? FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT
                : FragmentAssetRole.MAIN_IMAGE;
        VisualSlotState slot = resolvedRole == FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT
                ? fragment.bridgeImage()
                : fragment.mainImage();
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourceBlockId", fragment.sourceBlockId());
        metadata.put("narratable", Boolean.toString(fragment.narratable()));
        metadata.put("slotAssigned", Boolean.toString(slot.assigned()));
        metadata.putAll(slot.metadata());
        String prompt = slot.prompt().isBlank() ? fragment.text() : slot.prompt();
        String notes = slot.notes().isBlank() ? fragment.title() : slot.notes();
        return new VisualGenerationRequest(
                fragment.fragmentId().value(),
                fragment.segmentId(),
                resolvedRole,
                fragment.title(),
                fragment.text(),
                prompt,
                notes,
                metadata);
    }
}
