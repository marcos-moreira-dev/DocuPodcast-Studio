package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Safe maintenance surface for XTTS runtime, model, speaker and real unit/batch checks. */
final class XttsEngineAdministration extends AbstractLocalEngineAdministration {
    private static final EngineActionId PREPARE_RUNTIME = new EngineActionId("prepare-runtime");
    private static final EngineActionId IMPORT_MODEL = new EngineActionId("import-model");
    private static final EngineActionId PREPARE_CUDA = new EngineActionId("prepare-cuda");
    private static final EngineActionId BATCH_SMOKE = new EngineActionId("batch-smoke-test");
    private final VoiceSynthesisEngine voice;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    XttsEngineAdministration(VoiceSynthesisEngine voice, RuntimeAssetCatalog assets) {
        super(voice, assets, List.of(
                action(PREPARE_RUNTIME, "Preparar runtime XTTS",
                        "Importa un runtime XTTS local al destino administrado.", true,
                        directory("runtimeDirectory", "Directorio del runtime XTTS", true)),
                action(IMPORT_MODEL, "Importar modelo y muestra XTTS",
                        "Importa el modelo y una muestra de voz a destinos fijos.", true,
                        directory("modelDirectory", "Directorio del modelo", true),
                        file("speakerFile", "Muestra de voz WAV", true)),
                action(PREPARE_CUDA, "Preparar CUDA para XTTS",
                        "Ejecuta el preparador CUDA incluido sobre el Python local administrado.", true),
                action(EngineActionId.REPAIR, "Reparar XTTS",
                        "Limpia exclusivamente staging incompleto de XTTS.", true),
                action(EngineActionId.SMOKE_TEST, "Probar unidad XTTS",
                        "Genera una unidad WAV real con el modelo local.", false),
                action(BATCH_SMOKE, "Probar lote XTTS",
                        "Genera dos unidades en un unico lote real.", false)));
        this.voice = voice;
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (PREPARE_RUNTIME.equals(request.actionId())) {
            runtime.importDirectory(inputPath(request, "runtimeDirectory"), "tools/xtts-wrapper", context);
        } else if (IMPORT_MODEL.equals(request.actionId())) {
            runtime.importDirectory(inputPath(request, "modelDirectory"), "models/tts/xtts", context);
            runtime.importFile(inputPath(request, "speakerFile"),
                    "models/tts/xtts/speakers/voz-por-defecto.wav", context);
        } else if (PREPARE_CUDA.equals(request.actionId())) {
            Path python = assets.require(XttsVoiceEngine.ID, "python");
            if (!Files.isRegularFile(python)) {
                throw new IOException("Falta el Python XTTS administrado.");
            }
            var result = executor.run(List.of(python.toString(), "-m", "pip", "install", "torch", "torchvision",
                    "torchaudio", "--index-url", "https://download.pytorch.org/whl/cu121"),
                    runtime.runtimeRoot(), context);
            if (result.exitCode() != 0) throw new IOException("La preparacion CUDA fallo: " + tail(result.output()));
            var probe = executor.run(List.of(python.toString(), "-c",
                    "import torch,sys; print(torch.cuda.is_available()); sys.exit(0 if torch.cuda.is_available() else 2)"),
                    runtime.runtimeRoot(), context);
            if (probe.exitCode() != 0) throw new IOException("CUDA se instalo, pero el runtime no detecta una GPU compatible.");
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            ManagedPythonEnvironmentRepair.repair(
                    assets.root(), assets.require(XttsVoiceEngine.ID, "python"), "XTTS");
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/xtts/xtts-unit-smoke.wav");
            Files.createDirectories(output.getParent());
            Path referenceSample = optionalReferenceSample(request);
            voice.synthesize(new VoiceSynthesisRequest("Prueba local de voz avanzada.", "es", "",
                    referenceSample, output, Map.of()), context);
            return List.of(artifact(output, "unit"));
        } else if (BATCH_SMOKE.equals(request.actionId())) {
            Path directory = runtime.target("diagnostics/xtts");
            Files.createDirectories(directory);
            List<String> texts = List.of(
                    "Frase sin puntuacion",
                    "Frase con punto final.",
                    "Primera oracion. Segunda oracion.",
                    "Una espera... y continuamos.",
                    "El valor decimal es 3.14.",
                    "Este texto contiene literalmente pausa breve.");
            List<VoiceSynthesisUnit> units = java.util.stream.IntStream.range(0, texts.size())
                    .mapToObj(index -> new VoiceSynthesisUnit("xtts-audit-" + (index + 1), texts.get(index),
                            "", "", null, directory.resolve("xtts-audit-" + (index + 1) + ".wav"),
                            Map.of("auditText", texts.get(index))))
                    .toList();
            VoiceSynthesisBatchRequest batch = new VoiceSynthesisBatchRequest(units, "es",
                    Map.of("purpose", "punctuation-audit", "applicationAddedWords", "false"));
            voice.synthesizeBatch(batch, context);
            return units.stream().map(unit -> artifact(unit.outputFile(), "batch-punctuation-audit")).toList();
        }
        return List.of();
    }

    private static Path optionalReferenceSample(EngineActionRequest request) throws IOException {
        String configured = request.inputs().getOrDefault("speakerFile", "").strip();
        if (configured.isBlank()) return null;
        Path sample = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(sample)) {
            throw new IOException("La muestra oficial solicitada para la prueba XTTS no existe: " + sample);
        }
        return sample;
    }

    private static GenerationArtifact artifact(Path output, String mode) {
        return new GenerationArtifact("audio", output.toUri(), Map.of("purpose", "smoke", "mode", mode));
    }

    private static String tail(String output) {
        String value = output == null ? "" : output.strip();
        return value.length() <= 1000 ? value : value.substring(value.length() - 1000);
    }
}
