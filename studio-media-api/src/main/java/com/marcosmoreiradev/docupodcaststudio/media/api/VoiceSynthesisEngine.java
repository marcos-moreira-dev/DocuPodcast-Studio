package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public interface VoiceSynthesisEngine extends MediaEngine {
    VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException;

    /** Default batch behavior is portable; optimized engines may override it. */
    default VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("voice-batch") : context;
        ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
        int index = 0;
        for (VoiceSynthesisUnit unit : request.units()) {
            current.cancellation().throwIfCancellationRequested();
            results.add(synthesize(new VoiceSynthesisRequest(unit.text(), request.language(), unit.voiceId(),
                    unit.referenceAudio(), unit.outputFile(), request.options()), current));
            index++;
            current.progress().report("voice-unit", index / (double) request.units().size(), unit.id());
        }
        return new VoiceSynthesisBatchResult(results, Map.of("mode", "portable-loop"));
    }
}
