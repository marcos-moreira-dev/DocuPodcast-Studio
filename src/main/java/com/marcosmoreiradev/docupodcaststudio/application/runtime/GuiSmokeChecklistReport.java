package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Assisted smoke checklist for validating the real GUI workflow before a personal release candidate. */
public record GuiSmokeChecklistReport(
        Path applicationRoot,
        Instant generatedAt,
        boolean voiceEngineReady,
        boolean ffmpegReady,
        List<GuiSmokeStep> steps,
        String userMessage
) {
    public GuiSmokeChecklistReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        steps = List.copyOf(steps == null ? List.of() : steps);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean readyForManualGuiSmoke() {
        return voiceEngineReady && steps.stream().allMatch(GuiSmokeStep::readyOrManual);
    }

    public String compactSummary() {
        long ready = steps.stream().filter(step -> "READY".equals(step.status())).count();
        long manual = steps.stream().filter(step -> "MANUAL".equals(step.status())).count();
        long blocked = steps.stream().filter(step -> "BLOCKED".equals(step.status())).count();
        return "Smoke GUI: " + ready + " listo(s), " + manual + " manual(es), " + blocked + " bloqueado(s).";
    }
}
