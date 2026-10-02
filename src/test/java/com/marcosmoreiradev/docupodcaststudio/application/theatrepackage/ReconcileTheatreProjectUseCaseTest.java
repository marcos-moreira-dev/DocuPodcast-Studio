package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDelta;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreRefreshPlan;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReconcileTheatreProjectUseCaseTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void renameReplacesTheOldBindingAndCatalogReferenceWithoutTouchingOtherWork() {
        TheatrePackageEntry oldEntry = entry("capitan-frontal-v1", "assets/personajes/capitan/v1.png");
        TheatrePackageEntry newEntry = entry("capitan-frontal-v2", "assets/personajes/capitan/v2.png");
        ProjectAssetReference oldReference = reference("THEATRE-CAPITAN-FRONTAL-V1", "media/images/theatre/old.png");
        ProjectAssetReference newReference = reference("THEATRE-CAPITAN-FRONTAL-V2", "media/images/theatre/new.png");
        ProjectAssetReference unrelated = reference("USER-SCENERY", "media/images/user/scenery.png");

        TheatreProjectLayer theatre = TheatreProjectLayer.empty().withCharacterImages(List.of(
                new TheatreProjectLayer.CharacterImage(oldEntry.logicalId(), "capitan", "escena-1",
                        "frontal", oldReference.id(), "anterior"),
                new TheatreProjectLayer.CharacterImage("dibujo-manual", "capitan", "escena-1",
                        "perfil", unrelated.id(), "trabajo del usuario")));
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION)
                .withTheatre(theatre).withAsset(oldReference).withAsset(unrelated);
        StagedTheatreAsset staged = new StagedTheatreAsset(newEntry, newReference,
                Path.of("staging/new.png"), Path.of("project/media/images/theatre/new.png"), false);
        TheatreRefreshPlan plan = new TheatreRefreshPlan("obra", "antes", "despues", List.of(
                new TheatrePackageDelta(TheatrePackageDeltaStatus.RENAMED, oldEntry, newEntry,
                        "Mismo contenido identificado con una ruta nueva")));

        DocuPodcastProject result = new ReconcileTheatreProjectUseCase()
                .reconcile(project, List.of(staged), plan);

        assertTrue(result.assets().byId(oldReference.id()).isEmpty());
        assertTrue(result.assets().byId(newReference.id()).isPresent());
        assertTrue(result.assets().byId(unrelated.id()).isPresent());
        assertTrue(result.theatre().characterImages().stream()
                .noneMatch(image -> image.id().equals(oldEntry.logicalId())));
        assertTrue(result.theatre().characterImages().stream()
                .anyMatch(image -> image.id().equals(newEntry.logicalId())
                        && image.assetId().equals(newReference.id())));
        assertTrue(result.theatre().characterImages().stream()
                .anyMatch(image -> image.id().equals("dibujo-manual")));
    }

    private static TheatrePackageEntry entry(String id, String path) {
        return new TheatrePackageEntry(id, TheatrePackageAssetKind.CHARACTER_IMAGE, path, HASH, 24,
                Map.of("characterId", "capitan", "sceneId", "escena-1", "view", "frontal"));
    }

    private static ProjectAssetReference reference(String id, String path) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, id, path, "image/png",
                "Teatro", HASH, "");
    }
}
