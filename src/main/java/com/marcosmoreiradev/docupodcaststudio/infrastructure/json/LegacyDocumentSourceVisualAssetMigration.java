package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Upgrades the old path-valued sourceRoiAssetId into the single project asset model. */
final class LegacyDocumentSourceVisualAssetMigration {
    record Result(ProjectAssetCatalog assets, StudyProjectLayer study) { }

    Result migrate(ProjectAssetCatalog currentAssets, StudyProjectLayer currentStudy) {
        ProjectAssetCatalog assets = currentAssets == null
                ? ProjectAssetCatalog.empty() : currentAssets;
        StudyProjectLayer study = currentStudy == null
                ? StudyProjectLayer.empty() : currentStudy;
        DocumentStudyVideoConfiguration configuration =
                study.documentaryVideoConfiguration();
        ArrayList<DocumentVideoSlideConfiguration> slides = new ArrayList<>();
        boolean changed = false;
        for (DocumentVideoSlideConfiguration slide : configuration.contentSlides()) {
            String reference = slide.sourceVisualAssetId();
            if (reference.isBlank() || assets.byId(reference).isPresent()
                    || !looksLikeLegacyPath(reference)) {
                slides.add(slide);
                continue;
            }
            String normalized = reference.replace('\\', '/');
            ProjectAssetReference existing = assets.references().stream()
                    .filter(asset -> asset.relativePath().equals(normalized))
                    .findFirst().orElse(null);
            ProjectAssetReference migrated = existing == null
                    ? legacyReference(normalized) : existing;
            if (migrated == null) {
                slides.add(slide);
                continue;
            }
            assets = assets.withReference(migrated);
            slides.add(slide.withSourceVisualAsset(
                    migrated.id(), slide.sourceVisualFingerprint(),
                    slide.sourceFingerprint()));
            changed = true;
        }
        if (!changed) return new Result(assets, study);
        DocumentStudyVideoConfiguration migratedConfiguration =
                new DocumentStudyVideoConfiguration(
                        configuration.videoTitle(),
                        configuration.defaultTableDurationSeconds(),
                        configuration.paragraphVisuals(),
                        configuration.tableSlides(),
                        configuration.musicTracks(),
                        configuration.disabledBlockIds(),
                        configuration.closingSlides(),
                        List.copyOf(slides),
                        configuration.secondarySlideInclusionMode(), configuration.aiIllustrationsEnabled(), configuration.aiIllustrationAppearance());
        return new Result(assets,
                study.withDocumentaryVideoConfiguration(migratedConfiguration));
    }

    private static ProjectAssetReference legacyReference(String relativePath) {
        try {
            String id = "AST-DOCSRC-LEGACY-" + sha256(relativePath)
                    .substring(0, 20).toUpperCase(Locale.ROOT);
            Path path = Path.of(relativePath.replace('/', '\\'));
            String displayName = path.getFileName() == null
                    ? "visual-documental" : path.getFileName().toString();
            return new ProjectAssetReference(
                    id, ProjectAssetKind.STUDY_SOURCE_CROP,
                    displayName, relativePath, mimeType(relativePath),
                    "Visual original materializado del contenido documental",
                    "", "Migrado desde sourceRoiAssetId de un proyecto anterior.");
        } catch (RuntimeException invalidLegacyPath) {
            return null;
        }
    }

    private static boolean looksLikeLegacyPath(String value) {
        String normalized = value == null ? "" : value.strip()
                .toLowerCase(Locale.ROOT);
        return normalized.contains("/") || normalized.contains("\\")
                || normalized.endsWith(".png") || normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg") || normalized.endsWith(".webp");
    }

    private static String mimeType(String path) {
        String value = path.toLowerCase(Locale.ROOT);
        if (value.endsWith(".jpg") || value.endsWith(".jpeg")) return "image/jpeg";
        if (value.endsWith(".webp")) return "image/webp";
        return "image/png";
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
