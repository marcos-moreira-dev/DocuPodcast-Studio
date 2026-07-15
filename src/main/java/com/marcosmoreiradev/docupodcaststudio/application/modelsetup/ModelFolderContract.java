package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.util.List;

/**
 * Local file contract for a model engine.
 *
 * <p>The contract avoids hardcoded download URLs. It tells the assistant which local files are
 * expected after a user downloads or imports a model folder.</p>
 *
 * <p>Internal engine note: XTTS / Coqui and Piper are technical identifiers for diagnostics and
 * model folder inspection, not labels for the normal user interface.</p>
 */
public record ModelFolderContract(
        String engineId,
        String displayName,
        String recommendedFolder,
        String normalUserPurpose,
        List<ModelRequirement> requirements
) {
    public ModelFolderContract {
        engineId = normalize(engineId);
        displayName = normalize(displayName);
        recommendedFolder = normalize(recommendedFolder);
        normalUserPurpose = normalize(normalUserPurpose);
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        if (engineId.isBlank() || displayName.isBlank() || recommendedFolder.isBlank()) {
            throw new IllegalArgumentException("engine id, display name and folder are required");
        }
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("model contract requires at least one file requirement");
        }
    }

    public static ModelFolderContract xttsHighQuality() {
        return new ModelFolderContract(
                "tts-xtts",
                "Voz IA avanzada — calidad alta",
                "models/tts/xtts",
                "Narración más humana, personajes y voces de referencia autorizadas.",
                List.of(
                        new ModelRequirement("configuración", List.of("config.json")),
                        new ModelRequirement("pesos del modelo", List.of("model.pth")),
                        new ModelRequirement("vocabulario", List.of("vocab.json")),
                        new ModelRequirement("referencias internas de hablantes", List.of("speakers_xtts.pth")),
                        new ModelRequirement("codificador de audio", List.of("dvae.pth")),
                        new ModelRequirement("estadísticas de audio", List.of("mel_stats.pth"))
                )
        );
    }

    public static ModelFolderContract piperLightweight() {
        return new ModelFolderContract(
                "tts-piper",
                "Piper — lectura local intermedia/liviana",
                "models/tts/piper/voices",
                "Lectura general local con bajo consumo como modo intermedio/liviano.",
                List.of(
                        new ModelRequirement("voz ONNX", List.of(".onnx")),
                        new ModelRequirement("metadatos de voz", List.of(".onnx.json"))
                )
        );
    }

    public static ModelFolderContract localTheatreImage() {
        return new ModelFolderContract(
                "image-local-theatre",
                "Imagen IA teatral local",
                "models/image",
                "Generacion local de imagenes por intervencion teatral con referencias visuales y paquetes de contexto.",
                List.of(
                        new ModelRequirement("modelo base de imagen", List.of("safetensors", "ckpt")),
                        new ModelRequirement("workflow local", List.of(".json")),
                        new ModelRequirement("manifiesto de paquete", List.of("model-manifest.json", "manifest.json", "SHA256SUMS.txt"))
                )
        );
    }

    public static List<ModelFolderContract> recommended() {
        return List.of(xttsHighQuality(), piperLightweight(), localTheatreImage());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
