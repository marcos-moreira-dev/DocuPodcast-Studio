package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportEstimate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Exports all per-intervention AI context packages in a theatre hierarchy. */
public final class TheatreBulkInterventionContextExportWorkflow {
    private final TheatreGenerationUnitPlanner planner = new TheatreGenerationUnitPlanner();
    private final TheatreInterventionContextExportWorkflow singleExport = new TheatreInterventionContextExportWorkflow();

    public TheatreContextExportEstimate estimate(ProjectSession session, NarrationScriptDocument script, TheatreContextExportScope scope) {
        List<TheatreImageGenerationUnit> units = planner.units(session, script, scope);
        long bytes = 0;
        int files = 0;
        Path root = projectRoot(session);
        for (TheatreImageGenerationUnit unit : units) {
            Set<String> assetIds = packageAssetIds(session, unit);
            files += assetIds.size() + 1;
            for (String assetId : assetIds) {
                bytes += session.project().assets().byId(assetId)
                        .map(asset -> size(root.resolve(asset.relativePath()))).orElse(0L);
            }
        }
        int acts = (int) units.stream().map(TheatreImageGenerationUnit::actId).distinct().count();
        int scenes = (int) units.stream().map(TheatreImageGenerationUnit::sceneId).distinct().count();
        String warning = "La estimacion suma duplicados por intervencion; exportar toda la obra puede ocupar bastante disco.";
        return new TheatreContextExportEstimate(bytes, units.size(), files, acts, scenes, units.size(), List.of(warning));
    }

    public Result export(ProjectSession session, NarrationScriptDocument script, TheatreContextExportScope scope, Path targetDirectory) throws IOException {
        if (targetDirectory == null || !Files.isDirectory(targetDirectory)) throw new IOException("Elige una carpeta existente para crear paquetes IA.");
        List<TheatreImageGenerationUnit> units = planner.units(session, script, scope);
        Path root = targetDirectory.resolve("paquetes-ia-teatro").resolve(safe(session.title()));
        int copiedImages = 0;
        for (TheatreImageGenerationUnit unit : units) {
            Path sceneRoot = root.resolve(safe(unit.actName())).resolve(safe(unit.sceneName()));
            Files.createDirectories(sceneRoot);
            copiedImages += singleExport.export(session, script, unit.sceneId(), unit.interventionId(), sceneRoot).copiedImages();
        }
        return new Result(root, units.size(), copiedImages);
    }

    private static Set<String> packageAssetIds(ProjectSession session, TheatreImageGenerationUnit unit) {
        var theatre = session.project().theatre();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        theatre.intervencionesVisuales().stream().filter(v -> v.intervencionId().equals(unit.interventionId())).map(v -> v.assetId()).forEach(ids::add);
        theatre.textActionPlacements().stream().filter(p -> p.intervencionId().equals(unit.interventionId())).findFirst().ifPresent(placement ->
                placement.characterLocations().keySet().forEach(character -> theatre.characterImages().stream()
                        .filter(image -> image.characterId().equals(character) && (image.sceneId().isBlank() || image.sceneId().equals(unit.sceneId())))
                        .map(TheatreProjectLayer.CharacterImage::assetId).forEach(ids::add)));
        theatre.objectImages().stream().filter(image -> image.sceneId().isBlank() || image.sceneId().equals(unit.sceneId())).map(image -> image.assetId()).forEach(ids::add);
        theatre.scenes().stream().filter(scene -> scene.id().equals(unit.sceneId())).map(scene -> scene.spatialMapAssetId()).filter(id -> !id.isBlank()).forEach(ids::add);
        session.project().assets().references().stream().filter(asset -> asset.relativePath().toLowerCase(Locale.ROOT).contains("teatro-vacio")).map(ProjectAssetReference::id).forEach(ids::add);
        return ids;
    }

    private static Path projectRoot(ProjectSession session) { return session.projectFile().map(path -> path.toAbsolutePath().normalize().getParent()).orElse(Path.of(".")); }
    private static long size(Path file) { try { return Files.isRegularFile(file) ? Files.size(file) : 0L; } catch (IOException ex) { return 0L; } }
    private static String safe(String value) { return (value == null ? "sin-nombre" : value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-")).replaceAll("-+", "-"); }
    public record Result(Path root, int packages, int copiedImages) {}
}
