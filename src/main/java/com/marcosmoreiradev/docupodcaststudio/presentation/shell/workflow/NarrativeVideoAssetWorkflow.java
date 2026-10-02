package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextRole;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeGeneratedClip;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeKeyframeSource;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeGeneratedVideoArtifact;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Atomic project-owned assets and state for narrative-video production. */
public final class NarrativeVideoAssetWorkflow {
    public void update(ProjectSession session, NarrativeProjectLayer narrative) {
        Objects.requireNonNull(session, "session");
        session.replaceProject(session.project().withNarrative(narrative), true);
    }

    public void updateConfiguration(ProjectSession session, NarrativeVideoConfiguration configuration) {
        update(session, session.project().narrative().withVideoConfiguration(configuration));
    }

    public NarrativeContextReference importContextReference(ProjectSession session,
                                                            Path source,
                                                            NarrativeContextRole role) throws IOException {
        ImportedAsset imported = importImage(
                session,
                source,
                "media/images/narrative/context",
                "NARRATIVE-CONTEXT",
                "Referencia global de video narrativo");
        NarrativeContextReference reference = new NarrativeContextReference(
                "NARR-CTX-" + compactId(),
                role,
                imported.asset().id(),
                imported.asset().displayName(),
                true,
                1.0,
                "");
        session.replaceProject(imported.project().withNarrative(
                imported.project().narrative().withContextReference(reference)), true);
        return reference;
    }

    public ProjectAssetReference importKeyframe(ProjectSession session,
                                                String blockId,
                                                Path source) throws IOException {
        String safeBlock = safeToken(blockId, "paragraph");
        ImportedAsset imported = importImage(
                session,
                source,
                "generated/narrative/keyframes",
                "NARRATIVE-KEYFRAME-" + safeBlock,
                "Imagen clave de video narrativo");
        NarrativeParagraphTake previous = imported.project().narrative().takeOrDefault(blockId);
        NarrativeParagraphTake take = previous.withKeyframe(
                imported.asset().id(),
                NarrativeKeyframeSource.IMPORTED,
                previous.prompt(),
                previous.negativePrompt(),
                previous.seed(),
                "");
        session.replaceProject(imported.project().withNarrative(
                imported.project().narrative().withTake(take)), true);
        return imported.asset();
    }

    public ProjectAssetReference commitGeneratedKeyframe(ProjectSession session,
                                                         String blockId,
                                                         Path generatedPng,
                                                         String prompt,
                                                         String negativePrompt,
                                                         long seed,
                                                         String sourceFingerprint) throws IOException {
        ImportedAsset imported = importImage(
                session,
                generatedPng,
                "generated/narrative/keyframes",
                "NARRATIVE-KEYFRAME-" + safeToken(blockId, "paragraph"),
                "Imagen clave generada localmente para video narrativo");
        NarrativeParagraphTake previous = imported.project().narrative().takeOrDefault(blockId);
        NarrativeParagraphTake take = previous.withKeyframe(
                imported.asset().id(),
                NarrativeKeyframeSource.GENERATED,
                prompt,
                negativePrompt,
                seed,
                sourceFingerprint);
        session.replaceProject(imported.project().withNarrative(
                imported.project().narrative().withTake(take)), true);
        return imported.asset();
    }

