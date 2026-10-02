package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Safe maintenance surface for the bundled Piper adapter. */
final class PiperEngineAdministration extends AbstractLocalEngineAdministration {
    private static final EngineActionId VERIFY = new EngineActionId("verify");
    private final VoiceSynthesisEngine voice;

    PiperEngineAdministration(VoiceSynthesisEngine voice, RuntimeAssetCatalog assets) {
        super(voice, assets, List.of(
                action(EngineActionId.IMPORT, "Importar voz Piper",
                        "Importa un modelo y su configuracion usando los destinos seguros de Piper.", true,
                        file("modelFile", "Modelo .onnx", true), file("configFile", "Configuracion .json", true)),
                action(EngineActionId.REPAIR, "Reparar Piper",
                        "Limpia exclusivamente el staging administrado de Piper.", true),
                action(VERIFY, "Verificar Piper", "Comprueba la disponibilidad declarada del adaptador.", false),
                action(EngineActionId.SMOKE_TEST, "Generar prueba Piper",
                        "Genera un WAV real y corto dentro del runtime de diagnostico.", false)));
        this.voice = voice;
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.IMPORT.equals(request.actionId())) {
            runtime.importFile(inputPath(request, "modelFile"),
                    "models/tts/piper/voices/es_ES-default-medium.onnx", context);
            runtime.importFile(inputPath(request, "configFile"),
                    "models/tts/piper/voices/es_ES-default-medium.onnx.json", context);
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/piper/piper-smoke.wav");
            Files.createDirectories(output.getParent());
            voice.synthesize(new VoiceSynthesisRequest("Prueba local de voz.", "es", "", null, output, Map.of()), context);
            return List.of(new GenerationArtifact("audio", output.toUri(), Map.of("purpose", "smoke")));
        }
        return List.of();
    }
}
