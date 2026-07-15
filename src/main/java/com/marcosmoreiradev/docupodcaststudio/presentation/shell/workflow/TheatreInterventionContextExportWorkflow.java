package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Exports a compact visual context package for a single theatre intervention. */
public final class TheatreInterventionContextExportWorkflow {

    public Result export(ProjectSession session,
                         NarrationScriptDocument script,
                         String sceneId,
                         String interventionId,
                         Path targetDirectory) throws IOException {
        if (session == null) {
            throw new IOException("Abre un proyecto antes de exportar contexto IA.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar contexto IA."));
        Path projectRoot = Optional.ofNullable(projectFile.toAbsolutePath().normalize().getParent())
                .orElseThrow(() -> new IOException("No se pudo resolver la carpeta del proyecto."));
        Path targetRoot = targetDirectory == null ? null : targetDirectory.toAbsolutePath().normalize();
        if (targetRoot == null || !Files.isDirectory(targetRoot)) {
            throw new IOException("Selecciona una carpeta valida para el paquete IA.");
        }

        DocuPodcastProject project = session.project();
        TheatreProjectLayer theatre = project.theatre();
        TheatreProjectLayer.Scene scene = theatre.scenes().stream()
                .filter(candidate -> candidate.id().equals(sceneId))
                .findFirst()
                .orElseThrow(() -> new IOException("Escena no encontrada: " + sceneId));
        TheatreProjectLayer.Intervencion intervention = theatre.intervenciones().stream()
                .filter(candidate -> candidate.id().equals(interventionId))
                .findFirst()
                .orElseThrow(() -> new IOException("Intervencion no encontrada: " + interventionId));
        TheatreProjectLayer.TextActionPlacement placement = theatre.textActionPlacements().stream()
                .filter(candidate -> candidate.sceneId().equals(scene.id()))
                .filter(candidate -> candidate.intervencionId().equals(intervention.id()))
                .findFirst()
                .orElseThrow(() -> new IOException("Placement no encontrado para " + intervention.id() + "."));

        String speaker = displayName(project, placement.characterId());
        if (speaker.isBlank()) {
            speaker = firstNonSpecial(placement.characterLocations().keySet());
        }
        Path packageDirectory = uniqueDirectory(targetRoot.resolve(safeFileName(intervention.id() + "-" + speaker)));
        Files.createDirectories(packageDirectory);

        ProjectAssetCatalog assets = project.assets();
        ArrayList<CopiedAsset> copied = new ArrayList<>();
        copyFragmentVisual(theatre, assets, intervention.id(), projectRoot, packageDirectory, copied);
        copyParticipantImages(project, assets, placement, scene.id(), projectRoot, packageDirectory, copied);
        copyObjectImages(project, assets, scene.id(), interventionText(script, intervention), projectRoot, packageDirectory, copied);
        copyAssetById(assets, scene.spatialMapAssetId(), projectRoot, packageDirectory,
                packageDirectory.resolve("mapa"), "mapa espacial", copied);
        copyEmptyTheatreReference(assets, projectRoot, packageDirectory, copied);

        Path markdown = packageDirectory.resolve("contexto-intervencion.md");
        Files.writeString(markdown, markdown(project, theatre, scene, intervention, placement, speaker,
                interventionText(script, intervention), copied), StandardCharsets.UTF_8);
        return new Result(packageDirectory, copied.size(), markdown);
    }

    private static void copyFragmentVisual(TheatreProjectLayer theatre,
                                           ProjectAssetCatalog assets,
                                           String interventionId,
                                           Path projectRoot,
                                           Path packageDirectory,
                                           List<CopiedAsset> copied) throws IOException {
        Optional<String> assetId = theatre.intervencionesVisuales().stream()
                .filter(visual -> visual.intervencionId().equals(interventionId))
                .map(TheatreProjectLayer.IntervencionVisual::assetId)
                .findFirst();
        if (assetId.isPresent()) {
            copyAssetById(assets, assetId.get(), projectRoot, packageDirectory,
                    packageDirectory.resolve("fragmento"), "fragmento visual", copied);
        }
    }

    private static void copyParticipantImages(DocuPodcastProject project,
                                              ProjectAssetCatalog assets,
                                              TheatreProjectLayer.TextActionPlacement placement,
                                              String sceneId,
                                              Path projectRoot,
                                              Path packageDirectory,
                                              List<CopiedAsset> copied) throws IOException {
        LinkedHashMap<String, String> participants = participants(project, placement);
        for (Map.Entry<String, String> participant : participants.entrySet()) {
            String characterId = participant.getKey();
            String display = participant.getValue();
            List<TheatreProjectLayer.CharacterImage> images = project.theatre().characterImages().stream()
                    .filter(image -> image.characterId().equals(characterId))
                    .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                    .sorted(Comparator.comparing((TheatreProjectLayer.CharacterImage image) -> image.sceneId().isBlank()).thenComparing(TheatreProjectLayer.CharacterImage::view))
                    .toList();
            for (TheatreProjectLayer.CharacterImage image : images) {
                copyAssetById(assets, image.assetId(), projectRoot, packageDirectory,
                        packageDirectory.resolve("personajes").resolve(safeFileName(display)),
                        "personaje " + display + " (" + image.view() + ")",
                        copied);
            }
        }
    }

