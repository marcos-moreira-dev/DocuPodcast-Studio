package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputeDeviceSelectorT115SourceTest {
    @Test
    void computeDeviceSelectionIsRealAndPropagatedToVoiceEngines() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectComputeEnvironmentUseCase.java");
        String gateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/compute/WindowsComputeDeviceDiscoveryGateway.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String formModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java");
        String tts = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java");

        assertTrue(useCase.contains("ComputeDeviceDiscoveryGateway"));
        assertTrue(useCase.contains("Dispositivo solicitado para voz"));
        assertTrue(gateway.contains("Win32_VideoController"));
        assertTrue(gateway.contains("gpu-nvidia"));
        assertTrue(gateway.contains("gpu-amd"));
        assertTrue(gateway.contains("gpu-intel"));
        assertTrue(settings.contains("Dispositivo para voz"));
        assertTrue(formModel.contains("ComboBox<String> computeSelectedDeviceId"));
        assertFalse(formModel.contains("TextField computeSelectedDeviceId"));
        assertTrue(tts.contains("ComputeDeviceArgumentMapper.toProcessDeviceArgument"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
