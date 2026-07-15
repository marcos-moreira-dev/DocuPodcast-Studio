package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Builds an auditable voice-sample report for portable project bundle exports. */
public final class BuildVoiceReferenceSamplesExportReportUseCase {
    public VoiceReferenceSamplesExportReport build(DocuPodcastProject project, Path projectFile) {
        Objects.requireNonNull(project, "project");
        Path projectDirectory = projectDirectory(projectFile);
        StringBuilder markdown = new StringBuilder();
        StringBuilder tsv = new StringBuilder("voice_id\tvoice_name\ttone\tsample_id\trelative_path\tasset_registered\tfile_exists\townership\torigin\n");
        Map<String, VoiceProfile> voicesById = new LinkedHashMap<>();
        for (VoiceProfile voice : project.voiceLibrary().voices()) {
            voicesById.put(voice.id(), voice);
        }

        int sampleCount = 0;
        int registeredAssetCount = 0;
        int missingFileCount = 0;
        markdown.append("# Muestras de voz por tono\n\n");
        markdown.append("Este reporte audita las muestras de voz registradas por tono dentro del proyecto exportado. ")
                .append("La muestra neutral es la referencia base; los tonos faltantes pueden usar fallback neutral según la configuración del documento.\n\n");
        markdown.append("| Voz | Tono | Muestra | Ruta | Asset | Archivo |\n");
        markdown.append("|---|---|---|---|---|---|\n");

        for (VoiceReferenceSampleSet sampleSet : project.voiceLibrary().referenceSampleSets()) {
            VoiceProfile voice = voicesById.get(sampleSet.voiceProfileId());
            String voiceName = voice == null ? sampleSet.voiceProfileId() : voice.displayName();
            for (VoiceReferenceSample sample : sampleSet.samples()) {
                sampleCount++;
                ProjectAssetReference asset = project.assets().byId(sample.id()).orElse(null);
                boolean assetRegistered = asset != null && asset.kind() == ProjectAssetKind.VOICE_SAMPLE;
                if (assetRegistered) {
                    registeredAssetCount++;
                }
                boolean fileExists = projectDirectory != null && fileExists(projectDirectory, sample.fileUri());
                if (!fileExists) {
                    missingFileCount++;
                }
                markdown.append("| ").append(escapeTable(voiceName)).append(" | ")
                        .append(escapeTable(sample.tone().displayName())).append(" | `")
                        .append(sample.id()).append("` | `")
                        .append(sample.fileUri()).append("` | ")
                        .append(assetRegistered ? "registrado" : "faltante")
                        .append(" | ")
                        .append(fileExists ? "presente" : "no encontrado")
                        .append(" |\n");
                tsv.append(safeTsv(sample.voiceProfileId())).append('\t')
                        .append(safeTsv(voiceName)).append('\t')
                        .append(safeTsv(sample.tone().name())).append('\t')
                        .append(safeTsv(sample.id())).append('\t')
                        .append(safeTsv(sample.fileUri())).append('\t')
                        .append(assetRegistered).append('\t')
                        .append(fileExists).append('\t')
                        .append(safeTsv(sample.ownership().name())).append('\t')
                        .append(safeTsv(sample.origin().name())).append('\n');
            }
        }
        if (sampleCount == 0) {
            markdown.append("| Sin muestras registradas | - | - | - | - | - |\n");
        }
        int generatedTestFileCount = countGeneratedTestFiles(projectDirectory);
        markdown.append("\n## Resumen\n\n")
                .append("- Voces: ").append(project.voiceLibrary().voices().size()).append("\n")
                .append("- Sets de muestras por tono: ").append(project.voiceLibrary().referenceSampleSets().size()).append("\n")
                .append("- Muestras registradas: ").append(sampleCount).append("\n")
                .append("- Muestras con asset registrado: ").append(registeredAssetCount).append("\n")
                .append("- Muestras con archivo faltante: ").append(missingFileCount).append("\n")
                .append("- Archivos de pruebas generadas: ").append(generatedTestFileCount).append("\n");
        return new VoiceReferenceSamplesExportReport(
                project.voiceLibrary().voices().size(),
                project.voiceLibrary().referenceSampleSets().size(),
                sampleCount,
                registeredAssetCount,
                missingFileCount,
                generatedTestFileCount,
                markdown.toString(),
                tsv.toString()
        );
    }

    private static Path projectDirectory(Path projectFile) {
        if (projectFile == null) {
            return null;
        }
        return projectFile.toAbsolutePath().normalize().getParent();
    }

    private static boolean fileExists(Path projectDirectory, String relativePath) {
        try {
            Path resolved = safeResolve(projectDirectory, relativePath);
            return Files.isRegularFile(resolved);
        } catch (IOException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static int countGeneratedTestFiles(Path projectDirectory) {
        if (projectDirectory == null) {
            return 0;
        }
        Path generated = projectDirectory.resolve("voices").resolve("generated-tests").normalize();
        if (!generated.startsWith(projectDirectory) || !Files.exists(generated)) {
            return 0;
        }
        try (var stream = Files.walk(generated)) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (IOException ex) {
            return 0;
        }
    }

    private static Path safeResolve(Path root, String relativePath) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        String normalizedRelative = relativePath == null ? "" : relativePath.replace('\\', '/').strip();
        if (normalizedRelative.isBlank() || normalizedRelative.startsWith("/") || normalizedRelative.contains("://")) {
            throw new IOException("Invalid voice sample path");
        }
        Path resolved = normalizedRoot.resolve(normalizedRelative).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Voice sample path outside project");
        }
        return resolved;
    }

    private static String escapeTable(String value) {
        return (value == null ? "" : value).replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    private static String safeTsv(String value) {
        return (value == null ? "" : value).replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