    private static void copyObjectImages(DocuPodcastProject project,
                                         ProjectAssetCatalog assets,
                                         String sceneId,
                                         String text,
                                         Path projectRoot,
                                         Path packageDirectory,
                                         List<CopiedAsset> copied) throws IOException {
        String normalizedText = normalize(text);
        Set<String> objectIds = new LinkedHashSet<>();
        List<TheatreProjectLayer.TheatreObject> mentioned = project.theatre().objects().stream()
                .filter(object -> normalizedText.contains(normalize(object.displayName())))
                .toList();
        mentioned.forEach(object -> objectIds.add(object.id()));
        project.theatre().objectImages().stream()
                .filter(image -> image.sceneId().isBlank() || image.sceneId().equals(sceneId))
                .forEach(image -> objectIds.add(image.objectId()));
        if (objectIds.isEmpty()) {
            return;
        }
        for (String objectId : objectIds) {
            String objectName = objectName(project, objectId);
            for (TheatreProjectLayer.ObjectImage image : project.theatre().objectImages()) {
                if (image.objectId().equals(objectId) && (image.sceneId().isBlank() || image.sceneId().equals(sceneId))) {
                    copyAssetById(assets, image.assetId(), projectRoot, packageDirectory,
                            packageDirectory.resolve("objetos").resolve(safeFileName(objectName)),
                            "objeto " + objectName + " (" + image.view() + ")",
                            copied);
                }
            }
        }
    }

    private static void copyEmptyTheatreReference(ProjectAssetCatalog assets,
                                                  Path projectRoot,
                                                  Path packageDirectory,
                                                  List<CopiedAsset> copied) throws IOException {
        Optional<ProjectAssetReference> reference = assets.references().stream()
                .filter(ProjectAssetReference::isImage)
                .filter(asset -> normalize(asset.relativePath()).contains("teatro-vacio")
                        || normalize(asset.relativePath()).contains("teatro vacio")
                        || normalize(asset.displayName()).contains("teatro vacio"))
                .findFirst();
        if (reference.isPresent()) {
            copyAsset(reference.get(), projectRoot, packageDirectory, packageDirectory.resolve("referencia"), "teatro vacio", copied);
        }
    }

    private static void copyAssetById(ProjectAssetCatalog assets,
                                      String assetId,
                                      Path projectRoot,
                                      Path packageDirectory,
                                      Path targetDirectory,
                                      String label,
                                      List<CopiedAsset> copied) throws IOException {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        Optional<ProjectAssetReference> asset = assets.byId(assetId).filter(ProjectAssetReference::isImage);
        if (asset.isPresent()) {
            copyAsset(asset.get(), projectRoot, packageDirectory, targetDirectory, label, copied);
        }
    }

    private static void copyAsset(ProjectAssetReference asset,
                                  Path projectRoot,
                                  Path packageDirectory,
                                  Path targetDirectory,
                                  String label,
                                  List<CopiedAsset> copied) throws IOException {
        Path source = projectRoot.resolve(asset.relativePath()).toAbsolutePath().normalize();
        if (!source.startsWith(projectRoot) || !Files.isRegularFile(source)) {
            return;
        }
        Files.createDirectories(targetDirectory);
        Path target = uniqueFile(targetDirectory.resolve(source.getFileName()));
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        copied.add(new CopiedAsset(label, asset.relativePath(), packageDirectory.relativize(target).toString().replace('\\', '/')));
    }

