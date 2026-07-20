package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class AbstractProcessVoiceEngine implements VoiceSynthesisEngine {
    private final EngineDescriptor descriptor;
    private final EngineConfiguration configuration;
    private final EngineConfigurationSchema schema;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    AbstractProcessVoiceEngine(EngineId id, String name, Set<EngineFeature> features,
                               EngineConfiguration configuration) {
        this.descriptor = new EngineDescriptor(id, CapabilityId.VOICE_SYNTHESIS, name, "local",
                "external-process", features, false);
        this.configuration = configuration == null ? new EngineConfiguration(id, Map.of()) : configuration;
        this.schema = new EngineConfigurationSchema(id, List.of(
                new EngineConfigurationField("commandTemplate", "Comando", "Plantilla con {textFile} y {outputFile}.",
                        ConfigurationFieldType.TEXT, true, "")));
    }

    @Override public EngineDescriptor descriptor() { return descriptor; }
    @Override public EngineConfigurationSchema configurationSchema() { return schema; }

    @Override
    public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        String command = configuration.value("commandTemplate");
        if (command.isBlank() || !command.contains("{textFile}") || !command.contains("{outputFile}")) {
            return EngineReadiness.unavailable(descriptor.id(),
                    "El comando del motor de voz no está configurado.",
                    "Configura una plantilla con {textFile} y {outputFile}.");
        }
        return EngineReadiness.ready(descriptor.id(), descriptor.displayName() + " configurado.");
    }

    @Override
    public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        EngineReadiness readiness = inspectReadiness(configuration);
        if (!readiness.ready()) throw new IOException(readiness.summary());
        ExecutionContext current = context == null ? ExecutionContext.defaults("voice-synthesis") : context;
        Path output = request.outputFile().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        Path textFile = Files.createTempFile(output.getParent(), "voice-input-", ".txt");
        Files.writeString(textFile, request.text(), StandardCharsets.UTF_8);
        try {
            String command = configuration.value("commandTemplate")
                    .replace("{textFile}", textFile.toString())
                    .replace("{outputFile}", output.toString())
                    .replace("{language}", request.language())
                    .replace("{voice}", request.voiceId())
                    .replace("{referenceAudio}", request.referenceAudio() == null ? "" : request.referenceAudio().toString());
            current.progress().report("SYNTHESIZING", 0.1, "Generando voz con " + descriptor.displayName() + ".");
            LocalProcessExecutor.Result result = executor.run(new ArrayList<>(CommandLineTokenizer.split(command)),
                    output.getParent(), current);
            if (result.exitCode() != 0) throw new IOException(descriptor.displayName() + " terminó con código "
                    + result.exitCode() + ": " + tail(result.output()));
            if (!Files.isRegularFile(output) || Files.size(output) == 0) {
                throw new IOException(descriptor.displayName() + " no produjo un archivo de audio.");
            }
            current.progress().report("COMPLETED", 1.0, "Voz generada.");
            return new VoiceSynthesisResult(output, 0.0,
                    Map.of("engineId", descriptor.id().value(), "output", tail(result.output())));
        } finally {
            Files.deleteIfExists(textFile);
        }
    }

    private static String tail(String value) {
        String text = value == null ? "" : value.strip();
        return text.length() <= 1200 ? text : text.substring(text.length() - 1200);
    }
}
