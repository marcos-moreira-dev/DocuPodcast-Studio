package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the transversal visual production projection from fragments and project assets. */
public final class BuildVisualProductionProjectionUseCase {
    private final VisualAssetTracePolicy tracePolicy;

    public BuildVisualProductionProjectionUseCase() {
        this(new VisualAssetTracePolicy());
    }

    public BuildVisualProductionProjectionUseCase(VisualAssetTracePolicy tracePolicy) {
        this.tracePolicy = tracePolicy == null ? new VisualAssetTracePolicy() : tracePolicy;
    }

    public VisualProductionProjection build(
            FragmentWorkspaceProjection projection,
            DocuPodcastProject project,
            Path projectDirectory
    ) {
        if (projection == null) {
            return new VisualProductionProjection(List.of(), List.of(), readiness(List.of(), List.of()), List.of());
        }
        ProjectAssetCatalog assets = project == null ? ProjectAssetCatalog.empty() : project.assets();
        ArrayList<VisualFragmentState> fragments = new ArrayList<>();
        ArrayList<VisualDiagnostic> diagnostics = new ArrayList<>();
        for (DocumentFragment fragment : projection.fragments()) {
            List<FragmentAssetBinding> bindings = projection.bindingsForFragment(fragment.fragmentId());
            VisualSlotState main = slot(selectMainImage(bindings), assets, projectDirectory, FragmentAssetRole.MAIN_IMAGE);
            VisualSlotState bridge = slot(selectFirst(bindings, FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT), assets,
                    projectDirectory, FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT);
            List<VisualSlotState> documentSupport = bindings.stream()
                    .filter(binding -> binding.role() == FragmentAssetRole.DOCUMENT_IMAGE
                            || binding.role() == FragmentAssetRole.FORMULA_PREVIEW)
                    .map(binding -> slot(binding, assets, projectDirectory, binding.role()))
                    .toList();
            List<VisualSlotState> theatreVisuals = bindings.stream()
                    .filter(binding -> binding.role() == FragmentAssetRole.THEATRE_VISUAL)
                    .map(binding -> slot(binding, assets, projectDirectory, binding.role()))
                    .toList();
            ArrayList<VisualDiagnostic> fragmentDiagnostics = new ArrayList<>();
            for (FragmentAssetBinding binding : bindings) {
                if (visualRole(binding.role())) {
                    VisualDiagnostic diagnostic = tracePolicy.diagnostic(binding, assets, projectDirectory);
                    if (diagnostic != null) {
                        fragmentDiagnostics.add(diagnostic);
                        diagnostics.add(diagnostic);
                    }
                }
            }
            fragments.add(new VisualFragmentState(
                    fragment.fragmentId(),
                    fragment.order(),
                    fragment.segmentId(),
                    fragment.sourceBlockId(),
                    fragment.sourceLocation(),
                    fragment.text(),
                    fragment.hasSegment(),
                    main,
                    bridge,
                    documentSupport,
                    theatreVisuals,
                    fragmentDiagnostics));
        }
        fragments.sort(Comparator.comparingInt(VisualFragmentState::order));
        List<VisualSlotState> globalVisuals = globalVisuals(project, projectDirectory);
        diagnostics.addAll(globalVisuals.stream()
                .filter(VisualSlotState::missingAsset)
                .map(slot -> VisualDiagnostic.warning("GLOBAL_VISUAL_ASSET_MISSING",
                        "Visual teatral global no resuelto: " + (slot.assetId().isBlank() ? slot.assetPath() : slot.assetId()),
                        ""))
                .toList());
        return new VisualProductionProjection(fragments, globalVisuals, readiness(fragments, globalVisuals), diagnostics);
    }

    private static FragmentAssetBinding selectMainImage(List<FragmentAssetBinding> bindings) {
        return (bindings == null ? List.<FragmentAssetBinding>of() : bindings).stream()
                .filter(binding -> binding.role() == FragmentAssetRole.MAIN_IMAGE
                        || binding.role() == FragmentAssetRole.DOCUMENT_IMAGE)
                .min(Comparator.comparingInt(BuildVisualProductionProjectionUseCase::mainPriority))
                .orElse(null);
    }

    private static int mainPriority(FragmentAssetBinding binding) {
        if (binding.role() == FragmentAssetRole.MAIN_IMAGE && binding.source() == FragmentAssetSource.NARRATIVE_LAYER) {
            return 0;
        }
        if (binding.role() == FragmentAssetRole.MAIN_IMAGE && binding.source() == FragmentAssetSource.STORYBOARD) {
            return 1;
        }
        if (binding.role() == FragmentAssetRole.DOCUMENT_IMAGE) {
            return 2;
        }
        return 9;
    }

    private static FragmentAssetBinding selectFirst(List<FragmentAssetBinding> bindings, FragmentAssetRole role) {
        return (bindings == null ? List.<FragmentAssetBinding>of() : bindings).stream()
                .filter(binding -> binding.role() == role)
                .findFirst()
                .orElse(null);
    }