    private static Path uniqueDirectory(Path desired) throws IOException {
        Path candidate = desired;
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = desired.resolveSibling(desired.getFileName() + "-" + suffix);
            suffix++;
        }
        return candidate;
    }

    private static Path uniqueFile(Path desired) {
        if (!Files.exists(desired)) {
            return desired;
        }
        String fileName = desired.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        int suffix = 2;
        Path candidate;
        do {
            candidate = desired.resolveSibling(base + "-" + suffix + ext);
            suffix++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private static String markdown(DocuPodcastProject project,
                                   TheatreProjectLayer theatre,
                                   TheatreProjectLayer.Scene scene,
                                   TheatreProjectLayer.Intervencion intervention,
                                   TheatreProjectLayer.TextActionPlacement placement,
                                   String speaker,
                                   String text,
                                   List<CopiedAsset> copied) {
        StringBuilder md = new StringBuilder();
        md.append("# Contexto de ").append(intervention.id()).append(System.lineSeparator()).append(System.lineSeparator());
        md.append("- Obra: ").append(project.metadata().title()).append(System.lineSeparator());
        md.append("- Acto: ").append(actName(theatre, scene.actId())).append(System.lineSeparator());
        md.append("- Escena: ").append(scene.displayName()).append(System.lineSeparator());
        md.append("- Intervencion: ").append(intervention.id()).append(System.lineSeparator());
        md.append("- Personaje hablante: ").append(speaker.isBlank() ? placement.characterId() : speaker).append(System.lineSeparator());
        md.append("- Origen: ").append(blank(placement.origin())).append(System.lineSeparator());
        md.append("- Destino: ").append(blank(placement.destination())).append(System.lineSeparator());
        md.append("- Interaccion: ").append(blank(placement.interactionTarget())).append(System.lineSeparator()).append(System.lineSeparator());
        md.append("## Texto completo").append(System.lineSeparator()).append(System.lineSeparator());
        md.append(text == null || text.isBlank() ? "Sin texto." : text).append(System.lineSeparator()).append(System.lineSeparator());
        md.append("## Posiciones por personaje").append(System.lineSeparator()).append(System.lineSeparator());
        if (placement.characterLocations().isEmpty()) {
            md.append("- Sin posiciones declaradas.").append(System.lineSeparator());
        } else {
            placement.characterLocations().forEach((character, location) ->
                    md.append("- ").append(character).append(": ").append(location).append(System.lineSeparator()));
        }
        md.append(System.lineSeparator()).append("## Imagenes copiadas").append(System.lineSeparator()).append(System.lineSeparator());
        if (copied.isEmpty()) {
            md.append("- No se copiaron imagenes.").append(System.lineSeparator());
        } else {
            copied.forEach(asset -> md.append("- ").append(asset.label()).append(": `").append(asset.relativePath()).append("`")
                    .append(" (origen: ").append(asset.originalRelativePath()).append(")").append(System.lineSeparator()));
        }
        md.append(System.lineSeparator());
        md.append("> Estas imagenes sirven como contexto rapido para una IA generadora de imagen unificada.")
                .append(System.lineSeparator());
        return md.toString();
    }

    private static LinkedHashMap<String, String> participants(DocuPodcastProject project,
                                                              TheatreProjectLayer.TextActionPlacement placement) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String speakerId = placement.characterId();
        String speakerName = displayName(project, speakerId);
        if (!speakerId.isBlank() && !speakerName.isBlank()) {
            result.put(speakerId, speakerName);
        }
        String target = placement.interactionTarget();
        if (target != null && !target.isBlank() && !specialTarget(target)) {
            String targetId = characterIdForDisplay(project, target);
            if (!targetId.isBlank()) {
                result.put(targetId, displayName(project, targetId));
            }
        }
        placement.characterLocations().keySet().forEach(name -> {
            if (name != null && !name.isBlank() && !specialTarget(name)) {
                String id = characterIdForDisplay(project, name);
                if (!id.isBlank()) {
                    result.put(id, displayName(project, id));
                }
            }
        });
        return result;
    }

    private static String interventionText(NarrationScriptDocument script, TheatreProjectLayer.Intervencion intervention) {
        if (script == null || intervention == null) {
            return "";
        }
        return script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(intervention.blockId()) || segment.id().equals(intervention.blockId()))
                .findFirst()
                .map(NarrationSegment::narrationText)
                .orElse("");
    }

    private static String displayName(DocuPodcastProject project, String characterId) {
        if (project == null || characterId == null || characterId.isBlank()) {
            return "";
        }
        return project.theatre().characters().stream()
                .filter(character -> character.id().equals(characterId))
                .map(TheatreProjectLayer.CharacterProfile::displayName)
                .findFirst()
                .orElse("");
    }

    private static String characterIdForDisplay(DocuPodcastProject project, String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) {
            return "";
        }
        return project.theatre().characters().stream()
                .filter(character -> normalize(character.displayName()).equals(normalized)
                        || character.aliases().stream().anyMatch(alias -> normalize(alias).equals(normalized)))
                .map(TheatreProjectLayer.CharacterProfile::id)
                .findFirst()
                .orElse("");
    }

    private static String objectName(DocuPodcastProject project, String objectId) {
        return project.theatre().objects().stream()
                .filter(object -> object.id().equals(objectId))
                .map(TheatreProjectLayer.TheatreObject::displayName)
                .findFirst()
                .orElse(objectId);
    }

    private static String actName(TheatreProjectLayer theatre, String actId) {
        return theatre.acts().stream()
                .filter(act -> act.id().equals(actId))
                .map(TheatreProjectLayer.TheatreAct::displayName)
                .findFirst()
                .orElse(actId == null || actId.isBlank() ? "Sin acto" : actId);
    }

    private static String firstNonSpecial(Set<String> names) {
        if (names == null) {
            return "";
        }
        return names.stream().filter(name -> !specialTarget(name)).findFirst().orElse("");
    }

    private static boolean specialTarget(String target) {
        String normalized = normalize(target);
        return normalized.equals("publico")
                || normalized.equals("para si mismo")
                || normalized.equals("entidad no presente en escenario");
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "Sin definir" : value;
    }

    private static String safeFileName(String value) {
        String normalized = normalize(value)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "intervencion" : normalized;
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    public record Result(Path folder, int copiedImages, Path markdownFile) {
    }

    private record CopiedAsset(String label, String originalRelativePath, String relativePath) {
    }
}
