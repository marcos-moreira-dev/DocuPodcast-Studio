package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PiperVoiceEngine extends AbstractProcessVoiceEngine {
    public static final EngineId ID = new EngineId("piper");
    private final EngineConfiguration configuration;

    public PiperVoiceEngine(EngineConfiguration configuration) {
        super(ID, "Piper · voz local simple", Set.of(EngineFeature.PACKAGED_VOICE), configuration);
        this.configuration = configuration == null
                ? new EngineConfiguration(ID, Map.of()) : configuration;
    }

    @Override
    public String prepareText(String normalizedText) {
        String value = (normalizedText == null ? "" : normalizedText.strip())
                .replace('ñ', 'n').replace('Ñ', 'N')
                .replace('¿', '?').replace('¡', '!');
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    @Override
    public ComputeResourceDemand resourceDemand(ComputePreference preference) {
        final long mib = 1024L * 1024L;
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                192L * mib, 0L, true, ComputeDeviceId.CPU_0,
                0, null, EncoderResourceDemand.none());
    }

    @Override
    public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        EngineReadiness command = super.inspectReadiness(ignored);
        if (!command.ready()) return command;
        List<String> missing = new ArrayList<>();
        requireFile("script", "script de sintesis Piper", missing);
        requireFile("executable", "ejecutable Piper", missing);
        requireFile("model", "modelo de voz Piper", missing);
        requireFile("metadata", "metadatos del modelo Piper", missing);
        if (!missing.isEmpty()) {
            return EngineReadiness.unavailable(ID,
                    "Voz local simple no esta lista: faltan artefactos de Piper.",
                    "Restaura o importa: " + String.join(", ", missing) + ".");
        }
        return EngineReadiness.ready(ID,
                "Voz local simple lista con Piper, modelo y metadatos verificados.");
    }

    private void requireFile(String key, String label, List<String> missing) {
        String configured = configuration.value(key);
        if (configured.isBlank()) {
            missing.add(label + " (ruta no configurada)");
            return;
        }
        try {
            if (!Files.isRegularFile(Path.of(configured))) {
                missing.add(label + " (" + configured + ")");
            }
        } catch (InvalidPathException invalid) {
            missing.add(label + " (ruta invalida)");
        }
    }
}