    public List<NarrativeGeneratedClip> commitGeneratedClips(ProjectSession session,
                                                            String blockId,
                                                            List<NarrativeGeneratedVideoArtifact> results,
                                                            String sourceFingerprint) throws IOException {
        Path projectFile = requireProjectFile(session);
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (results == null || results.isEmpty()) {
            throw new IOException("La generacion no produjo clips para registrar.");
        }
        var project = session.project();
        ArrayList<NarrativeGeneratedClip> clips = new ArrayList<>();
        int order = 0;
        for (NarrativeGeneratedVideoArtifact result : results) {
            Path clipPath = requireProjectOwnedFile(root, result.clipPath(), "clip de video");
            Path lastFramePath = requireProjectOwnedFile(root, result.continuationFramePath(), "ultimo frame");
            String clipAssetId = "NARRATIVE-CLIP-" + safeToken(blockId, "paragraph") + "-" + compactId();
            String lastFrameAssetId = "NARRATIVE-LAST-FRAME-" + safeToken(blockId, "paragraph")
                    + "-" + compactId();
            ProjectAssetReference clipAsset = new ProjectAssetReference(
                    clipAssetId,
                    ProjectAssetKind.VIDEO_SOURCE,
                    clipPath.getFileName().toString(),
                    portable(root, clipPath),
                    "video/mp4",
                    "Clip local image-to-video de Video narrativo",
                    "",
                    "");
            ProjectAssetReference lastFrameAsset = new ProjectAssetReference(
                    lastFrameAssetId,
                    ProjectAssetKind.IMAGE,
                    lastFramePath.getFileName().toString(),
                    portable(root, lastFramePath),
                    "image/png",
                    "Frame final para continuidad de Video narrativo",
                    "",
                    "");
            project = project.withAsset(clipAsset).withAsset(lastFrameAsset);
            clips.add(new NarrativeGeneratedClip(
                    "NARRATIVE-GENERATED-CLIP-" + compactId(),
                    clipAssetId,
                    order++,
                    result.durationSeconds(),
                    lastFrameAssetId,
                    result.engineId().value(),
                    result.presetId().value(),
                    result.seed(),
                    sourceFingerprint,
                    result.metadata() == null ? Map.of() : result.metadata()));
        }
        NarrativeParagraphTake take = project.narrative().takeOrDefault(blockId)
                .withClips(List.copyOf(clips), sourceFingerprint);
        session.replaceProject(project.withNarrative(project.narrative().withTake(take)), true);
        return List.copyOf(clips);
    }

    private ImportedAsset importImage(ProjectSession session,
                                      Path source,
                                      String relativeDirectory,
                                      String idPrefix,
                                      String purpose) throws IOException {
        Path projectFile = requireProjectFile(session);
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        Path normalizedSource = source == null ? null : source.toAbsolutePath().normalize();
        if (normalizedSource == null || !Files.isRegularFile(normalizedSource)) {
            throw new IOException("Selecciona una imagen existente.");
        }
        String extension = extension(normalizedSource);
        if (!isImageExtension(extension)) {
            throw new IOException("El archivo seleccionado no es una imagen compatible.");
        }
        String baseName = safeToken(stripExtension(normalizedSource.getFileName().toString()), "image");
        String suffix = compactId().substring(0, 10);
        Path directory = root.resolve(relativeDirectory).normalize();
        if (!directory.startsWith(root)) {
            throw new IOException("El destino narrativo debe permanecer dentro del proyecto.");
        }
        Path target = directory.resolve(baseName + "-" + suffix + extension);
        Path staging = root.resolve(".narrative-import-" + compactId() + extension);
        try {
            Files.copy(normalizedSource, staging, StandardCopyOption.REPLACE_EXISTING);
            Files.createDirectories(directory);
            move(staging, target);
        } finally {
            Files.deleteIfExists(staging);
        }
        ProjectAssetReference asset = new ProjectAssetReference(
                idPrefix + "-" + compactId(),
                ProjectAssetKind.IMAGE,
                normalizedSource.getFileName().toString(),
                portable(root, target),
                imageMime(extension),
                purpose,
                "",
                "");
        return new ImportedAsset(session.project().withAsset(asset), asset);
    }

    private static Path requireProjectFile(ProjectSession session) throws IOException {
        return Objects.requireNonNull(session, "session").projectFile()
                .orElseThrow(() -> new IOException(
                        "Guarda el proyecto antes de importar recursos de video narrativo."));
    }

    private static Path requireProjectOwnedFile(Path root, Path candidate, String label) throws IOException {
        Path normalized = candidate == null ? null : candidate.toAbsolutePath().normalize();
        if (normalized == null || !normalized.startsWith(root) || !Files.isRegularFile(normalized)) {
            throw new IOException("El " + label + " generado debe existir dentro de la carpeta del proyecto.");
        }
        return normalized;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isImageExtension(String extension) {
        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".webp", ".bmp" -> true;
            default -> false;
        };
    }

    private static String imageMime(String extension) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    private static String extension(Path file) {
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : ".png";
    }

    private static String stripExtension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : Objects.requireNonNullElse(name, "image");
    }

    private static String safeToken(String value, String fallback) {
        String normalized = value == null ? "" : value.strip()
                .replaceAll("[^A-Za-z0-9_-]", "-")
                .replaceAll("-{2,}", "-");
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String portable(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static String compactId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record ImportedAsset(
            com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject project,
            ProjectAssetReference asset) {
    }
}
