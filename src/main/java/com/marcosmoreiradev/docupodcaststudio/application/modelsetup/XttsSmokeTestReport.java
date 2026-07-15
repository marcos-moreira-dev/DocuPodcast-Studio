package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Proof artifact for the advanced AI voice engine.
 *
 * <p>Model/download readiness is not the same as real synthesis readiness. This report tracks whether
 * the app generated a real WAV through the local engine, and whether the user later confirmed playback
 * from the app. Playback confirmation can be added by the UI; generation proof is already enough to
 * distinguish "downloaded" from "actually executed".</p>
 */
public record XttsSmokeTestReport(
        Path smokeDirectory,
        Path textFile,
        Path audioFile,
        Path manifestFile,
        boolean generated,
        boolean playbackConfirmed,
        long outputBytes,
        Instant generatedAt,
        List<String> issues,
        String userMessage
) {
    public XttsSmokeTestReport {
        outputBytes = Math.max(0L, outputBytes);
        issues = List.copyOf(issues == null ? List.of() : issues);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean generatedWavProof() {
        return generated && outputBytes > 44L;
    }

    public boolean fullyVerified() {
        return generatedWavProof() && playbackConfirmed;
    }

    public boolean requiresUserPlayback() {
        return generatedWavProof() && !playbackConfirmed;
    }

    public static XttsSmokeTestReport pending(Path smokeDirectory, String message) {
        Path dir = smokeDirectory == null ? Path.of("runtime/tts/xtts-smoke") : smokeDirectory;
        return new XttsSmokeTestReport(dir, dir.resolve("xtts-readiness-smoke.txt"),
                dir.resolve("xtts-readiness-smoke.wav"), dir.resolve("xtts-readiness-smoke.json"),
                false, false, 0L, null, List.of(message), message);
    }

    public static XttsSmokeTestReport failed(Path smokeDirectory, String message, List<String> issues) {
        Path dir = smokeDirectory == null ? Path.of("runtime/tts/xtts-smoke") : smokeDirectory;
        return new XttsSmokeTestReport(dir, dir.resolve("xtts-readiness-smoke.txt"),
                dir.resolve("xtts-readiness-smoke.wav"), dir.resolve("xtts-readiness-smoke.json"),
                false, false, 0L, null, issues, message);
    }
}
