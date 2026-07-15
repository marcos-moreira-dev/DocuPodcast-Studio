package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Validates that visual bindings still trace to project assets and portable files. */
public final class VisualAssetTracePolicy {
    public boolean missingAsset(ProjectAssetCatalog assets, String assetId, String assetPath, Path projectDirectory) {
        String id = normalize(assetId);
        String path = normalize(assetPath);
        if (!id.isBlank()) {
            Optional<ProjectAssetReference> asset = assets == null ? Optional.empty() : assets.byId(id);
            if (asset.isEmpty()) {
                return true;
            }
            path = asset.get().relativePath();
        }
        if (projectDirectory != null && !path.isBlank()) {
            Path target = projectDirectory.toAbsolutePath().normalize().resolve(path).normalize();
            if (!target.startsWith(projectDirectory.toAbsolutePath().normalize())) {
                return true;
            }
            return !Files.isRegularFile(target);
        }
        return false;
    }

    public VisualDiagnostic diagnostic(FragmentAssetBinding binding, ProjectAssetCatalog assets, Path projectDirectory) {
        if (binding == null || !missingAsset(assets, binding.assetId(), binding.assetPath(), projectDirectory)) {
            return null;
        }
        String id = binding.assetId().isBlank() ? binding.assetPath() : binding.assetId();
        return VisualDiagnostic.blocker("VISUAL_ASSET_MISSING",
                "El visual asociado no existe o no se puede resolver: " + id,
                binding.fragmentId().value());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
