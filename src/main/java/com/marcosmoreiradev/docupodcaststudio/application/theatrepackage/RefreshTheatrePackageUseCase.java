package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.project.InspectProjectIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRepository;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectWorkspaceHydration;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Orchestrates scan, plan, preflight and an all-or-nothing theatre package commit. */
public final class RefreshTheatrePackageUseCase {
    private final TheatrePackageScanner scanner;
    private final TheatreImportStateRepository stateRepository;
    private final ComputeTheatreRefreshPlanUseCase planner;
    private final PreflightTheatreRefreshUseCase preflight;
    private final TheatreAssetStager assetStager;
    private final ReconcileTheatreProjectUseCase reconciler;
    private final ProjectRepository projectRepository;
    private final InspectProjectIntegrityUseCase integrity;
    private final AtomicJsonWriter atomicJsonWriter;

    public RefreshTheatrePackageUseCase(TheatrePackageScanner scanner,
                                        TheatreImportStateRepository stateRepository,
                                        ComputeTheatreRefreshPlanUseCase planner,
                                        PreflightTheatreRefreshUseCase preflight,
                                        TheatreAssetStager assetStager,
                                        ReconcileTheatreProjectUseCase reconciler,
                                        ProjectRepository projectRepository,
                                        InspectProjectIntegrityUseCase integrity,
                                        AtomicJsonWriter atomicJsonWriter) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.preflight = Objects.requireNonNull(preflight, "preflight");
        this.assetStager = Objects.requireNonNull(assetStager, "assetStager");
        this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.integrity = Objects.requireNonNull(integrity, "integrity");
        this.atomicJsonWriter = Objects.requireNonNull(atomicJsonWriter, "atomicJsonWriter");
    }

    public PreparedTheatreRefresh prepare(DocuPodcastProject project, Path projectFile, Path sourceRoot,
                                          TheatreRefreshProgressListener listener,
                                          TheatreRefreshCancellation cancellation) throws IOException {
        TheatreRefreshProgressListener progress = listener == null ? TheatreRefreshProgressListener.NONE : listener;
        TheatreRefreshCancellation token = cancellation == null ? TheatreRefreshCancellation.NEVER : cancellation;
        token.checkpoint();
        progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.SCANNING, 0, 1, "Escaneando carpeta de obra…"));
        var inventory = scanner.scan(sourceRoot);
        token.checkpoint();
        progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.PLANNING, 0, 1, "Calculando cambios…"));
        Path projectRoot = requireProjectRoot(projectFile);
        TheatreImportState previous = stateRepository.open(projectRoot).orElse(null);
        var plan = planner.compute(previous, inventory);
        token.checkpoint();
        progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.PREFLIGHT, 0, 1, "Verificando cambios…"));
        var report = preflight.inspect(project, projectFile, sourceRoot, plan);
        progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.PREFLIGHT, 1, 1, "Preflight listo."));
        return new PreparedTheatreRefresh(project, projectFile, sourceRoot, inventory, previous, report);
    }

    public TheatreRefreshResult commit(PreparedTheatreRefresh prepared,
                                       TheatreRefreshProgressListener listener,
                                       TheatreRefreshCancellation cancellation) throws IOException {
        Objects.requireNonNull(prepared, "prepared");
        if (!prepared.preflight().canCommit()) {
            throw new IOException("El preflight contiene errores bloqueantes.");
        }
        TheatreRefreshProgressListener progress = listener == null ? TheatreRefreshProgressListener.NONE : listener;
        TheatreRefreshCancellation token = cancellation == null ? TheatreRefreshCancellation.NEVER : cancellation;
        Path projectRoot = requireProjectRoot(prepared.projectFile());
        if (prepared.preflight().plan().isNoOp() && prepared.previousState() != null) {
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.COMPLETED, 1, 1,
                    "La obra ya está actualizada."));
            return new TheatreRefreshResult(TheatreRefreshResult.Status.NO_CHANGES, prepared.project(),
                    prepared.preflight(), null, "La obra ya está actualizada.");
        }

        Path transactionRoot = projectRoot.resolve(".docupodcast-staging")
                .resolve("theatre-" + UUID.randomUUID()).normalize();
        ArrayList<StagedTheatreAsset> staged = new ArrayList<>();
        ArrayList<StagedTheatreAsset> published = new ArrayList<>();
        String previousProjectJson = Files.isRegularFile(prepared.projectFile())
                ? Files.readString(prepared.projectFile(), StandardCharsets.UTF_8) : null;
        Path stateFile = projectRoot.resolve("theatre-import-state.json");
        String previousStateJson = Files.isRegularFile(stateFile)
                ? Files.readString(stateFile, StandardCharsets.UTF_8) : null;
        boolean projectWritten = false;
        boolean stateWritten = false;
        try {
            var baselineIntegrity = integrity.inspect(prepared.project(), prepared.projectFile(),
                    ProjectWorkspaceHydration.empty(), List.of());
            List<TheatrePackageEntry> changed = prepared.preflight().plan().deltas().stream()
                    .filter(delta -> delta.status() == TheatrePackageDeltaStatus.NEW
                            || delta.status() == TheatrePackageDeltaStatus.MODIFIED
                            || delta.status() == TheatrePackageDeltaStatus.RENAMED)
                    .map(delta -> delta.after()).toList();
            for (int index = 0; index < changed.size(); index++) {
                token.checkpoint();
                progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.STAGING, index, changed.size(),
                        "Preparando " + changed.get(index).relativePath()));
                staged.add(assetStager.stage(prepared.sourceRoot(), prepared.projectFile(), transactionRoot,
                        changed.get(index)));
            }
            token.checkpoint();
            DocuPodcastProject candidate = reconciler.reconcile(prepared.project(), staged,
                    prepared.preflight().plan());
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.COMMITTING, 0, 3,
                    "Guardando cambios de forma segura…"));
            for (StagedTheatreAsset item : staged) {
                assetStager.publish(item);
                published.add(item);
            }
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.COMMITTING, 1, 3,
                    "Guardando proyecto…"));
            projectRepository.save(candidate, prepared.projectFile());
            projectWritten = true;
            TheatreImportState nextState = TheatreImportState.fromInventory(prepared.inventory(), Instant.now(),
                    stateEntries(prepared));
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.COMMITTING, 2, 3,
                    "Guardando estado de sincronización…"));
            stateRepository.save(nextState, projectRoot);
            stateWritten = true;
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.VERIFYING, 0, 1,
                    "Verificando integridad…"));
            var report = integrity.inspect(candidate, prepared.projectFile(), ProjectWorkspaceHydration.empty(), List.of());
            List<String> newBlockingIssues = report.issues().stream()
                    .filter(com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityIssue::blocking)
                    .map(com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityIssue::displayLine)
                    .filter(line -> baselineIntegrity.issues().stream()
                            .map(com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityIssue::displayLine)
                            .noneMatch(line::equals))
                    .toList();
            if (!newBlockingIssues.isEmpty()) {
                throw new IOException("La verificación posterior detectó errores nuevos: "
                        + String.join(" | ", newBlockingIssues));
            }
            progress.onProgress(new TheatreRefreshProgress(TheatreRefreshStage.COMPLETED, 1, 1,
                    "Obra actualizada."));
            return new TheatreRefreshResult(TheatreRefreshResult.Status.APPLIED, candidate, prepared.preflight(), report,
                    "Obra actualizada: " + changed.size() + " cambio(s) aplicado(s).");
        } catch (Exception failure) {
            IOException rollbackFailure = rollback(prepared.projectFile(), stateFile, previousProjectJson,
                    previousStateJson, projectWritten, stateWritten, published, staged);
            if (rollbackFailure != null) failure.addSuppressed(rollbackFailure);
            if (failure instanceof IOException io) throw io;
            throw new IOException("No se pudo refrescar la obra; se restauró el estado anterior.", failure);
        } finally {
            deleteTree(transactionRoot);
        }
    }

    private IOException rollback(Path projectFile, Path stateFile, String projectJson, String stateJson,
                                 boolean projectWritten, boolean stateWritten,
                                 List<StagedTheatreAsset> published, List<StagedTheatreAsset> staged) {
        IOException failure = null;
        try {
            if (projectWritten) restore(projectFile, projectJson);
            if (stateWritten) restore(stateFile, stateJson);
        } catch (IOException ex) { failure = ex; }
        for (StagedTheatreAsset item : staged) {
            try { assetStager.rollbackPublished(item); }
            catch (IOException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
        }
        return failure;
    }

    private void restore(Path file, String contents) throws IOException {
        if (contents == null) Files.deleteIfExists(file);
        else atomicJsonWriter.write(file, contents);
    }

    private static List<TheatrePackageEntry> stateEntries(PreparedTheatreRefresh prepared) {
        LinkedHashMap<String, TheatrePackageEntry> entries = new LinkedHashMap<>();
        if (prepared.previousState() != null) prepared.previousState().entries().forEach(entry -> entries.put(entry.logicalId(), entry));
        prepared.preflight().plan().deltas().stream()
                .filter(delta -> delta.status() == TheatrePackageDeltaStatus.RENAMED && delta.before() != null)
                .map(delta -> delta.before().logicalId())
                .forEach(entries::remove);
        prepared.inventory().entries().forEach(entry -> entries.put(entry.logicalId(), entry));
        return List.copyOf(entries.values());
    }

    private static Path requireProjectRoot(Path projectFile) throws IOException {
        if (projectFile == null) throw new IOException("Guarda el proyecto antes de refrescar la obra.");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) throw new IOException("El proyecto no tiene carpeta contenedora.");
        Files.createDirectories(root);
        return root;
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }
}
