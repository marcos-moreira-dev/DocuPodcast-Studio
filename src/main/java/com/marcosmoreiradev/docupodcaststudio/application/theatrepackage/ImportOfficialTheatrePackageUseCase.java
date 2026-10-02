package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.ResolveTheatreInterventionSnapshotUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.TheatrePackageSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Atomic official theatre-v2 package importer. Folder and ZIP share this exact pipeline. */
public final class ImportOfficialTheatrePackageUseCase {
    private final JsonTheatrePackageScanner scanner = new JsonTheatrePackageScanner();
    private final TheatreImportUseCase importer = new TheatreImportUseCase();
    private final ResolveTheatreInterventionSnapshotUseCase snapshots = new ResolveTheatreInterventionSnapshotUseCase();

    public Result execute(DocuPodcastProject original, Path source, Path projectRoot,
                          NarrationScriptDocument previousScript) throws IOException {
        Objects.requireNonNull(original, "original"); Objects.requireNonNull(projectRoot, "projectRoot");
        try (TheatrePackageSource resolved = TheatrePackageSource.open(source)) {
            TheatrePackageInventory inventory = scanner.scan(resolved.root());
            if (inventory.schemaVersion() != 2) throw new IOException("La importación oficial requiere schemaVersion 2.");
            Path grammarFile = scanner.grammarFile(resolved.root());
            ImportPlan plan;
            try { plan = TheatreGrammarMarkdownParser.parse(Files.readString(grammarFile, StandardCharsets.UTF_8)); }
            catch (RuntimeException ex) { throw new IOException("Gramática theatre-v2 inválida: " + ex.getMessage(), ex); }
            if (!plan.grammarVersion().equals("theatre-v2")) throw new IOException("obra.teatro.md debe declarar grammarVersion: theatre-v2.");
            var configured = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarVoiceConfiguration().enrich(grammarFile, plan);
            plan = configured.plan();
            ArrayList<String> importWarnings = new ArrayList<>(configured.diagnostics().stream().map(d -> d.message()).toList());
            if (plan.interventions().stream().allMatch(i -> i.tono().isBlank()))
                importWarnings.add("La obra no declara tonos por intervención: se usará el tono predeterminado. "
                        + "Para elegir muestras emocionales de cada personaje, declara por ejemplo tono=ANGRY o tono=enojado.");
            var document = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder().build(plan, grammarFile);
            NarrationScriptDocument script = new com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase()
                    .build(document, original.metadata().language());
            if (script.empty()) throw new IOException("La carpeta teatral no contiene parlamentos.");

            Path voiceConfiguration = grammarFile.getParent().resolve("config/voces.csv");
            if (Files.exists(voiceConfiguration) && !voiceConfiguration.toRealPath().startsWith(resolved.root().toRealPath()))
                throw new IOException("La configuración de voces sale de la carpeta teatral.");
            String destinationKey = safe(inventory.packageId()) + "-" + contentKey(inventory, grammarFile, voiceConfiguration, resolved.root().resolve("docupodcast-theatre.json"));
            Path finalRoot = projectRoot.toAbsolutePath().normalize().resolve("assets/theatre").resolve(destinationKey);
            Path staging = projectRoot.toAbsolutePath().normalize().resolve(".docupodcast-staging").resolve("theatre-" + UUID.randomUUID());
            LinkedHashMap<String, String> importedIds = new LinkedHashMap<>();
            ArrayList<StagedTheatreAsset> stagedAssets = new ArrayList<>();
            DocuPodcastProject candidate = original.withMetadata(original.metadata().withTitle(plan.title())
                    .withMode(com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode.THEATRE_PRODUCTION));
            Map<String, String> assetSources = new HashMap<>();
            try {
                Files.createDirectories(staging);
                for (TheatrePackageEntry entry : inventory.entries()) {
                    Path sourceFile = resolved.root().resolve(entry.relativePath()).normalize();
                    String relative = entry.relativePath();
                    Path staged = staging.resolve(relative).normalize();
                    if (!staged.startsWith(staging)) throw new IOException("Ruta de asset insegura: " + entry.relativePath());
                    Files.createDirectories(staged.getParent()); Files.copy(sourceFile, staged, StandardCopyOption.REPLACE_EXISTING);
                    String physicalId = entry.metadata("packageAssetLogicalId").isBlank() ? entry.logicalId() : entry.metadata("packageAssetLogicalId");
                    String assetId = "THEATRE-" + physicalId.replaceAll("[^A-Za-z0-9_-]", "-");
                    String identity = entry.relativePath() + "|" + entry.sha256();
                    String previousIdentity = assetSources.putIfAbsent(assetId, identity);
                    if (previousIdentity != null && !previousIdentity.equals(identity)) throw new IOException("IDs de assets incompatibles: " + assetId);
                    String projectPath = "assets/theatre/" + destinationKey + "/" + relative.replace('\\', '/');
                    ProjectAssetReference reference = new ProjectAssetReference(assetId, projectKind(entry.kind()),
                            Path.of(relative).getFileName().toString(), projectPath, mime(relative),
                            entry.kind().name(), entry.sha256(), "Paquete teatral " + inventory.packageId());
                    candidate = candidate.withAsset(reference);
                    stagedAssets.add(new StagedTheatreAsset(entry, reference, staged, finalRoot.resolve(relative), false));
                    importedIds.put(entry.relativePath(), assetId);
                    importedIds.put(relative, assetId);
                    importedIds.put(entry.logicalId(), assetId);
                }
                TheatreImportUseCase.ImportResult imported = importer.execute(plan, script, candidate.voiceLibrary(), importedIds);
                candidate = candidate.withTheatre(imported.layer()).withViewState("theatre.presentationMode", scanner.presentationMode(resolved.root()));
                for (var boundary : imported.sceneBoundariesStart().entrySet()) candidate = candidate.withViewState(
                        "theatre.sceneBoundary." + boundary.getKey(), boundary.getValue() + "|" + imported.sceneBoundariesEnd().get(boundary.getKey()));
                for (var assignment : imported.emotionAssignments()) candidate = candidate.withNarrativeLayerAssignment(assignment);
                for (var assignment : imported.imageAssignments()) candidate = candidate.withNarrativeLayerAssignment(assignment);
                candidate = new ReconcileTheatreProjectUseCase().reconcile(candidate, stagedAssets, null, script);
                validateBindings(candidate, inventory);
                validate(candidate);
                Path grammarRelative = resolved.root().toRealPath().relativize(grammarFile.toRealPath());
                Files.createDirectories(staging.resolve(grammarRelative).getParent());
                Files.copy(grammarFile, staging.resolve(grammarRelative), StandardCopyOption.REPLACE_EXISTING);
                if (Files.isRegularFile(voiceConfiguration)) {
                    Path voiceRelative = resolved.root().toRealPath().relativize(voiceConfiguration.toRealPath());
                    if (!staging.resolve(voiceRelative).normalize().startsWith(staging)) throw new IOException("Configuración fuera de la carpeta");
                    Files.createDirectories(staging.resolve(voiceRelative).getParent());
                    Files.copy(voiceConfiguration, staging.resolve(voiceRelative), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(resolved.root().resolve(JsonTheatrePackageScanner.MANIFEST_FILE), staging.resolve(JsonTheatrePackageScanner.MANIFEST_FILE), StandardCopyOption.REPLACE_EXISTING);
                Files.createDirectories(finalRoot.getParent());
                if (Files.exists(finalRoot)) {
                    try (var files = Files.walk(staging)) {
                        for (Path file : files.filter(Files::isRegularFile).toList()) {
                            Path existing = finalRoot.resolve(staging.relativize(file));
                            if (!Files.isRegularFile(existing) || Files.mismatch(file, existing) != -1)
                                throw new IOException("La copia del paquete fue modificada: " + existing);
                        }
                    }
                    deleteTree(staging);
                } else Files.move(staging, finalRoot, StandardCopyOption.ATOMIC_MOVE);
                var portableDocument = new com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument(
                        document.title(), document.format(), finalRoot.resolve(grammarRelative), document.blocks());
                return new Result(candidate, inventory, canonicalSnapshots(candidate, script),
                        List.copyOf(importWarnings), portableDocument, script);
            } catch (Exception ex) {
                deleteTree(staging);
                if (ex instanceof IOException io) throw io;
                throw new IOException("No se pudo importar el paquete teatral: " + ex.getMessage(), ex);
            }
        }
    }

    private void validate(DocuPodcastProject project) throws IOException {
        Set<String> ids = new HashSet<>();
        for (var value : project.theatre().intervenciones()) if (!ids.add(value.id())) throw new IOException("ID de intervención duplicado: " + value.id());
        ids.clear(); for (var value : project.theatre().characters()) if (!ids.add(value.id())) throw new IOException("ID de personaje duplicado: " + value.id());
        ids.clear(); for (var value : project.theatre().objects()) if (!ids.add(value.id())) throw new IOException("ID de objeto duplicado: " + value.id());
        Set<String> cameraIds = project.theatre().cameraReferences().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
        for (var cue : project.theatre().cameraCues()) if (!cameraIds.contains(cue.cameraId())) throw new IOException("Cámara teatral inexistente: " + cue.cameraId());
        for (var state : project.theatre().interventionStates()) {
            Map<String, EnumSet<com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState.EventType>> eventTypes = new HashMap<>();
            state.events().stream().filter(event -> !event.characterId().isBlank()).forEach(event ->
                    eventTypes.computeIfAbsent(event.characterId(), ignored -> EnumSet.noneOf(com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState.EventType.class)).add(event.type()));
            for (var entry : eventTypes.entrySet()) if (entry.getValue().contains(com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState.EventType.ENTER)
                    && entry.getValue().contains(com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState.EventType.EXIT))
                throw new IOException("Entrada y salida contradictorias en " + state.interventionId() + ": " + entry.getKey());
        }
        for (var value : canonicalSnapshots(project)) {
            if (!value.invalidReferences().isEmpty()) throw new IOException("Referencias teatrales inválidas en " + value.interventionId() + ": " + value.invalidReferences());
            var speaker = value.characters().get(value.speakerCharacterId());
            if (speaker != null && !speaker.present()) throw new IOException("El personaje ausente no puede hablar en " + value.interventionId());
            for (var object : value.objects().entrySet()) if (!object.getValue().holderCharacterId().isBlank()) {
                var holder = value.characters().get(object.getValue().holderCharacterId());
                if (holder == null || !holder.present()) throw new IOException("Objeto " + object.getKey() + " portado por personaje ausente en " + value.interventionId());
            }
        }
    }

    private static void validateBindings(DocuPodcastProject project, TheatrePackageInventory inventory) throws IOException {
        Set<String> characters = project.theatre().characters().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
        Set<String> objects = project.theatre().objects().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
        Set<String> scenes = project.theatre().scenes().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
        Set<String> interventions = project.theatre().intervenciones().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
        for (TheatrePackageEntry entry : inventory.entries()) {
            if (!entry.metadata("characterId").isBlank() && !characters.contains(entry.metadata("characterId"))) throw new IOException("Asset refiere personaje inexistente: " + entry.metadata("characterId"));
            if (!entry.metadata("objectId").isBlank() && !objects.contains(entry.metadata("objectId"))) throw new IOException("Asset refiere objeto inexistente: " + entry.metadata("objectId"));
            if (!entry.metadata("sceneId").isBlank() && !scenes.contains(entry.metadata("sceneId"))) throw new IOException("Asset refiere escena inexistente: " + entry.metadata("sceneId"));
            if (!entry.metadata("interventionId").isBlank() && !interventions.contains(entry.metadata("interventionId"))) throw new IOException("Asset refiere intervención inexistente: " + entry.metadata("interventionId"));
        }
    }

    private List<TheatreInterventionSnapshot> canonicalSnapshots(DocuPodcastProject project) {
        return canonicalSnapshots(project, null);
    }

    private List<TheatreInterventionSnapshot> canonicalSnapshots(DocuPodcastProject project, NarrationScriptDocument script) {
        return project.theatre().intervenciones().stream().sorted(Comparator.comparingInt(value -> value.sequenceIndex()))
                .map(value -> snapshots.execute(project, script, value.id())).toList();
    }

    private static ProjectAssetKind projectKind(TheatrePackageAssetKind kind) {
        return switch (kind) {
            case HUMAN_AUDIO, VOICE_SAMPLE -> ProjectAssetKind.AUDIO_CLIP;
            case VIDEO -> ProjectAssetKind.VIDEO_SOURCE;
            default -> ProjectAssetKind.IMAGE;
        };
    }
    private static String mime(String path) throws IOException { String value = Files.probeContentType(Path.of(path)); return value == null ? "application/octet-stream" : value; }
    private static String safe(String value) { String result = value.replaceAll("[^A-Za-z0-9_-]", "-"); return result.isBlank() ? "theatre" : result; }
    private static String contentKey(TheatrePackageInventory inventory, Path grammar, Path voices, Path manifest) throws IOException {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(inventory.fingerprint().getBytes(StandardCharsets.UTF_8));
            digest.update(Files.readAllBytes(grammar));
            digest.update(Files.readAllBytes(manifest));
            if (Files.isRegularFile(voices)) digest.update(Files.readAllBytes(voices));
            return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static void deleteTree(Path root) { try { if (root != null && Files.exists(root)) try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } } catch (IOException ignored) { } }

    public record Result(DocuPodcastProject project, TheatrePackageInventory inventory,
                         List<TheatreInterventionSnapshot> snapshots, List<String> warnings,
                         com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument document,
                         NarrationScriptDocument script) { }
}
