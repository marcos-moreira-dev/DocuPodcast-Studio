package com.marcosmoreiradev.docupodcaststudio.application.ink;

/**
 * Future integration contract for theatre frame sketches.
 *
 * <p>This deliberately lives in application.ink so theatre can later depend on
 * the reusable ink state without depending on document-study UI classes.</p>
 */
public record TheatreFrameSketchInkHook(String frameId, InkWorkspaceState state) {
    public TheatreFrameSketchInkHook {
        frameId = frameId == null ? "" : frameId;
        state = state == null
                ? InkWorkspaceState.create(1, 1, "#ffffffff", java.util.List.of(), java.util.List.of(), java.util.Map.of())
                : state;
    }
}
