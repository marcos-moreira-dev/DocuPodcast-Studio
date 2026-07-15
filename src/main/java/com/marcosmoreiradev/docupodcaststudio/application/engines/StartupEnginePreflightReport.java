package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.util.Comparator;
import java.util.List;

/** Startup report used before exposing voice/media workflows to the user. */
public record StartupEnginePreflightReport(List<StartupEnginePreflightItem> items) {
    public StartupEnginePreflightReport {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public boolean listeningReady() {
        return items.stream()
                .filter(StartupEnginePreflightItem::requiredForListening)
                .allMatch(StartupEnginePreflightItem::ready);
    }

    public List<StartupEnginePreflightItem> missingRequiredItems() {
        return items.stream()
                .filter(StartupEnginePreflightItem::requiredForListening)
                .filter(StartupEnginePreflightItem::missing)
                .sorted(Comparator.comparing(StartupEnginePreflightItem::itemId))
                .toList();
    }

    public String summary() {
        long ready = items.stream().filter(StartupEnginePreflightItem::ready).count();
        long total = items.size();
        return "Arranque de motores: " + ready + "/" + total + " elementos listos. "
                + (listeningReady() ? "Voz IA lista." : "Falta preparar voz IA.");
    }
}
