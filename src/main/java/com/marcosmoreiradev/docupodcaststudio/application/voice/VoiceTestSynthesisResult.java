package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** Result of calling the real voice-test synthesis port. */
public record VoiceTestSynthesisResult(
        boolean generated,
        boolean requiresConfiguration,
        String userMessage,
        long outputBytes,
        String diagnosticTail
) {
    public VoiceTestSynthesisResult {
        userMessage = userMessage == null ? "" : userMessage.strip();
        diagnosticTail = diagnosticTail == null ? "" : diagnosticTail.strip();
        outputBytes = Math.max(0L, outputBytes);
    }

    public static VoiceTestSynthesisResult generated(long outputBytes, String message, String diagnosticTail) {
        return new VoiceTestSynthesisResult(true, false, message, outputBytes, diagnosticTail);
    }

    public static VoiceTestSynthesisResult blocked(String message, boolean requiresConfiguration) {
        return new VoiceTestSynthesisResult(false, requiresConfiguration, message, 0L, "");
    }

    public static VoiceTestSynthesisResult failed(String message, String diagnosticTail) {
        return new VoiceTestSynthesisResult(false, false, message, 0L, diagnosticTail);
    }
}
