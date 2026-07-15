package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.util.Comparator;
import java.util.List;

/** Report for repo-local Piper/FFmpeg artifacts used by smoke and future setup UI. */
public record LocalToolReadinessReport(List<LocalToolReadinessItem> items) {
    public LocalToolReadinessReport {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public boolean piperReady() {
        return ready("piper-exe") && ready("piper-model");
    }

    public boolean ffmpegReady() {
        return ready("ffmpeg-exe");
    }

    public boolean fullLocalToolsReady() {
        return piperReady() && ffmpegReady();
    }

    public List<LocalToolReadinessItem> missingRequiredItems() {
        return items.stream()
                .filter(LocalToolReadinessItem::requiredForProductSmoke)
                .filter(LocalToolReadinessItem::missing)
                .sorted(Comparator.comparing(LocalToolReadinessItem::itemId))
                .toList();
    }

    public String summary() {
        long ready = items.stream().filter(LocalToolReadinessItem::ready).count();
        return "Herramientas locales Piper/FFmpeg: " + ready + "/" + items.size()
                + " listas. Piper=" + yesNo(piperReady())
                + " · FFmpeg=" + yesNo(ffmpegReady()) + ".";
    }

    private boolean ready(String itemId) {
        return items.stream()
                .filter(item -> itemId.equals(item.itemId()))
                .findFirst()
                .map(LocalToolReadinessItem::ready)
                .orElse(false);
    }

    private static String yesNo(boolean value) {
        return value ? "sí" : "no";
    }
}
