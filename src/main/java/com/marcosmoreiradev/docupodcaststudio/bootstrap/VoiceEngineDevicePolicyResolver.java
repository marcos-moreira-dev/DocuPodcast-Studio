package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceArgumentMapper;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.settings.PropertiesOperationalSettingsRepository;

import java.io.IOException;

/** Resolves the persisted general compute priority into the XTTS process argument. */
public final class VoiceEngineDevicePolicyResolver {
    public String resolve() {
        OperationalSettings settings = loadSettings();
        if (!settings.compute().allowGpuForTts()) return "cpu";
        String requested = ComputeDeviceArgumentMapper.toProcessDeviceArgument(
                settings.compute().policy(), settings.compute().selectedDeviceId());
        if (requested.isBlank()) {
            throw new IllegalStateException(
                    "La política de cómputo exige elegir un dispositivo para Voz IA avanzada.");
        }
        return requested.startsWith("gpu-nvidia")
                ? ComputeDeviceArgumentMapper.toCudaDeviceArgument(requested)
                : requested;
    }

    private static OperationalSettings loadSettings() {
        try {
            return PropertiesOperationalSettingsRepository.defaultRepository().load();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "No se pudo cargar la prioridad de cómputo guardada en Configuración: "
                            + failure.getMessage(), failure);
        }
    }
}
