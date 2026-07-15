package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Single local artifact contract for engines and tools used by DocuPodcast Studio.
 *
 * <p>The contract describes local files, not marketing names. It lets readiness,
 * diagnostics and packaging explain exactly why XTTS, Piper or FFmpeg are complete
 * or incomplete without duplicating hardcoded file lists in multiple screens.</p>
 */
public record ModelArtifactContract(
        String artifactId,
        String displayName,
        String recommendedRoot,
        List<ModelArtifactRequirement> requirements
) {
    public ModelArtifactContract {
        artifactId = normalize(artifactId);
        displayName = normalize(displayName);
        recommendedRoot = normalize(recommendedRoot).replace('\\', '/');
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        if (artifactId.isBlank() || displayName.isBlank() || recommendedRoot.isBlank()) {
            throw new IllegalArgumentException("artifactId, displayName and recommendedRoot are required");
        }
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("artifact contract requires at least one file requirement");
        }
    }

    public static ModelArtifactContract xttsAdvancedVoice() {
        return new ModelArtifactContract(
                "xtts-advanced-voice",
                "Voz IA avanzada",
                "models/tts/xtts",
                List.of(
                        ModelArtifactRequirement.required("configuración", "config.json"),
                        ModelArtifactRequirement.required("pesos del modelo", "model.pth"),
                        ModelArtifactRequirement.required("vocabulario", "vocab.json"),
                        ModelArtifactRequirement.required("hablantes internos", "speakers_xtts.pth"),
                        ModelArtifactRequirement.required("codificador de audio", "dvae.pth"),
                        ModelArtifactRequirement.required("estadísticas de audio", "mel_stats.pth")
                ));
    }

    public static ModelArtifactContract piperLocalVoice() {
        return new ModelArtifactContract(
                "piper-local-voice",
                "Voz local simple",
                "tools/piper",
                List.of(
                        ModelArtifactRequirement.required("ejecutable Piper", "piper.exe"),
                        ModelArtifactRequirement.required("modelo de voz ONNX", "voices/es_ES-sharvard-medium.onnx"),
                        ModelArtifactRequirement.required("metadatos de voz", "voices/es_ES-sharvard-medium.onnx.json")
                ));
    }

    public static ModelArtifactContract ffmpegVideoAudio() {
        return new ModelArtifactContract(
                "ffmpeg-video-audio",
                "Video local / FFmpeg",
                "tools/ffmpeg/bin",
                List.of(
                        ModelArtifactRequirement.required("ffmpeg", "ffmpeg.exe"),
                        ModelArtifactRequirement.optional("ffprobe", "ffprobe.exe")
                ));
    }

    public RuntimeArtifactInspection inspect(Path rootDirectory) {
        Path root = rootDirectory == null ? Path.of(recommendedRoot) : rootDirectory;
        ArrayList<String> present = new ArrayList<>();
        ArrayList<String> missingRequired = new ArrayList<>();
        ArrayList<String> missingOptional = new ArrayList<>();
        for (ModelArtifactRequirement requirement : requirements) {
            boolean found = requirement.candidateRelativePaths().stream()
                    .map(root::resolve)
                    .map(Path::normalize)
                    .anyMatch(Files::isRegularFile);
            String label = requirement.purpose() + " (" + String.join(" o ", requirement.candidateRelativePaths()) + ")";
            if (found) {
                present.add(label);
            } else if (requirement.required()) {
                missingRequired.add(label);
            } else {
                missingOptional.add(label);
            }
        }
        boolean complete = missingRequired.isEmpty();
        String message = complete
                ? displayName + " tiene los archivos locales requeridos."
                : displayName + " requiere archivos locales: " + String.join(", ", missingRequired) + ".";
        return new RuntimeArtifactInspection(artifactId, displayName, root.normalize(), complete,
                present, missingRequired, missingOptional, message);
    }

    public static List<ModelArtifactContract> standardContracts() {
        return List.of(xttsAdvancedVoice(), piperLocalVoice(), ffmpegVideoAudio());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
