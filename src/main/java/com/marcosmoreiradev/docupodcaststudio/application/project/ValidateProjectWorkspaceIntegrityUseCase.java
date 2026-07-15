package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Performs the strong, file-system-aware validation that is needed after opening a .docupodcast project.
 *
 * <p>{@link ValidateProjectPayloadUseCase} validates the declared project kind against the asset catalog. This
 * use case goes one level deeper: it verifies that referenced files exist, that materialized workspace artifacts
 * were actually rehydrated and that cross-artifact references are coherent.</p>
 */
public final class ValidateProjectWorkspaceIntegrityUseCase {
    public ProjectValidationResult validate(DocuPodcastProject project, Path projectFile, ProjectWorkspaceHydration hydration) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        ProjectWorkspaceHydration safeHydration = hydration == null ? ProjectWorkspaceHydration.empty() : hydration;
        List<String> messages = new ArrayList<>();

        Path projectRoot = projectRoot(projectFile, messages);
        if (projectRoot != null) {
            validateAssetFiles(project, projectRoot, messages);
        }
        validateMaterializedArtifacts(project, safeHydration, messages);
        validateStoryboardBindings(project, safeHydration, messages);

        return messages.isEmpty() ? ProjectValidationResult.ok() : ProjectValidationResult.invalid(messages);
    }

    private static Path projectRoot(Path projectFile, List<String> messages) {
        Path normalizedFile = projectFile.toAbsolutePath().normalize();
        Path root = normalizedFile.getParent();
        if (root == null) {
            messages.add("El archivo .docupodcast debe estar dentro de una carpeta de proyecto.");
            return null;
        }
        if (!Files.isDirectory(root)) {
            messages.add("La carpeta del proyecto no existe: " + root + ".");
            return null;
        }
        return root;
    }

    private static void validateAssetFiles(DocuPodcastProject project, Path projectRoot, List<String> messages) {
        for (ProjectAssetReference asset : project.assets().references()) {
            Path resolved = projectRoot.resolve(asset.relativePath()).normalize();
            if (!resolved.startsWith(projectRoot)) {
                messages.add("Asset " + asset.id() + " sale de la carpeta del proyecto: " + asset.relativePath() + ".");
                continue;
            }
            if (!Files.exists(resolved)) {
                messages.add("Falta el archivo del asset " + asset.id() + " (" + asset.kind() + "): " + asset.relativePath() + ".");
                continue;
            }
            if (!Files.isRegularFile(resolved)) {
                messages.add("El asset " + asset.id() + " no apunta a un archivo regular: " + asset.relativePath() + ".");
            }
        }
    }

    private static void validateMaterializedArtifacts(DocuPodcastProject project, ProjectWorkspaceHydration hydration, List<String> messages) {
        ProjectKind kind = project.metadata().kind();
        if (kind == ProjectKind.EMPTY) {
            return;
        }
        boolean declaresImportedDocument = project.assets().containsKind(ProjectAssetKind.IMPORTED_DOCUMENT);
        boolean declaresNarrationScript = project.assets().containsKind(ProjectAssetKind.NARRATION_SCRIPT);
        boolean declaresStoryboard = project.assets().containsKind(ProjectAssetKind.STORYBOARD_MANIFEST);

        if ((kind == ProjectKind.DOCUMENT_ONLY || declaresImportedDocument) && hydration.importedDocument().isEmpty()) {
            messages.add("El proyecto declara documento importado pero no se pudo rehidratar document/document.json.");
        }
        if (requiresScript(kind) && hydration.narrationScript().isEmpty()) {
            messages.add("El proyecto declara lectura preparada interna pero no se pudo rehidratar script/narration-script.json.");
        }
        if (declaresNarrationScript && hydration.narrationScript().isEmpty()) {
            messages.add("Existe asset de lectura preparada interna, pero la proyección materializada no se cargó.");
        }
        if (requiresStoryboard(kind) && hydration.storyboard().isEmpty()) {
            messages.add("El proyecto declara storyboard pero no se pudo rehidratar storyboard/storyboard.json.");
        }
        if (declaresStoryboard && hydration.storyboard().isEmpty()) {
            messages.add("Existe asset de storyboard, pero el manifiesto de storyboard no se cargó.");
        }
        if (kind == ProjectKind.AUDIO_PROJECT && !hasAnyAudioAsset(project)) {
            messages.add("El proyecto de audio no tiene clips, audio final ni manifest de audio registrados.");
        }
    }

    private static boolean requiresScript(ProjectKind kind) {
        return kind == ProjectKind.NARRATION_SCRIPT
                || kind == ProjectKind.STORYBOARD
                || kind == ProjectKind.AUDIO_PROJECT
                || kind == ProjectKind.FULL_PROJECT;
    }

    private static boolean requiresStoryboard(ProjectKind kind) {
        return kind == ProjectKind.STORYBOARD || kind == ProjectKind.FULL_PROJECT;
    }

    private static boolean hasAnyAudioAsset(DocuPodcastProject project) {
        return project.assets().containsKind(ProjectAssetKind.AUDIO_CLIP)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_FINAL)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_MANIFEST);
    }

    private static void validateStoryboardBindings(DocuPodcastProject project, ProjectWorkspaceHydration hydration, List<String> messages) {
        if (hydration.storyboard().isEmpty()) {
            return;
        }
        StoryboardDocument storyboard = hydration.storyboard().get();
        Set<String> segmentIds = hydration.narrationScript()
                .map(NarrationScriptDocument::segments)
                .orElse(List.of())
                .stream()
                .map(segment -> segment.id())
                .collect(Collectors.toUnmodifiableSet());
        boolean hasScript = hydration.narrationScript().isPresent();
        for (StoryboardBinding binding : storyboard.bindings()) {
            if (hasScript && !segmentIds.contains(binding.segmentId())) {
                messages.add("Storyboard referencia un segmento inexistente: " + binding.segmentId() + ".");
            }
            boolean imageAssetExists = project.assets().byId(binding.imageAssetId())
                    .filter(asset -> asset.kind() == ProjectAssetKind.IMAGE || asset.kind() == ProjectAssetKind.THUMBNAIL)
                    .isPresent();
            if (!imageAssetExists) {
                messages.add("Storyboard referencia un asset de imagen inexistente o no visual: " + binding.imageAssetId() + ".");
            }
        }
    }
}
