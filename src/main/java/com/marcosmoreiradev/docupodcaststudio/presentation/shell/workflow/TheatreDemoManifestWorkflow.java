package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAviadoresConfigurator;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreExampleSetupService;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFragmentVisualAssignmentService;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ActPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImageRef;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.InterventionPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ProfilePlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ScenePlan;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Materializes the Aviadores theatre demo from its markdown manifest. */
public final class TheatreDemoManifestWorkflow {
    private static final String NARRATOR_RESOURCE = "/examples/aviadores-comicos/assets/personajes/narrador/narrador_01_frontal.png";
    private static final String EMPTY_THEATRE_RESOURCE = "/examples/aviadores-comicos/assets/mapas/teatro-vacio.png";
    private final WorkspaceApplicationServices applicationServices;

    public TheatreDemoManifestWorkflow(WorkspaceApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }

    public ConfigureResult configureAviadores(
            String exampleId,
            List<Path> visualAssets,
            Path theatreMarkdownFile,
            Path targetProjectFile,
            ProjectSession session,
            ReadableDocument document,
            NarrationScriptDocument script,
            VoiceLibrary voiceLibrary) throws IOException {
        if (!"aviadores-comicos".equals(exampleId)) {
            return ConfigureResult.skipped();
        }
        Path projectFile = session.projectFile()
                .orElse(targetProjectFile == null ? null : targetProjectFile.toAbsolutePath().normalize());
        if (projectFile == null) {
            throw new IOException("El demo teatral necesita una ruta .docupodcast.json para copiar sus assets.");
        }
        Map<String, Path> assetsByName = assetsByName(visualAssets);
        Map<String, String> assetIds = theatreAssetIdIndex(session.project());
        importMissingAssets(session, projectFile, visualAssets, assetIds);
        for (String fileName : TheatreAviadoresConfigurator.REQUIRED_ASSET_FILENAMES) {
            if (resolveTheatreAssetId(assetIds, fileName).isEmpty() && assetsByName.containsKey(fileName)) {
                importSingleAsset(session, projectFile, assetsByName.get(fileName), assetIds);
            }
        }

        assetIds = theatreAssetIdIndex(session.project());
        ImportPlan plan = TheatreGrammarMarkdownParser.parse(readTheatreMarkdown(theatreMarkdownFile));
        validateTheatreMarkdown(plan, document, assetIds);
        TheatreExampleSetupService.TheatreSetupResult result =
                new TheatreExampleSetupService().execute(plan, script, voiceLibrary, assetIds);
        var imageAssignments = new TheatreFragmentVisualAssignmentService()
                .assign(script, session.project().assets(), result.imageAssignments());
        DocuPodcastProject project = session.project().withTheatre(result.layer())
                .withNarrativeLayerAssignments(NarrativeLayerCoordinator.filterAddTheatreManifestLayers(
                        session.project(), result.emotionAssignments(), imageAssignments))
                .withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name());
        session.replaceProject(project, true);
        return new ConfigureResult(true, result);
    }

    public boolean repairAviadoresLegacyAssets(ProjectSession session) throws IOException {
        if (session == null || !looksLikeAviadores(session.project())) {
            return false;
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de reparar assets del demo Aviadores."));
        boolean changed = false;
        Map<String, String> assetIds = theatreAssetIdIndex(session.project());
        String narratorAssetId = resolveTheatreAssetId(assetIds, "personajes/narrador/narrador_01_frontal.png").orElse("");
        if (narratorAssetId.isBlank()) {
            importSingleAsset(session, projectFile, copyResourceToTemp(NARRATOR_RESOURCE, "narrador_01_frontal", ".png"), assetIds);
            assetIds = theatreAssetIdIndex(session.project());
            narratorAssetId = resolveTheatreAssetId(assetIds, "narrador_01_frontal.png").orElse("");
            changed = true;
        }
        String narratorCharacterId = narratorCharacterId(session.project());
        if (!narratorAssetId.isBlank() && !narratorCharacterId.isBlank() && !hasGlobalCharacterImage(session.project(), narratorCharacterId, narratorAssetId)) {
            TheatreProjectLayer theatre = session.project().theatre();
            ArrayList<TheatreProjectLayer.CharacterImage> images = new ArrayList<>(theatre.characterImages());
            images.add(new TheatreProjectLayer.CharacterImage(
                    uniqueCharacterImageId(images, "CHARIMG-" + narratorCharacterId + "-GLOBAL"),
                    narratorCharacterId,
                    "",
                    "Frontal",
                    narratorAssetId,
                    "Foto frontal global reparada para el Narrador."));
            session.replaceProject(session.project().withTheatre(new TheatreProjectLayer(
                    theatre.intervenciones(),
                    theatre.characters(),
                    theatre.voiceRoleAliases(),
                    images,
                    theatre.intervencionesVisuales(),
                    theatre.intermediateFrames(),
                    theatre.acts(),
                    theatre.scenes(),
                    theatre.positions(),
                    theatre.actions(),
                    theatre.textActionPlacements(),
                    theatre.objectImages(),
                    theatre.objects(),
                    theatre.audioTracks(),
                    theatre.cameraReferences(),
                    theatre.cameraCues(),
                    theatre.stageBackdrops(),
                    theatre.stageBackdropAssignments())), true);
            updateMaterializedMarkdown(session, "personajes/narrador/narrador_01_frontal.png");
            changed = true;
        }
        assetIds = theatreAssetIdIndex(session.project());
        if (resolveTheatreAssetId(assetIds, "mapas/teatro-vacio.png").isEmpty()) {
            importSingleAsset(session, projectFile, copyResourceToTemp(EMPTY_THEATRE_RESOURCE, "teatro-vacio", ".png"), assetIds);
            changed = true;
        }
        return changed;
    }

    private void importMissingAssets(
            ProjectSession session,
            Path projectFile,
            List<Path> visualAssets,
            Map<String, String> assetIds) throws IOException {
        for (Path asset : visualAssets == null ? List.<Path>of() : visualAssets) {
            if (asset == null || asset.getFileName() == null
                    || resolveTheatreAssetId(assetIds, asset.getFileName().toString()).isPresent()) {
                continue;
            }
            importSingleAsset(session, projectFile, asset, assetIds);
        }
    }

    private void importSingleAsset(
            ProjectSession session,
            Path projectFile,
            Path asset,
            Map<String, String> assetIds) throws IOException {
        var imported = applicationServices.generation().storyboard().importImageAsset()
                .importImage(session.project(), projectFile, asset);
        session.replaceProject(imported.project(), true);
        indexTheatreAsset(assetIds, imported.imageAsset().displayName(), imported.imageAsset().id());
        indexTheatreAsset(assetIds, imported.imageAsset().relativePath(), imported.imageAsset().id());
    }

    private static Path copyResourceToTemp(String resourcePath, String prefix, String suffix) throws IOException {
        try (var input = TheatreDemoManifestWorkflow.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Recurso no encontrado: " + resourcePath);
            }
            Path tempDirectory = Files.createTempDirectory("aviadores-demo-assets");
            Path temp = tempDirectory.resolve(prefix + suffix);
            Files.copy(input, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            tempDirectory.toFile().deleteOnExit();
            return temp;
        }
    }

    private static boolean looksLikeAviadores(DocuPodcastProject project) {
        if (project == null) {
            return false;
        }
        String title = project.metadata().title().toLowerCase(Locale.ROOT);
        if (title.contains("aviadores") || title.contains("tornillo dorado")) {
            return true;
        }
        return project.theatre().scenes().stream()
                .map(TheatreProjectLayer.Scene::displayName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains("hangar") || name.contains("aterrizaje"));
    }

    private static String narratorCharacterId(DocuPodcastProject project) {
        return project.theatre().characters().stream()
                .filter(character -> character.displayName().equalsIgnoreCase("NARRADOR")
                        || character.id().equalsIgnoreCase("CHR-NARRADOR"))
                .map(TheatreProjectLayer.CharacterProfile::id)
                .findFirst()
                .orElse("");
    }

    private static boolean hasGlobalCharacterImage(DocuPodcastProject project, String characterId, String assetId) {
        return project.theatre().characterImages().stream()
                .anyMatch(image -> image.characterId().equals(characterId)
                        && image.sceneId().isBlank()
                        && image.assetId().equals(assetId));
    }

    private static String uniqueCharacterImageId(List<TheatreProjectLayer.CharacterImage> images, String baseId) {
        String candidate = baseId;
        int suffix = 2;
        while (containsCharacterImageId(images, candidate)) {
            candidate = baseId + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean containsCharacterImageId(List<TheatreProjectLayer.CharacterImage> images, String candidate) {
        for (TheatreProjectLayer.CharacterImage image : images) {
            if (image.id().equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void updateMaterializedMarkdown(ProjectSession session, String narratorPath) {
        session.projectFile().ifPresent(projectFile -> {
            Path markdown = projectFile.toAbsolutePath().normalize().getParent().resolve("teatro.md");
            if (!Files.isRegularFile(markdown)) {
                return;
            }
            try {
                String current = Files.readString(markdown, StandardCharsets.UTF_8);
                if (current.contains(narratorPath)) {
                    return;
                }
                String marker = "- Personaje: NARRADOR";
                int markerIndex = current.indexOf(marker);
                if (markerIndex < 0) {
                    return;
                }
                int lineEnd = current.indexOf('\n', markerIndex);
                if (lineEnd < 0) {
                    lineEnd = current.length();
                }
                String insertion = System.lineSeparator() + "- " + narratorPath + " | angulo=Frontal";
                Files.writeString(markdown,
                        current.substring(0, lineEnd) + insertion + current.substring(lineEnd),
                        StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        });
    }

    private static Map<String, Path> assetsByName(List<Path> visualAssets) {
        Map<String, Path> assets = new LinkedHashMap<>();
        for (Path asset : visualAssets == null ? List.<Path>of() : visualAssets) {
            if (asset != null && asset.getFileName() != null) {
                assets.put(asset.getFileName().toString(), asset);
            }
        }
        return assets;
    }

    private static String readTheatreMarkdown(Path theatreMarkdownFile) throws IOException {
        if (theatreMarkdownFile != null && Files.isRegularFile(theatreMarkdownFile)) {
            return Files.readString(theatreMarkdownFile, StandardCharsets.UTF_8);
        }
        try (var input = TheatreDemoManifestWorkflow.class.getResourceAsStream("/examples/aviadores-comicos/PROYECTO_DEMO.md")) {
            if (input == null) {
                throw new IOException("Recurso no encontrado: /examples/aviadores-comicos/PROYECTO_DEMO.md");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> theatreAssetIdIndex(DocuPodcastProject project) {
        Map<String, String> assetIds = new LinkedHashMap<>();
        if (project == null) {
            return assetIds;
        }
        for (ProjectAssetReference asset : project.assets().byKind(ProjectAssetKind.IMAGE)) {
            indexTheatreAsset(assetIds, asset.displayName(), asset.id());
            indexTheatreAsset(assetIds, asset.relativePath(), asset.id());
        }
        return assetIds;
    }

    private static void indexTheatreAsset(Map<String, String> assetIds, String key, String assetId) {
        if (assetIds == null || key == null || key.isBlank() || assetId == null || assetId.isBlank()) {
            return;
        }
        String normalized = key.strip().replace('\\', '/');
        addTheatreAssetKey(assetIds, normalized, assetId);
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
            addTheatreAssetKey(assetIds, normalized, assetId);
        }
        if (normalized.startsWith("assets/")) {
            addTheatreAssetKey(assetIds, normalized.substring("assets/".length()), assetId);
        }
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            addTheatreAssetKey(assetIds, normalized.substring(slash + 1), assetId);
        }
    }

    private static void addTheatreAssetKey(Map<String, String> assetIds, String key, String assetId) {
        if (key == null || key.isBlank()) {
            return;
        }
        assetIds.putIfAbsent(key, assetId);
        assetIds.putIfAbsent(key.toLowerCase(Locale.ROOT), assetId);
    }

    private static Optional<String> resolveTheatreAssetId(Map<String, String> assetIds, String requestedPath) {
        if (assetIds == null || assetIds.isEmpty() || requestedPath == null || requestedPath.isBlank()) {
            return Optional.empty();
        }
        String normalized = requestedPath.strip().replace('\\', '/');
        ArrayList<String> candidates = new ArrayList<>();
        candidates.add(requestedPath.strip());
        candidates.add(normalized);
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
            candidates.add(normalized);
        }
        if (normalized.startsWith("assets/")) {
            candidates.add(normalized.substring("assets/".length()));
        }
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            candidates.add(normalized.substring(slash + 1));
        }
        for (String candidate : candidates) {
            String id = assetIds.get(candidate);
            if (id != null) {
                return Optional.of(id);
            }
            id = assetIds.get(candidate.toLowerCase(Locale.ROOT));
            if (id != null) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private static void validateTheatreMarkdown(ImportPlan plan, ReadableDocument document, Map<String, String> assetIds) throws IOException {
        ArrayList<String> errors = new ArrayList<>();
        int blockCount = document == null ? 0 : document.blocks().size();
        Set<String> characters = plan.characters().stream()
                .map(ProfilePlan::name)
                .map(TheatreDemoManifestWorkflow::normalizeTheatreName)
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        for (ActPlan act : plan.acts()) {
            for (ScenePlan scene : act.scenes()) {
                if (scene.textStartIndex() <= 0 || scene.textEndIndex() <= 0) {
                    errors.add("Escena sin limites: " + scene.name());
                } else if (scene.textStartIndex() > scene.textEndIndex()) {
                    errors.add("Rango invertido en " + scene.name() + ": texto_inicio=" + scene.textStartIndex()
                            + " texto_fin=" + scene.textEndIndex());
                } else if (blockCount > 0 && (scene.textStartIndex() > blockCount || scene.textEndIndex() > blockCount)) {
                    errors.add("Rango fuera del DOCX en " + scene.name() + ": " + scene.textStartIndex()
                            + "-" + scene.textEndIndex() + " (DOCX tiene " + blockCount + " textos)");
                }
                validateImageReference(errors, assetIds, scene.spatialMap(), "mapa_espacial de escena " + scene.name());
            }
        }
        for (ProfilePlan character : plan.characters()) {
            for (ImageRef image : character.imagenes()) {
                validateImageReference(errors, assetIds, image.path(), "imagen de personaje " + character.name());
            }
        }
        for (ProfilePlan object : plan.objects()) {
            for (ImageRef image : object.imagenes()) {
                validateImageReference(errors, assetIds, image.path(), "imagen de objeto " + object.name());
            }
        }
        for (InterventionPlan intervention : plan.interventions()) {
            if (!intervention.stageDirection() && !characters.contains(normalizeTheatreName(intervention.characterName()))) {
                errors.add("Personaje desconocido: " + intervention.characterName());
            }
            validatePosition(errors, intervention.origin(), "origen de " + intervention.characterName());
            validatePosition(errors, intervention.destination(), "destino de " + intervention.characterName());
            if (!intervention.interactionTarget().isBlank()
                    && !matchesKnownCharacter(intervention.interactionTarget(), characters)) {
                errors.add("Interaccion con personaje desconocido: " + intervention.interactionTarget());
            }
            for (String image : intervention.images()) {
                validateImageReference(errors, assetIds, image, "imagen de intervencion " + intervention.sequenceIndex());
            }
        }
        if (!errors.isEmpty()) {
            throw new IOException("teatro.md no se pudo aplicar:\n- " + String.join("\n- ", errors));
        }
    }

    private static void validateImageReference(List<String> errors, Map<String, String> assetIds, String path, String context) {
        if (path != null && !path.isBlank() && resolveTheatreAssetId(assetIds, path).isEmpty()) {
            errors.add("Imagen no encontrada en " + context + ": " + path);
        }
    }

    private static void validatePosition(List<String> errors, String position, String context) {
        if (position != null && !position.isBlank() && !TheatreStageGeometry.knownPosition(position)) {
            errors.add("Posicion no reconocida en " + context + ": " + position);
        }
    }

    private static boolean matchesKnownCharacter(String candidate, Set<String> characters) {
        String normalized = normalizeTheatreName(candidate);
        if (normalized.isBlank() || TheatreStageGeometry.specialInteractionTarget(candidate) || characters.contains(normalized)) {
            return true;
        }
        return characters.stream().anyMatch(character -> character.contains(normalized) || normalized.contains(character));
    }

    private static String normalizeTheatreName(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    public record ConfigureResult(boolean configured, TheatreExampleSetupService.TheatreSetupResult setupResult) {
        static ConfigureResult skipped() {
            return new ConfigureResult(false, null);
        }
    }
}