    private VisualSlotState slot(
            FragmentAssetBinding binding,
            ProjectAssetCatalog assets,
            Path projectDirectory,
            FragmentAssetRole fallbackRole
    ) {
        if (binding == null) {
            return VisualSlotState.empty(fallbackRole);
        }
        String path = binding.assetPath();
        if (path.isBlank() && !binding.assetId().isBlank()) {
            path = assets.byId(binding.assetId()).map(ProjectAssetReference::relativePath).orElse("");
        }
        boolean missing = tracePolicy.missingAsset(assets, binding.assetId(), path, projectDirectory);
        return new VisualSlotState(
                binding.role(),
                binding.id(),
                binding.assetId(),
                path,
                binding.source(),
                missing ? "MISSING" : binding.status(),
                binding.metadata().getOrDefault("visualPrompt", ""),
                binding.metadata().getOrDefault("notes", binding.metadata().getOrDefault("caption", "")),
                missing,
                binding.metadata());
    }

    private List<VisualSlotState> globalVisuals(DocuPodcastProject project, Path projectDirectory) {
        if (project == null || project.theatre() == null) {
            return List.of();
        }
        ProjectAssetCatalog assets = project.assets();
        TheatreProjectLayer theatre = project.theatre();
        ArrayList<VisualSlotState> result = new ArrayList<>();
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            result.add(globalSlot("THEATRE-CHAR-" + firstPresent(image.id(), image.characterId() + "-" + image.view()),
                    image.assetId(), assets, projectDirectory, "character", Map.of(
                            "characterId", image.characterId(),
                            "sceneId", image.sceneId(),
                            "view", image.view())));
        }
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            result.add(globalSlot("THEATRE-OBJ-" + firstPresent(image.id(), image.objectId() + "-" + image.view()),
                    image.assetId(), assets, projectDirectory, "object", Map.of(
                            "objectId", image.objectId(),
                            "sceneId", image.sceneId(),
                            "view", image.view())));
        }
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            if (!scene.spatialMapAssetId().isBlank()) {
                result.add(globalSlot("THEATRE-MAP-" + scene.id(), scene.spatialMapAssetId(), assets, projectDirectory,
                        "spatialMap", Map.of("sceneId", scene.id(), "sceneName", scene.displayName())));
            }
        }
        return List.copyOf(result);
    }

    private VisualSlotState globalSlot(
            String bindingId,
            String assetId,
            ProjectAssetCatalog assets,
            Path projectDirectory,
            String theatreKind,
            Map<String, String> metadata
    ) {
        String path = assets.byId(assetId).map(ProjectAssetReference::relativePath).orElse("");
        boolean missing = tracePolicy.missingAsset(assets, assetId, path, projectDirectory);
        LinkedHashMap<String, String> meta = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        meta.put("theatreKind", theatreKind);
        return new VisualSlotState(FragmentAssetRole.THEATRE_VISUAL, bindingId, assetId, path,
                FragmentAssetSource.THEATRE, missing ? "MISSING" : "READY", "", theatreKind, missing, meta);
    }

    private static boolean visualRole(FragmentAssetRole role) {
        return role == FragmentAssetRole.MAIN_IMAGE
                || role == FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT
                || role == FragmentAssetRole.DOCUMENT_IMAGE
                || role == FragmentAssetRole.FORMULA_PREVIEW
                || role == FragmentAssetRole.THEATRE_VISUAL
                || role == FragmentAssetRole.GENERATED_FRAME;
    }

    private static VisualReadiness readiness(List<VisualFragmentState> fragments, List<VisualSlotState> globalVisuals) {
        int total = fragments.size();
        int narratable = (int) fragments.stream().filter(VisualFragmentState::narratable).count();
        int mainReady = (int) fragments.stream().filter(VisualFragmentState::narratable)
                .filter(VisualFragmentState::mainImageReady).count();
        int bridgeReady = (int) fragments.stream().filter(VisualFragmentState::bridgeImageReady).count();
        int documentSupport = fragments.stream().mapToInt(fragment -> fragment.documentSupport().size()).sum();
        int theatreVisual = fragments.stream().mapToInt(fragment -> fragment.theatreVisuals().size()).sum();
        int global = globalVisuals == null ? 0 : globalVisuals.size();
        int brokenFragments = fragments.stream().mapToInt(fragment -> (int) fragment.diagnostics().stream()
                .filter(VisualDiagnostic::blocking).count()).sum();
        List<VisualSlotState> globals = globalVisuals == null ? List.of() : globalVisuals;
        int brokenGlobal = (int) globals.stream()
                .filter(VisualSlotState::missingAsset)
                .count();
        int broken = brokenFragments + brokenGlobal;
        int missingMain = Math.max(0, narratable - mainReady);
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        if (missingMain > 0) {
            blockers.add("Falta imagen principal en " + missingMain + " fragmento(s) narrativo(s).");
        }
        if (broken > 0) {
            blockers.add("Hay " + broken + " visual(es) con asset roto o inexistente.");
        }
        if (bridgeReady == 0) {
            warnings.add("No hay imagenes puente; son opcionales y no bloquean exportacion.");
        }
        return new VisualReadiness(total, narratable, mainReady, bridgeReady, documentSupport, theatreVisual,
                global, missingMain, broken, blockers, warnings);
    }

    private static String firstPresent(String first, String fallback) {
        String normalized = normalize(first);
        return normalized.isBlank() ? normalize(fallback) : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
