package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineReadinessUiItem;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRecoverySummary;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobMaintenanceReport;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.presentation.audio.AudioQueueState;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Coordinates the audio-job brain behind the narrated document.
 *
 * <p>The shell still owns visible state and JavaFX properties, but this coordinator centralizes
 * audio use-case access, recovery selection, detail labels and process diagnostics so the view-model
 * stops acting as the audio backend of the desktop app.</p>
 */
public final class AudioWorkflowCoordinator {
    private static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";
    private final WorkspaceApplicationServices applicationServices;

    public AudioWorkflowCoordinator(WorkspaceApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public AudioEngineDescriptor engineDescriptor() {
        return applicationServices.playback().audio().getAudioEngineDescriptor().get();
    }

    public List<AudioEngineAvailability> engineAvailability() {
        return applicationServices.playback().audio().listAudioEngineAvailability().list();
    }

    public List<AudioEngineReadinessUiItem> engineReadinessUi() {
        return applicationServices.playback().audio().inspectAudioEngineReadinessUi().inspect();
    }

    public List<String> engineReadinessLines() {
        return applicationServices.playback().audio().inspectAudioEngineReadinessUi().compactLines();
    }


    public String selectDocumentAudioSource(String sourceLabel) throws IOException {
        String label = sourceLabel == null ? "" : sourceLabel.strip();
        if (label.isBlank() || "Audio del computador".equals(label)) {
            return "Audio del computador seleccionado para el fragmento.";
        }
        OperationalSettings current = applicationServices.administration().settings().loadOperationalSettings().load();
        OperationalSettings updated;
        if ("Voz local simple".equals(label)) {
            updated = applicationServices.administration().settings().selectPiperAsEngine().select(current);
        } else if ("Voz IA avanzada".equals(label)) {
            updated = applicationServices.administration().settings().selectXttsAsEngine().select(current);
        } else if ("Modo de prueba".equals(label)) {
            updated = new OperationalSettings(current.readingDocument(), current.playbackBuffer(),
                    new OperationalSettings.TtsEngineSettings("mock", "", "Modo de prueba", current.tts().language(),
                            current.tts().voiceProfileId(), current.tts().timeoutSeconds(), current.tts().maxRetries()),
                    current.video(), current.imageGeneration(), current.frameGeneration(), current.compute(),
                    current.ocr(), current.storage(), current.diagnostics());
        } else {
            return "Origen de voz no reconocido: " + label + ".";
        }
        applicationServices.administration().settings().saveOperationalSettings().save(updated);
        return "Origen de voz seleccionado: " + label + ".";
    }

    public String useVoiceForDocument(VoiceProfile voice) throws IOException {
        Objects.requireNonNull(voice, "voice");
        selectDocumentAudioSource(sourceLabelForVoice(voice));
        OperationalSettings current = applicationServices.administration().settings().loadOperationalSettings().load();
        OperationalSettings.TtsEngineSettings tts = current.tts();
        OperationalSettings updated = new OperationalSettings(current.readingDocument(), current.playbackBuffer(),
                new OperationalSettings.TtsEngineSettings(tts.engineMode(), tts.commandTemplate(), tts.displayName(),
                        tts.language(), voice.id(), tts.timeoutSeconds(), tts.maxRetries(), tts.xttsDownloadBaseUrl(),
                        tts.piperRuntimeZipUrl(), tts.piperDefaultVoiceUrl(), tts.piperDefaultVoiceMetadataUrl()),
                current.video(), current.imageGeneration(), current.frameGeneration(), current.compute(),
                current.ocr(), current.storage(), current.diagnostics());
        applicationServices.administration().settings().saveOperationalSettings().save(updated);
        return "Voz del documento seleccionada: " + voice.displayName() + ".";
    }

    public String configuredVoiceProfileId() {
        try { return applicationServices.administration().settings().loadOperationalSettings().load().tts().voiceProfileId(); }
        catch (IOException | RuntimeException ex) { return ""; }
    }

    private static String sourceLabelForVoice(VoiceProfile voice) {
        return voice.engineType() == VoiceEngineType.PIPER || DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(voice.id())
                ? "Voz local simple" : "Voz IA avanzada";
    }

    public Optional<String> generationProblem(NarrationScriptDocument script, Optional<Path> projectFile) {
        if (script == null || script.empty()) {
            return Optional.of("Crea una proyección interna de narración antes de generar audio.");
        }
        if (projectFile == null || projectFile.isEmpty()) {
            return Optional.of("Guarda el proyecto antes de generar audio para crear la carpeta jobs/.");
        }
        return Optional.empty();
    }

    public AudioGenerationRequest request(NarrationScriptDocument script, Path projectDirectory, String jobName) {
        return new AudioGenerationRequest(script, projectDirectory, jobName, "es", configuredVoiceProfileId());
    }

    public AudioGenerationRequest request(NarrationScriptDocument script, Path projectDirectory, String jobName,
                                          VoiceLibrary voiceLibrary) {
        return new AudioGenerationRequest(script, null, projectDirectory, jobName, "es", configuredVoiceProfileId(), voiceLibrary);
    }

    public AudioGenerationRequest request(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan, Path projectDirectory, String jobName) {
        return new AudioGenerationRequest(script, renderUnitPlan, projectDirectory, jobName, "es", configuredVoiceProfileId(), null);
    }

    public AudioGenerationRequest request(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                          Path projectDirectory, String jobName, VoiceLibrary voiceLibrary) {
        return new AudioGenerationRequest(script, renderUnitPlan, projectDirectory, jobName, "es", configuredVoiceProfileId(), voiceLibrary);
    }

    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        return applicationServices.playback().audio().submitAudioGenerationJob().submit(request, statusConsumer);
    }

