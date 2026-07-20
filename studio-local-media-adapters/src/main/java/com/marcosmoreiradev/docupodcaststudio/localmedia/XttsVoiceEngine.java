package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XttsVoiceEngine extends AbstractProcessVoiceEngine {
    public static final EngineId ID = new EngineId("xtts");
    private final EngineConfiguration configuration;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    public XttsVoiceEngine(EngineConfiguration configuration) {
        super(ID, "Voz IA avanzada",
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE, EngineFeature.BATCH),
                configuration);
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
    }

    @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                               ExecutionContext context)
            throws IOException, InterruptedException {
        String template = configuration.value("batchCommandTemplate");
        if (template.isBlank() || request.units().size() == 1) return super.synthesizeBatch(request, context);
        ExecutionContext current = context == null ? ExecutionContext.defaults("voice-batch") : context;
        Path parent = request.units().getFirst().outputFile().toAbsolutePath().normalize().getParent();
        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, ".voice-batch-").toAbsolutePath().normalize();
        try {
            Path manifest = staging.resolve("batch.json");
            Files.writeString(manifest, manifest(request, staging), StandardCharsets.UTF_8);
            String command = template.replace("{manifestFile}", manifest.toString())
                    .replace("{language}", request.language());
            current.progress().report("SYNTHESIZING", 0.1, "Cargando una vez el modelo para el lote de voz.");
            LocalProcessExecutor.Result executed = executor.run(new ArrayList<>(CommandLineTokenizer.split(command)),
                    staging, current);
            if (executed.exitCode() != 0) throw new IOException("El lote de voz terminó con código "
                    + executed.exitCode() + ": " + tail(executed.output()));
            ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
            int completed = 0;
            for (VoiceSynthesisUnit unit : request.units()) {
                if (!Files.isRegularFile(unit.outputFile()) || Files.size(unit.outputFile()) == 0) {
                    throw new IOException("El lote no produjo la unidad " + unit.id());
                }
                results.add(new VoiceSynthesisResult(unit.outputFile(), 0,
                        Map.of("engineId", ID.value(), "batch", "true")));
                current.progress().report("voice-unit", ++completed / (double) request.units().size(), unit.id());
            }
            return new VoiceSynthesisBatchResult(results, Map.of("mode", "optimized-batch", "engineId", ID.value()));
        } finally {
            deleteStaging(staging, parent);
        }
    }

    private String manifest(VoiceSynthesisBatchRequest request, Path staging) throws IOException {
        String defaultSpeaker = configuration.value("defaultSpeaker");
        StringBuilder json = new StringBuilder("{\"segments\":[");
        int index = 0;
        for (VoiceSynthesisUnit unit : request.units()) {
            if (index > 0) json.append(',');
            Path text = staging.resolve(String.format("unit-%04d.txt", ++index));
            Files.writeString(text, unit.text(), StandardCharsets.UTF_8);
            Path speaker = unit.referenceAudio() == null
                    ? (defaultSpeaker.isBlank() ? null : Path.of(defaultSpeaker).toAbsolutePath().normalize())
                    : unit.referenceAudio();
            if (speaker == null || !Files.isRegularFile(speaker)) {
                throw new IOException("Falta la referencia de voz para " + unit.id());
            }
            json.append("{\"segmentId\":\"").append(escape(unit.id()))
                    .append("\",\"textFile\":\"").append(escape(text.toString()))
                    .append("\",\"outputFile\":\"").append(escape(unit.outputFile().toString()))
                    .append("\",\"speakerWav\":\"").append(escape(speaker.toString()))
                    .append("\",\"language\":\"").append(escape(request.language())).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static void deleteStaging(Path root, Path parent) {
        if (root == null || parent == null || !root.startsWith(parent)
                || !root.getFileName().toString().startsWith(".voice-batch-") || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String tail(String value) {
        String text = value == null ? "" : value.strip();
        return text.length() <= 1200 ? text : text.substring(text.length() - 1200);
    }
}
