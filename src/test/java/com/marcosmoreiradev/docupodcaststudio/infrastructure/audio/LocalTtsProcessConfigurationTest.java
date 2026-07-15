package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.errors.EngineUnavailableException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTtsProcessConfigurationTest {
    @Test
    void readsCommandFromPropertiesBeforeEnvironment() {
        Properties properties = new Properties();
        properties.setProperty(LocalTtsProcessConfiguration.PROPERTY_COMMAND, "prop-engine --text {textFile} --out {outputFile}");
        LocalTtsProcessConfiguration configuration = LocalTtsProcessConfiguration.from(properties,
                Map.of(LocalTtsProcessConfiguration.ENV_COMMAND, "env-engine"));

        assertTrue(configuration.enabled());
        assertEquals("prop-engine --text {textFile} --out {outputFile}", configuration.commandTemplate());
    }

    @Test
    void disabledWhenNoCommandIsConfigured() {
        LocalTtsProcessConfiguration configuration = LocalTtsProcessConfiguration.from(new Properties(), Map.of());

        assertFalse(configuration.enabled());
        assertFalse(configuration.descriptor().configured());
    }

    @Test
    void tokenizesQuotedCommandAndAppliesPlaceholders() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "tts-worker --text-file \"{textFile}\" --output-file '{outputFile}' --lang {language} --voice {voice}",
                "XTTS Worker",
                "es",
                "VOC-001",
                30
        );

        var command = configuration.commandFor("SEG-001", Path.of("segments/SEG-001.txt"), Path.of("audio/SEG-001.wav"), "es", "VOC-ANA");

        assertEquals("tts-worker", command.get(0));
        assertEquals("segments/SEG-001.txt", command.get(2).replace('\\', '/'));
        assertEquals("audio/SEG-001.wav", command.get(4).replace('\\', '/'));
        assertEquals("VOC-ANA", command.get(8));
    }

    @Test
    void exposesComputePlaceholdersForExternalWrappers() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "tts --raw-device {computeDevice} --device {device} --policy {computePolicy} --gpu {gpuIndex} --text {textFile} --out {outputFile}",
                "XTTS Worker",
                "es",
                "VOC-001",
                30,
                1,
                ComputeDevicePolicy.SPECIFIC_DEVICE,
                "gpu-intel-0"
        );

        var command = configuration.commandFor("SEG-001", Path.of("segment.txt"), Path.of("out.wav"), "es", "VOC-ANA");

        assertEquals("gpu-intel-0", command.get(2));
        assertEquals("gpu-intel-0", command.get(4));
        assertEquals("SPECIFIC_DEVICE", command.get(6));
        assertEquals("0", command.get(8));
    }

    @Test
    void blocksLegacyModelDirectoryBeforeProcessExecution() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "powershell -File scripts/tts/Voz IA avanzada-file-to-wav.ps1 -ModelDir \"C:/old/recursos locales IA avanzada/model.pth/model.pth\" -Text {textFile} -Output {outputFile}",
                "Voz IA avanzada",
                "es",
                "VOC-001",
                30
        );

        assertThrows(EngineUnavailableException.class,
                () -> configuration.commandFor("SEG-001", Path.of("segment.txt"), Path.of("out.wav"), "es", "VOC-ANA"));
    }

    @Test
    void blocksLegacyWrapperBeforeProcessExecution() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "powershell -File \"C:/old/componentes locales IA avanzada-wrapper/run.ps1\" -Text {textFile} -Output {outputFile}",
                "Voz IA avanzada",
                "es",
                "VOC-001",
                30
        );

        assertThrows(EngineUnavailableException.class,
                () -> configuration.commandFor("SEG-001", Path.of("segment.txt"), Path.of("out.wav"), "es", "VOC-ANA"));
    }
}
