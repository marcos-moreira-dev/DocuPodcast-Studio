package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGlobalVisualAssetProjectionProvider;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVisualProductionProjectionUseCaseTest {
    private final BuildVisualProductionProjectionUseCase useCase = new BuildVisualProductionProjectionUseCase();

    @Test
    void projectsMainBridgeDocumentSupportAndBrokenAssetsByFragment() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        FragmentWorkspaceProjection source = projection(fragmentId,
                binding("STORY-MAIN", fragmentId, "IMG-STORY", FragmentAssetRole.MAIN_IMAGE,
                        FragmentAssetSource.STORYBOARD, Map.of()),
                binding("MAIN", fragmentId, "IMG-MAIN", FragmentAssetRole.MAIN_IMAGE,
                        FragmentAssetSource.NARRATIVE_LAYER, Map.of("visualPrompt", "Primer plano")),
                binding("BRIDGE", fragmentId, "IMG-BRIDGE", FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT,
                        FragmentAssetSource.NARRATIVE_LAYER, Map.of("notes", "Puente suave")),
                binding("DOC", fragmentId, "", FragmentAssetRole.DOCUMENT_IMAGE,
                        FragmentAssetSource.SOURCE_DOCUMENT, Map.of("caption", "Figura del documento"), "media/document/figura.png"),
                binding("THEATRE-MISSING", fragmentId, "IMG-MISSING", FragmentAssetRole.THEATRE_VISUAL,
                        FragmentAssetSource.THEATRE, Map.of()));
        DocuPodcastProject project = DocuPodcastProject.createNew("Video")
                .withAsset(image("IMG-STORY", "media/storyboard/story.png"))
                .withAsset(image("IMG-MAIN", "media/narrative/main.png"))
                .withAsset(image("IMG-BRIDGE", "media/narrative/bridge.png"));

        VisualProductionProjection projection = useCase.build(source, project, null);
        VisualFragmentState state = projection.fragmentById(fragmentId).orElseThrow();

        assertEquals("IMG-MAIN", state.mainImage().assetId());
        assertEquals(FragmentAssetSource.NARRATIVE_LAYER, state.mainImage().source());
        assertEquals("Primer plano", state.mainImage().prompt());
        assertEquals("IMG-BRIDGE", state.bridgeImage().assetId());
        assertEquals(1, state.documentSupport().size());
        assertTrue(state.documentSupport().getFirst().ready());
        assertEquals(1, state.theatreVisuals().size());
        assertTrue(state.theatreVisuals().getFirst().missingAsset());
        assertEquals(1, projection.readiness().brokenAssetCount());
        assertTrue(projection.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("VISUAL_ASSET_MISSING")
                        && diagnostic.fragmentId().equals(fragmentId.value())));
    }

    @Test
    void indexesGlobalTheatreVisualsWithoutPromotingStoryboardWorkspace() {
        VisualAssetTracePolicy tracePolicy = new VisualAssetTracePolicy();
        BuildVisualProductionProjectionUseCase theatreUseCase = new BuildVisualProductionProjectionUseCase(
                tracePolicy,
                new TheatreGlobalVisualAssetProjectionProvider(tracePolicy));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.CharacterImage("IMG-CHR-1", "CHR-CAP", "SCN-1",
                        "frontal", "IMG-CHAR", "")),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.Scene("SCN-1", "Escena 1", "", "", "IMG-MAP")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Teatro")
                .withAsset(image("IMG-CHAR", "media/theatre/char.png"))
                .withAsset(image("IMG-MAP", "media/theatre/map.png"))
                .withTheatre(theatre);

        VisualProductionProjection projection = theatreUseCase.build(projection(FragmentId.fromBlockId("B001")),
                project, null);

        assertEquals(2, projection.globalVisuals().size());
        assertTrue(projection.globalVisuals().stream().anyMatch(slot ->
                slot.metadata().getOrDefault("theatreKind", "").equals("character")));
        assertTrue(projection.globalVisuals().stream().anyMatch(slot ->
                slot.metadata().getOrDefault("theatreKind", "").equals("spatialMap")));
        assertFalse(projection.globalVisuals().stream().anyMatch(VisualSlotState::missingAsset));
    }

    @Test
    void visualPromptContextUsesSlotPromptAndCandidateRequiresApproval() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        VisualFragmentState state = useCase.build(projection(fragmentId,
                binding("BRIDGE", fragmentId, "IMG-BRIDGE", FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT,
                        FragmentAssetSource.NARRATIVE_LAYER, Map.of("visualPrompt", "Ciudad al amanecer"))),
                DocuPodcastProject.createNew("Video").withAsset(image("IMG-BRIDGE", "media/bridge.png")), null)
                .fragmentById(fragmentId)
                .orElseThrow();

        FragmentVisualGenerationRequest request = new BuildVisualPromptContextUseCase()
                .build(state, FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT);
        VisualGenerationCandidate candidate = new VisualGenerationCandidate("CAND-1", request.fragmentId(),
                request.role(), "", Path.of("exports/generated.png"), false, "Revision humana");

        assertEquals(FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT, request.role());
        assertEquals("Ciudad al amanecer", request.prompt());
        assertFalse(candidate.approved());
        VisualGenerationCandidate approved = candidate.approve("IMG-APPROVED");
        assertTrue(approved.approved());
        assertEquals("IMG-APPROVED", approved.assetId());
    }

    private static FragmentWorkspaceProjection projection(FragmentId fragmentId, FragmentAssetBinding... bindings) {
        DocumentFragment fragment = new DocumentFragment(fragmentId, 0, "Texto del fragmento", "Bloque 1",
                null, "B001", "SEG-001", "", FragmentStatus.NARRATABLE, Map.of());
        return new FragmentWorkspaceProjection(List.of(fragment), List.of(bindings));
    }

    private static FragmentAssetBinding binding(String id, FragmentId fragmentId, String assetId,
                                                FragmentAssetRole role, FragmentAssetSource source,
                                                Map<String, String> metadata) {
        return binding(id, fragmentId, assetId, role, source, metadata, "");
    }

    private static FragmentAssetBinding binding(String id, FragmentId fragmentId, String assetId,
                                                FragmentAssetRole role, FragmentAssetSource source,
                                                Map<String, String> metadata, String assetPath) {
        return new FragmentAssetBinding(id, fragmentId, assetId, role, source, assetPath, "READY",
                assetId.isBlank() ? id : assetId, metadata);
    }

    private static ProjectAssetReference image(String id, String relativePath) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, id, relativePath, "image/png",
                "visual-test", "", "");
    }
}
