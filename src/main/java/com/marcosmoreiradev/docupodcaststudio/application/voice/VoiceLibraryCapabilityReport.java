package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.util.List;
import java.util.Optional;

/** Capability summary for the voice library under the currently configured audio engine. */
public record VoiceLibraryCapabilityReport(
        String engineLabel,
        List<VoiceProfileCapability> voices,
        List<PerformanceStyleCapability> styles,
        int assignableVoices,
        int synthesizableVoices,
        int referenceVoices,
        int blockedVoices,
        int honoredStyles,
        int roadmapStyles
) {
    public VoiceLibraryCapabilityReport {
        engineLabel = engineLabel == null || engineLabel.isBlank() ? "Motor de audio no disponible" : engineLabel.strip();
        voices = voices == null ? List.of() : List.copyOf(voices);
        styles = styles == null ? List.of() : List.copyOf(styles);
    }

    public static VoiceLibraryCapabilityReport empty(String engineLabel) {
        return new VoiceLibraryCapabilityReport(engineLabel, List.of(), List.of(), 0, 0, 0, 0, 0, 0);
    }

    public Optional<VoiceProfileCapability> voice(String voiceId) {
        String target = normalize(voiceId);
        return voices.stream().filter(voice -> voice.voiceId().equals(target)).findFirst();
    }

    public Optional<PerformanceStyleCapability> style(String styleId) {
        String target = normalize(styleId);
        return styles.stream().filter(style -> style.styleId().equals(target)).findFirst();
    }

    public String readinessLabel() {
        return "Motor: " + engineLabel
                + " · Voces asignables: " + assignableVoices + "/" + voices.size()
                + " · Sintetizables ahora: " + synthesizableVoices
                + " · Referencia humana: " + referenceVoices
                + " · Bloqueadas: " + blockedVoices;
    }

    public List<String> summaryLines() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(readinessLabel());
        lines.add("Estilos honrados por motor: " + honoredStyles + "/" + styles.size() + " · Roadmap/dependientes: " + roadmapStyles);
        voices.stream().map(VoiceProfileCapability::summaryLabel).forEach(lines::add);
        styles.stream().map(PerformanceStyleCapability::summaryLabel).forEach(lines::add);
        return List.copyOf(lines);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
