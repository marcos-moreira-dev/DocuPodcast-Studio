package com.marcosmoreiradev.docupodcaststudio.ink;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of semi-abstract drawing compositions. */
public final class DrawingFeatureCatalog {
    public static final String DOCUMENT_PROBLEM = "document-problem";
    public static final String FREE_COMPOSITION = "free-composition";
    public static final String DOCUMENTARY_ILLUSTRATION = "documentary-illustration";
    public static final String THEATRE_FRAME = "theatre-frame";

    private final Map<String, DrawingProfile> profiles = new LinkedHashMap<>();

    public DrawingFeatureCatalog register(DrawingProfile profile) {
        if (profiles.putIfAbsent(profile.id(), profile) != null) {
            throw new IllegalArgumentException("duplicate drawing profile: " + profile.id());
        }
        return this;
    }

    public Optional<DrawingProfile> find(String id) { return Optional.ofNullable(profiles.get(id)); }

    public DrawingProfile require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("drawing profile not registered: " + id));
    }

    public List<DrawingProfile> profiles() { return List.copyOf(profiles.values()); }

    public static DrawingFeatureCatalog official() {
        return new DrawingFeatureCatalog()
                .register(new DrawingProfile(DOCUMENT_PROBLEM, "Problema documental", ViewportMode.GROWING,
                        980, 1800, InkInputPolicy.NATIVE_PREFERRED,
                        List.of(DrawingToolId.PEN, DrawingToolId.STRAIGHT_LINE,
                                DrawingToolId.ANGLE_MEASURE, DrawingToolId.ERASER,
                                DrawingToolId.IMAGE, DrawingToolId.CROP, DrawingToolId.PAN,
                                DrawingToolId.REGION_SELECT),
                        100, true, new DrawingExportProfile(4, true, true)))
                .register(new DrawingProfile(FREE_COMPOSITION, "Composición libre", ViewportMode.GROWING,
                        1280, 960, InkInputPolicy.MOUSE_AND_NATIVE,
                        List.of(DrawingToolId.PEN, DrawingToolId.ERASER, DrawingToolId.IMAGE, DrawingToolId.PAN),
                        100, true, new DrawingExportProfile(2, false, true)))
                .register(new DrawingProfile(DOCUMENTARY_ILLUSTRATION, "Ilustración documental", ViewportMode.FIXED,
                        1344, 432, InkInputPolicy.NATIVE_PREFERRED,
                        List.of(DrawingToolId.PEN, DrawingToolId.ERASER, DrawingToolId.IMAGE,
                                DrawingToolId.CROP, DrawingToolId.PAN),
                        20, true, new DrawingExportProfile(1, false, true)))
                .register(new DrawingProfile(THEATRE_FRAME, "Frame teatral", ViewportMode.FIXED,
                        1280, 720, InkInputPolicy.NATIVE_PREFERRED,
                        List.of(DrawingToolId.PEN, DrawingToolId.ERASER, DrawingToolId.PAN),
                        50, true, new DrawingExportProfile(3, false, true)));
    }
}