    public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
        return applicationServices.playback().audio().resumePersistedAudioJob().resume(request, snapshot, statusConsumer);
    }

    public boolean cancel(String jobId) {
        return applicationServices.playback().audio().cancelAudioGenerationJob().cancel(jobId);
    }

    public List<AudioJobSnapshot> persistedJobs(Path projectDirectory) throws IOException {
        return applicationServices.playback().audio().listPersistedAudioJobs().list(projectDirectory);
    }

    public void deletePersistedJobs(Path projectDirectory) throws IOException {
        applicationServices.playback().audio().deletePersistedAudioJobs().delete(projectDirectory);
    }

    public CompletableFuture<String> replaceAndSubmitAsync(Path projectDirectory,
                                                            AudioJobStatusDto activeStatus,
                                                            Consumer<AudioJobStatusDto> onCancelled,
                                                            AudioGenerationRequest request,
                                                            Consumer<AudioJobStatusDto> statusConsumer) {
        return prepareFreshWorkspaceAsync(projectDirectory, activeStatus, onCancelled)
                .thenApply(ignored -> submit(request, statusConsumer));
    }

    public CompletableFuture<Void> prepareFreshWorkspaceAsync(Path projectDirectory,
                                                               AudioJobStatusDto activeStatus,
                                                               Consumer<AudioJobStatusDto> onCancelled) {
        return CompletableFuture.runAsync(() -> {
            String jobId = activeStatus == null ? "" : Objects.toString(activeStatus.jobId(), "").strip();
            try {
                if (!jobId.isBlank()) {
                    cancelActiveAudioJobSilently(activeStatus, onCancelled);
                    if (!applicationServices.playback().audio().cancelAudioGenerationJob().cancelAndAwait(jobId, Duration.ofSeconds(30))) {
                        throw new IOException("El trabajo de audio activo no termino en 30 segundos. Se conservaron jobs/ y no se inicio otro render.");
                    }
                }
                deletePersistedJobs(projectDirectory);
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new CompletionException(ex);
            }
        });
    }

    public Optional<AudioJobSnapshot> recoverableSnapshot(Path projectDirectory) throws IOException {
        return persistedJobs(projectDirectory).stream()
                .filter(AudioJobSnapshot::resumable)
                .findFirst();
    }

    public Optional<AudioJobSnapshot> selectedSnapshot(List<AudioJobSnapshot> snapshots, String activeJobId) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Optional.empty();
        }
        String normalizedJobId = activeJobId == null ? "" : activeJobId.strip();
        if (!normalizedJobId.isBlank()) {
            Optional<AudioJobSnapshot> active = snapshots.stream()
                    .filter(snapshot -> snapshot.jobId().equals(normalizedJobId))
                    .findFirst();
            if (active.isPresent()) {
                return active;
            }
        }
        return snapshots.stream().filter(AudioJobSnapshot::resumable).findFirst().or(() -> Optional.of(snapshots.get(0)));
    }


    public AudioJobMaintenanceReport maintenanceReport(Path projectDirectory, AudioJobSnapshot snapshot) {
        return applicationServices.playback().audio().inspectAudioJobMaintenance().inspect(projectDirectory, snapshot);
    }

    public AudioJobMaintenanceReport maintenanceReport(Path projectDirectory, AudioJobSnapshot snapshot, SourceDocumentChangeReport sourceReport) {
        return applicationServices.playback().audio().inspectAudioJobMaintenance().inspect(projectDirectory, snapshot, sourceReport);
    }

    public List<String> jobDetailLines(AudioJobSnapshot selected) {
        Objects.requireNonNull(selected, "selected");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        AudioJobRecoverySummary summary = AudioJobRecoverySummary.from(selected);
        lines.add(summary.label());
        selected.segments().forEach(segment -> lines.add(segment.segmentId() + " · " + segment.status().displayName()
                + " · intentos " + segment.attempts()
                + (segment.audioRelativePath().isBlank() ? " · sin audio" : " · " + segment.audioRelativePath())
                + (segment.errorMessage().isBlank() ? "" : " · " + segment.errorMessage())));
        return List.copyOf(lines);
    }

    public AudioGenerationRequest buildGenerationRequest(ProjectSession session, NarrationScriptDocument script, Path projectDirectory, String jobName) {
        try {
            var narrationPlan = applicationServices.generation().render().buildNarrationRenderPlan()
                    .build(script, session.project());
            var renderUnitPlan = applicationServices.generation().render().buildRenderUnitPlan().build(narrationPlan);
            return request(script, renderUnitPlan, projectDirectory, jobName, session.project().voiceLibrary());
        } catch (RuntimeException ex) {
            if (requiresRenderUnitPlan(session)) throw ex;
            return request(script, projectDirectory, jobName, session.project().voiceLibrary());
        }
    }

    public AudioGenerationRequest buildGenerationRequestForSelection(
            ProjectSession session,
            NarrationScriptDocument fullScript,
            NarrationScriptDocument suffixScript,
            Path projectDirectory,
            String jobName,
            String startSegmentId) {
        try {
            var narrationPlan = applicationServices.generation().render().buildNarrationRenderPlan()
                    .build(fullScript, session.project());
            RenderUnitPlan fullPlan = applicationServices.generation().render().buildRenderUnitPlan().build(narrationPlan);
            RenderUnitPlan suffixPlan = renderUnitPlanStartingAt(fullPlan, suffixScript, startSegmentId);
            if (suffixPlan == null || suffixPlan.audioUnits().isEmpty()) {
                return request(suffixScript, projectDirectory, jobName + " desde " + startSegmentId, session.project().voiceLibrary());
            }
            return request(suffixScript, suffixPlan, projectDirectory, jobName + " desde " + startSegmentId, session.project().voiceLibrary());
        } catch (RuntimeException ex) {
            if (requiresRenderUnitPlan(session)) throw ex;
            return request(suffixScript, projectDirectory, jobName + " desde " + startSegmentId, session.project().voiceLibrary());
        }
    }

    public AudioGenerationRequest buildInterventionRegenerationRequest(ProjectSession session,
                                                                        NarrationScriptDocument script,
                                                                        Path projectDirectory,
                                                                        String jobName,
                                                                        String segmentId) {
        NarrationSegment segment = script.segmentById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la intervencion " + segmentId + "."));
        var narrationPlan = applicationServices.generation().render().buildNarrationRenderPlan().build(script, session.project());
        RenderUnitPlan fullPlan = applicationServices.generation().render().buildRenderUnitPlan().build(narrationPlan);
        var units = fullPlan.units().stream().filter(unit -> unit.segmentId().equals(segment.id())).toList();
        if (units.isEmpty()) throw new IllegalStateException("La intervencion no contiene unidades de audio regenerables.");
        if (units.stream().anyMatch(com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit::usesExternalAudio))
            throw new IllegalStateException("La intervencion usa audio humano importado. Retiralo antes de volver a TTS.");
        NarrationScriptDocument isolated = new NarrationScriptDocument(script.id(), script.title(), script.language(),
                script.sourceDocumentTitle(), List.of(segment), script.createdAt(), script.updatedAt(), script.notes());
        RenderUnitPlan isolatedPlan = new RenderUnitPlan(fullPlan.id() + "-ONLY-" + segment.id(), fullPlan.sourceScriptId(),
                units, fullPlan.defaultSilentVisualDurationSeconds(), java.time.Instant.now());
        return request(isolated, isolatedPlan, projectDirectory, jobName + " - " + segment.id(), session.project().voiceLibrary());
    }

    public String audioEngineUnavailableMessage() {
        AudioEngineDescriptor descriptor = engineDescriptor();
        String name = descriptor.displayName().isBlank() ? "motor de voz" : descriptor.displayName();
        String detail = descriptor.message().isBlank() ? "Prepara un motor de voz antes de generar audio." : descriptor.message();
        return name + " no está disponible para generar audio. " + detail + " Puedes gestionarlo desde Configuración.";
    }

    public AudioQueueState buildAudioQueueState(AudioJobStatusDto activeStatus, Optional<Path> projectFile, List<String> playbackLabels) {
        String engineLabel = engineDescriptor().statusLabel();
        if (projectFile == null || projectFile.isEmpty()) {
            return AudioQueueState.unsaved(engineLabel, activeStatus, playbackLabels);
        }
        try {
            Path projectDirectory = projectFile.get().toAbsolutePath().normalize().getParent();
            List<AudioJobSnapshot> snapshots = persistedJobs(projectDirectory);
            Optional<AudioJobSnapshot> sel = selectedSnapshot(snapshots, activeStatus == null ? "" : activeStatus.jobId());
            List<String> details = sel.map(this::jobDetailLines).orElseGet(() -> List.of("No hay jobs persistidos todavía."));
            List<String> diagnostics = sel.map(s -> {
                try { return diagnosticLabels(projectDirectory, s); }
                catch (IOException ex) { return List.of("No se pudieron leer diagnósticos: " + ex.getMessage()); }
            }).orElseGet(() -> List.of("Sin diagnósticos: no hay job seleccionado."));
            return AudioQueueState.saved(engineLabel, activeStatus, projectDirectory, snapshots, details, diagnostics, playbackLabels);
        } catch (IOException ex) {
            return AudioQueueState.saved(engineLabel, activeStatus, null, List.of(),
                    List.of("No se pudo leer cola de audio: " + ex.getMessage()),
                    List.of("No se pudo leer diagnósticos de audio."), playbackLabels);
        }
    }

    public static boolean requiresRenderUnitPlan(ProjectSession session) {
        if (session == null || session.project() == null) {
            return false;
        }
        DocuPodcastProject project = session.project();
        return !project.narrativeLayerAssignments().isEmpty()
                || !project.theatre().voiceRoleAliases().isEmpty()
                || !project.theatre().intervencionesVisuales().isEmpty();
    }

    public static NarrationScriptDocument scriptStartingAt(NarrationScriptDocument script, String startSegmentId) {
        String target = startSegmentId == null ? "" : startSegmentId.strip();
        java.util.List<NarrationSegment> suffix = script.segments().stream()
                .dropWhile(segment -> !segment.id().equals(target))
                .filter(NarrationSegment::narratable)
                .toList();
        return new NarrationScriptDocument(script.id(), script.title(), script.language(), script.sourceDocumentTitle(),
                suffix, script.createdAt(), script.updatedAt(), script.notes());
    }

    public static RenderUnitPlan renderUnitPlanStartingAt(RenderUnitPlan fullPlan, NarrationScriptDocument suffixScript, String startSegmentId) {
        if (fullPlan == null) {
            return null;
        }
        java.util.Set<String> allowedSegmentIds = suffixScript.segments().stream()
                .map(NarrationSegment::id)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.List<RenderUnit> units = fullPlan.units().stream()
                .filter(unit -> allowedSegmentIds.contains(unit.segmentId()))
                .toList();
        return new RenderUnitPlan(fullPlan.id() + "-FROM-" + startSegmentId, fullPlan.sourceScriptId(),
                units, fullPlan.defaultSilentVisualDurationSeconds(), java.time.Instant.now());
    }

    public static AudioJobStatusDto safeUiStatus(AudioJobStatusDto status) {
        if (!status.running()) {
            return status;
        }
        return new AudioJobStatusDto(
                status.jobId(), status.documentName(), AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                status.completedSegments(), status.totalSegments(), status.failedSegments(), status.progress(),
                status.currentSegmentId(), status.currentSegmentTitle(), 0L,
                "Job encontrado como activo al reabrir. Se marca como interrumpido y puede reanudarse desde Procesos.",
                status.outputDirectory(), status.finalAudioPath(), status.manifestPath());
    }

    public List<String> diagnosticLabels(Path projectDirectory, AudioJobSnapshot selected) throws IOException {
        Objects.requireNonNull(selected, "selected");
        List<AudioProcessDiagnosticEvent> events = applicationServices.playback().audio()
                .listAudioProcessDiagnostics()
                .list(projectDirectory, selected.jobId());
        if (events.isEmpty()) {
            return List.of("Sin process-diagnostics.jsonl para " + selected.jobId()
                    + ". El mock no genera diagnósticos de proceso.");
        }
        return events.stream()
                .limit(12)
                .map(AudioProcessDiagnosticEvent::compactLabel)
                .toList();
    }

    public List<String> persistedAudioJobLabels(Optional<Path> projectFile) {
        if (projectFile == null || projectFile.isEmpty()) {
            return List.of("Guarda el proyecto para ver historial de jobs persistidos.");
        }
        try {
            Path projectDirectory = projectFile.get().toAbsolutePath().normalize().getParent();
            return persistedJobs(projectDirectory).stream()
                    .map(snapshot -> snapshot.jobId() + " · " + snapshot.state().displayName()
                            + " · " + snapshot.completedSegments() + "/" + snapshot.totalSegments()
                            + " · " + snapshot.recoveryLabel()
                            + " · " + snapshot.message())
                    .toList();
        } catch (IOException ex) {
            return List.of("No se pudo leer historial de jobs: " + ex.getMessage());
        }
    }

    public List<String> persistedAudioJobDetailLabels(Optional<Path> projectFile) {
        if (projectFile == null || projectFile.isEmpty()) {
            return List.of("Guarda el proyecto para ver detalle de jobs persistidos.");
        }
        try {
            Path projectDirectory = projectFile.get().toAbsolutePath().normalize().getParent();
            List<AudioJobSnapshot> snapshots = persistedJobs(projectDirectory);
            if (snapshots.isEmpty()) {
                return List.of("No hay jobs persistidos todavía.");
            }
            AudioJobSnapshot selected = snapshots.stream().filter(AudioJobSnapshot::resumable).findFirst().orElse(snapshots.get(0));
            return jobDetailLines(selected);
        } catch (IOException ex) {
            return List.of("No se pudo leer detalle de jobs: " + ex.getMessage());
        }
    }

    public List<String> audioProcessDiagnosticLabels(Optional<Path> projectFile) {
        if (projectFile == null || projectFile.isEmpty()) {
            return List.of("Guarda el proyecto para ver diagnósticos del proceso TTS.");
        }
        try {
            Path projectDirectory = projectFile.get().toAbsolutePath().normalize().getParent();
            List<AudioJobSnapshot> snapshots = persistedJobs(projectDirectory);
            if (snapshots.isEmpty()) {
                return List.of("No hay diagnósticos de proceso todavía.");
            }
            AudioJobSnapshot selected = snapshots.stream().filter(AudioJobSnapshot::resumable).findFirst().orElse(snapshots.get(0));
            return diagnosticLabels(projectDirectory, selected);
        } catch (IOException ex) {
            return List.of("No se pudieron leer diagnósticos de proceso: " + ex.getMessage());
        }
    }

    public void registerCompletedAudioAssets(ProjectSession session, AudioJobStatusDto status, Runnable onUpdated) {
        com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject project = session.project();
        if (status == null) { onUpdated.run(); return; }
        if (!status.finalAudioPath().isBlank()) {
            project = applicationServices.project().assets().registerProjectAsset().register(project, new ProjectAssetReference(
                    "AUD-FINAL-" + status.jobId(), ProjectAssetKind.AUDIO_FINAL,
                    "Podcast " + status.jobId(), status.finalAudioPath(), "audio/wav",
                    "Audio final generado desde la lectura preparada del documento", "", ""));
        }
        if (!status.manifestPath().isBlank()) {
            project = applicationServices.project().assets().registerProjectAsset().register(project, new ProjectAssetReference(
                    "AUD-MANIFEST-" + status.jobId(), ProjectAssetKind.AUDIO_MANIFEST,
                    "Manifest de audio " + status.jobId(), status.manifestPath(), "application/json",
                    "Manifest de audio por segmentos", "", ""));
        }
        ProjectMetadata metadata = project.metadata()
                .withKind(ProjectKind.AUDIO_PROJECT)
                .withStatus(ProjectStatus.AUDIO_READY);
        session.replaceProject(project.withMetadata(metadata).withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name()), true);
        onUpdated.run();
    }

    public AudioJobStatusDto loadLatestPersistedAudioStatus(Optional<Path> projectFile) {
        if (projectFile == null || projectFile.isEmpty()) {
            return AudioJobStatusDto.idle();
        }
        try {
            Path projectDirectory = projectFile.get().toAbsolutePath().normalize().getParent();
            List<AudioJobSnapshot> snapshots = persistedJobs(projectDirectory);
            if (snapshots.isEmpty()) {
                return AudioJobStatusDto.idle();
            }
            return safeUiStatus(AudioJobSnapshotMapper.toStatusDto(snapshots.get(0), projectDirectory));
        } catch (IOException ex) {
            return AudioJobStatusDto.idle();
        }
    }

    public String cancelActiveAudioJob(AudioJobStatusDto activeStatus, java.util.function.Consumer<AudioJobStatusDto> onCancelled) {
        String jobId = activeStatus == null ? "" : activeStatus.jobId();
        if (jobId == null || jobId.isBlank()) {
            return "No hay job de audio activo para cancelar.";
        }
        boolean cancelled = cancel(jobId);
        if (cancelled && activeStatus != null) {
            onCancelled.accept(new AudioJobStatusDto(
                    activeStatus.jobId(), activeStatus.documentName(), AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                    activeStatus.completedSegments(), activeStatus.totalSegments(), activeStatus.failedSegments(),
                    activeStatus.progress(), activeStatus.currentSegmentId(), activeStatus.currentSegmentTitle(),
                    activeStatus.estimatedRemainingSeconds(),
                    "Generación cancelada por el usuario; cancelación segura solicitada. Puedes seguir generando desde la barra de estado si el job es recuperable.",
                    activeStatus.outputDirectory(), activeStatus.finalAudioPath(), activeStatus.manifestPath()));
        }
        return cancelled
                ? "Cancelación solicitada para " + jobId + ". Se conservarán segmentos completados cuando el job sea reanudable."
                : "No se encontró el job " + jobId + " para cancelar.";
    }

    public boolean cancelActiveAudioJobSilently(AudioJobStatusDto activeStatus, java.util.function.Consumer<AudioJobStatusDto> onCancelled) {
        String jobId = activeStatus == null ? "" : activeStatus.jobId();
        if (jobId == null || jobId.isBlank()) {
            return false;
        }
        boolean cancelled = cancel(jobId);
        if (cancelled && activeStatus != null) {
            onCancelled.accept(new AudioJobStatusDto(
                    activeStatus.jobId(), activeStatus.documentName(), AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                    activeStatus.completedSegments(), activeStatus.totalSegments(), activeStatus.failedSegments(),
                    activeStatus.progress(), activeStatus.currentSegmentId(), activeStatus.currentSegmentTitle(),
                    activeStatus.estimatedRemainingSeconds(),
                    "Generación cancelada por el usuario; cancelación segura solicitada.",
                    activeStatus.outputDirectory(), activeStatus.finalAudioPath(), activeStatus.manifestPath()));
        }
        return cancelled;
    }

    public void deletePersistedAudioForFreshVoice(
            Optional<Path> projectFile,
            AudioJobStatusDto activeStatus,
            java.util.function.Consumer<AudioJobStatusDto> onCancelled,
            Runnable onResetPlaybackState,
            Runnable onSetAudioJobIdle,
            Runnable onSetAudioJobNotRunning) {
        cancelActiveAudioJobSilently(activeStatus, onCancelled);
        onResetPlaybackState.run();
        onSetAudioJobIdle.run();
        onSetAudioJobNotRunning.run();
        if (projectFile != null && projectFile.isPresent()) {
            try {
                deletePersistedJobs(projectFile.get().toAbsolutePath().normalize().getParent());
            } catch (IOException ignored) {
            }
        }
    }
}
