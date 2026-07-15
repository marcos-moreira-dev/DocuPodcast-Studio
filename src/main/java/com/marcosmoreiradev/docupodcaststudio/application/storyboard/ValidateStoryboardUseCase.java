package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Validates storyboard bindings against the current script and asset catalog. */
public final class ValidateStoryboardUseCase {
    public List<StoryboardValidationIssue> validate(StoryboardDocument storyboard, NarrationScriptDocument script, ProjectAssetCatalog assets) {
        Objects.requireNonNull(storyboard, "storyboard");
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(assets, "assets");
        ArrayList<StoryboardValidationIssue> issues = new ArrayList<>();
        if (script.empty()) {
            issues.add(new StoryboardValidationIssue("ERROR", "No hay lectura preparada para el panel visual", storyboard.id()));
        }
        if (storyboard.bindings().isEmpty()) {
            issues.add(new StoryboardValidationIssue("WARNING", "El storyboard todavía no tiene imágenes asociadas", storyboard.id()));
        }
        storyboard.bindings().forEach(binding -> {
            if (script.segmentById(binding.segmentId()).isEmpty()) {
                issues.add(new StoryboardValidationIssue("ERROR", "La imagen apunta a un segmento inexistente", binding.segmentId()));
            }
            if (assets.byId(binding.imageAssetId()).filter(asset -> asset.kind() == ProjectAssetKind.IMAGE).isEmpty()) {
                issues.add(new StoryboardValidationIssue("ERROR", "La imagen asociada no existe en assets", binding.imageAssetId()));
            }
        });
        return List.copyOf(issues);
    }
}
