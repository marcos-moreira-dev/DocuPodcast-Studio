package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.util.Comparator;
import java.util.List;

/** Aggregate report for Settings/Diagnostics before enabling real voice/media workflows. */
public record AiEnginePreflightReport(List<AiEnginePreflightItem> items) {
    public AiEnginePreflightReport {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public boolean coquiMandatoryButReadyOrWarnOnly() {
        return items.stream()
                .filter(item -> "tts-xtts-coqui".equals(item.engineId()))
                .findFirst()
                .map(AiEnginePreflightItem::usable)
                .orElse(false);
    }

    public boolean piperReady() {
        return usable("tts-piper");
    }


    public boolean ffmpegReady() {
        return usable("ffmpeg");
    }

    public boolean ocrReady() {
        return usable("ocr-tesseract");
    }

    public boolean minimalVoiceDemoReady() {
        return coquiMandatoryButReadyOrWarnOnly() || piperReady();
    }

    public boolean fullAiDemoReady() {
        return minimalVoiceDemoReady() && ffmpegReady();
    }

    public List<AiEnginePreflightItem> needsConfiguration() {
        return items.stream()
                .filter(AiEnginePreflightItem::needsWork)
                .sorted(Comparator.comparing(AiEnginePreflightItem::mandatoryForTargetProduct).reversed()
                        .thenComparing(AiEnginePreflightItem::engineId))
                .toList();
    }

    public String summary() {
        long ready = items.stream().filter(AiEnginePreflightItem::usable).count();
        long pending = items.size() - ready;
        return "Motores voz/media: " + ready + " listos, " + pending + " pendientes. "
                + "Voz=" + yesNo(minimalVoiceDemoReady())
                + " OCR=" + yesNo(ocrReady())
                + " · Media=" + yesNo(ffmpegReady()) + ".";
    }

    private boolean usable(String engineId) {
        return items.stream()
                .filter(item -> engineId.equals(item.engineId()))
                .findFirst()
                .map(AiEnginePreflightItem::usable)
                .orElse(false);
    }

    private static String yesNo(boolean value) {
        return value ? "sí" : "no";
    }
}
