package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Runtime-only policy; it is deliberately not persisted in the project. */
public record PdfProcessingCapabilityProfile(
        HardwareClass hardwareClass,
        boolean ocrAvailable,
        boolean visualDescriptionAvailable,
        boolean tableStructureAvailable,
        boolean mathRecognitionAvailable,
        boolean mathSpeechAvailable,
        int maxParallelOcrPages,
        int prefetchPages,
        boolean serializeGenerativeAndTts
) {
    public enum HardwareClass { LIMITED, STANDARD }

    public PdfProcessingCapabilityProfile {
        hardwareClass = hardwareClass == null
                ? HardwareClass.LIMITED : hardwareClass;
        maxParallelOcrPages = Math.max(1, maxParallelOcrPages);
        prefetchPages = Math.max(0, prefetchPages);
    }
}
