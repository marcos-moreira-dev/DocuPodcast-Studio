package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.Objects;

/** Updates a segment text while preserving segment identity and source traceability. */
public final class UpdateNarrationSegmentTextUseCase {
    public NarrationScriptDocument update(NarrationScriptDocument script, String segmentId, String narrationText) {
        Objects.requireNonNull(script, "script");
        var segment = script.segmentById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el segmento " + segmentId));
        return script.replaceSegment(segment.withNarrationText(narrationText));
    }
}
