package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public interface VoiceSynthesisEngine extends MediaEngine {
    VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException;

    /**
     * Applies only the acoustic/runtime-specific text policy after the caller's
     * provider-neutral normalization. Engines should not repeat document parsing
     * or infer their policy from display names.
     */
    default String prepareText(String normalizedText) {
        return normalizedText == null ? "" : normalizedText.strip();
    }

    /**
     * Declares this engine's own admission demand. Built-in and high-resource
     * engines override this; the default remains conservative for third-party
     * local engines instead of assuming XTTS internals.
     */
    default ComputeResourceDemand resourceDemand(ComputePreference preference) {
        final long mib = 1024L * 1024L;
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                256L * mib, 0L, true, ComputeDeviceId.CPU_0,
                0, null, EncoderResourceDemand.none());
    }

    /** Technical acoustic identity used by cache/fingerprint layers. */
    default String acousticFingerprint() {
        EngineDescriptor descriptor = descriptor();
        return descriptor.id().value() + "|" + descriptor.version()
                + "|" + descriptor.runtimeKind();
    }

    /** Default batch behavior is portable; optimized engines may override it. */
    default VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("voice-batch") : context;
        ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
        int index = 0;
        for (VoiceSynthesisUnit unit : request.units()) {
            current.cancellation().throwIfCancellationRequested();
            LinkedHashMap<String, String> options = new LinkedHashMap<>(request.options());
            options.putAll(unit.metadata());
            results.add(synthesize(new VoiceSynthesisRequest(unit.text(), request.language(), unit.voiceId(),
                    unit.referenceAudio(), unit.outputFile(), options), current));
            index++;
            current.progress().report("voice-unit", index / (double) request.units().size(), unit.id());
        }
        return new VoiceSynthesisBatchResult(results, Map.of("mode", "portable-loop"));
    }
}
