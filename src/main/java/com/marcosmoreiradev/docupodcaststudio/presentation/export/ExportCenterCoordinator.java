package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessStatus;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectExportTargetCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Builds the central creative-export surface from application readiness. */
public final class ExportCenterCoordinator {
    private final ProjectExportTargetCatalog targetCatalog;

    public ExportCenterCoordinator() {
        this(new ProjectExportTargetCatalog());
    }

    ExportCenterCoordinator(ProjectExportTargetCatalog targetCatalog) {
        this.targetCatalog = Objects.requireNonNull(targetCatalog, "targetCatalog");
    }

    public ExportCenterState stateFrom(DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(viewModel, "viewModel");
        ProjectMode mode = viewModel.currentProjectModeProperty().get();
        ExportReadinessReport readiness = readinessReport(viewModel);
        return new ExportCenterState(
                mode,
                viewModel.projectOpenProperty().get(),
                readiness,
                processJobs(viewModel));
    }

    public List<ExportTargetPresentation> targets(ExportCenterState state) {
        ExportCenterState current = state == null
                ? new ExportCenterState(ProjectMode.defaultMode(), false, null, List.of())
                : state;
        if (!current.projectOpen() || current.readinessReport() == null) {
            return List.of();
        }
        Map<ExportableArtifactKind, ExportReadinessItem> items = itemsByKind(current.readinessReport());
        ArrayList<ExportTargetPresentation> targets = new ArrayList<>();
        for (ExportableArtifactKind kind : targetCatalog.creativeTargets(current.mode())) {
            ExportReadinessItem item = items.get(kind);
            if (item != null) {
                targets.add(withProcessSummary(targetFor(kind, item), current, relatedProcessKinds(kind)));
            }
        }
        return List.copyOf(targets);
    }

    /** Readiness for one visible creative target; hidden auxiliary artifacts never participate. */
    public Optional<UserVisibleDecision> readinessDecision(ExportCenterState state, AppCommandId commandId) {
        if (commandId == null) {
            return Optional.empty();
        }
        return targets(state).stream()
                .filter(target -> target.commandId() == commandId)
                .findFirst()
                .map(target -> {
                    String message = target.detail();
                    if (!target.executable()) {
                        return UserVisibleDecision.warning(
                                "Exportacion no disponible: " + target.title(), message);
                    }
                    if ("Listo con advertencias".equals(target.readinessLabel())) {
                        return UserVisibleDecision.warning(
                                "Exportacion disponible con advertencias: " + target.title(), message);
                    }
                    return UserVisibleDecision.informationDialog(
                            "Todo está bien", target.title() + " está disponible para exportar.\n\n" + message);
                });
    }

    private ExportReadinessReport readinessReport(DocuPodcastShellViewModel viewModel) {
        return viewModel.currentProject()
                .map(project -> {
                    try {
                        StoryboardDocument storyboard = viewModel.currentStoryboardProperty().get();
                        return viewModel.exportWorkspace().export().inspectExportReadiness().inspect(
                                project,
                                viewModel.currentProjectFile().orElse(null),
                                viewModel.currentScriptProperty().get(),
                                storyboard,
                                audioJobs(viewModel));
                    } catch (RuntimeException ex) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private static Map<ExportableArtifactKind, ExportReadinessItem> itemsByKind(ExportReadinessReport report) {
        EnumMap<ExportableArtifactKind, ExportReadinessItem> items = new EnumMap<>(ExportableArtifactKind.class);
        for (ExportReadinessItem item : report.items()) {
            items.put(item.kind(), item);
        }
        return items;
    }

    private static List<AudioJobSnapshot> audioJobs(DocuPodcastShellViewModel viewModel) {
        try {
            Path projectDirectory = viewModel.currentProjectDirectory().orElse(null);
            if (projectDirectory == null) {
                return List.of();
            }
            return viewModel.playbackWorkspace().audio().listPersistedAudioJobs().list(projectDirectory);
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private static List<ProcessJobSnapshot> processJobs(DocuPodcastShellViewModel viewModel) {
        try {
            Path projectDirectory = viewModel.currentProjectDirectory().orElse(null);
            if (projectDirectory == null) {
                return List.of();
            }
            return viewModel.exportWorkspace().process().listProcessJobs().listPersisted(projectDirectory);
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    private static ExportTargetPresentation withProcessSummary(
            ExportTargetPresentation base,
            ExportCenterState state,
            ProcessJobKind... relatedKinds
    ) {
        List<ProcessJobKind> kinds = relatedKinds == null ? List.of() : Arrays.asList(relatedKinds);
        List<ProcessJobSnapshot> related = state.processJobs().stream()
                .filter(job -> kinds.contains(job.kind()))
                .toList();
        return new ExportTargetPresentation(
                base.commandId(),
                base.title(),
                base.format(),
                base.targetHint(),
                base.readinessLabel(),
                base.detail(),
                base.executable(),
                processSummary(related));
    }

    private static String processSummary(List<ProcessJobSnapshot> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return "Sin procesos relacionados registrados.";
        }
        long running = jobs.stream().filter(ProcessJobSnapshot::running).count();
        long failed = jobs.stream().filter(job -> job.state() == ProcessJobState.FAILED).count();
        long recoverable = jobs.stream().filter(ProcessJobSnapshot::recoverable).count();
        if (failed > 0) {
            return failed + " proceso(s) fallido(s), " + recoverable + " recuperable(s). Usa Ver estado antes de exportar.";
        }
        if (running > 0) {
            return running + " proceso(s) en curso relacionado(s) con esta salida.";
        }
        return jobs.size() + " proceso(s) relacionado(s) registrado(s).";
    }

    private static ExportTargetPresentation targetFor(ExportableArtifactKind kind, ExportReadinessItem item) {
        boolean executable = item.exportable() || audioOnlyRemediable(item);
        return new ExportTargetPresentation(
                commandFor(kind),
                titleFor(kind),
                formatFor(kind, item),
                targetHintFor(kind, item),
                readinessLabel(item),
                detailFor(item, executable),
                executable);
    }

    private static AppCommandId commandFor(ExportableArtifactKind kind) {
        return switch (kind) {
            case PODCAST_WAV -> AppCommandId.EXPORT_PODCAST_WAV;
            case DOCUMENT_TEXT_AUDIO_VIDEO -> AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO;
            case FINAL_VIDEO_MP4 -> AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE;
            case THEATRE_WORK_VIDEO -> AppCommandId.EXPORT_THEATRE_WORK;
            case THEATRE_SPATIAL_MAP_VIDEO -> AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW;
            case THEATRE_PORTION_VIDEO -> AppCommandId.EXPORT_THEATRE_PORTION;
            default -> throw new IllegalArgumentException("Unsupported creative export target: " + kind);
        };
    }

    private static String titleFor(ExportableArtifactKind kind) {
        return switch (kind) {
            case PODCAST_WAV -> "Audio final";
            case DOCUMENT_TEXT_AUDIO_VIDEO -> "Video de estudio documental";
            case FINAL_VIDEO_MP4 -> "Video narrativo final";
            case THEATRE_WORK_VIDEO -> "Video teatral limpio";
            case THEATRE_SPATIAL_MAP_VIDEO -> "Mapa teatral";
            case THEATRE_PORTION_VIDEO -> "Acto o escena teatral";
            default -> kind.displayName();
        };
    }

    private static String formatFor(ExportableArtifactKind kind, ExportReadinessItem item) {
        return switch (kind) {
            case PODCAST_WAV -> "WAV / MP3 / AAC";
            case DOCUMENT_TEXT_AUDIO_VIDEO -> "MP4 texto+audio";
            default -> item.format().displayName();
        };
    }

    private static String targetHintFor(ExportableArtifactKind kind, ExportReadinessItem item) {
        if (kind == ExportableArtifactKind.FINAL_VIDEO_MP4 && "video-final.mp4".equals(item.targetHint())) {
            return "video-narrativo-final.mp4";
        }
        return item.targetHint();
    }

    private static String readinessLabel(ExportReadinessItem item) {
        if (item.status() == ExportReadinessStatus.EXPORTABLE) {
            return "Listo";
        }
        if (item.status() == ExportReadinessStatus.EXPORTABLE_CON_ADVERTENCIAS) {
            return "Listo con advertencias";
        }
        return item.missingRequirements().isEmpty()
                ? item.status().displayName()
                : item.missingRequirements().getFirst();
    }

    private static String detailFor(ExportReadinessItem item, boolean executable) {
        ArrayList<String> parts = new ArrayList<>();
        if (!item.evidence().isEmpty()) {
            parts.add("Evidencia: " + join(item.evidence()));
        }
        if (!item.missingRequirements().isEmpty()) {
            parts.add("Faltantes: " + join(item.missingRequirements()));
        }
        if (audioOnlyRemediable(item) && executable) {
            parts.add("Puede generar audio faltante solo con confirmacion del usuario.");
        }
        if (!item.limitations().isEmpty()) {
            parts.add("Limitaciones: " + join(item.limitations()));
        }
        return parts.isEmpty() ? "Sin detalle adicional." : String.join(" ", parts);
    }

    private static String join(List<String> values) {
        return String.join(" ", values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList());
    }

    private static boolean audioOnlyRemediable(ExportReadinessItem item) {
        return item.blocked()
                && !item.missingRequirements().isEmpty()
                && item.missingRequirements().stream().allMatch(ExportCenterCoordinator::audioRequirement);
    }

    private static boolean audioRequirement(String value) {
        String text = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return text.contains("falta audio")
                || text.contains("genera audio")
                || text.contains("job de audio")
                || text.contains("wav final")
                || text.contains("segmentos wav");
    }

    private static ProcessJobKind[] relatedProcessKinds(ExportableArtifactKind kind) {
        return switch (kind) {
            case PODCAST_WAV -> new ProcessJobKind[] {ProcessJobKind.TTS_AUDIO, ProcessJobKind.FINAL_EXPORT};
            case DOCUMENT_TEXT_AUDIO_VIDEO -> new ProcessJobKind[] {
                    ProcessJobKind.TTS_AUDIO, ProcessJobKind.VIDEO_RENDER, ProcessJobKind.FINAL_EXPORT};
            case FINAL_VIDEO_MP4 -> new ProcessJobKind[] {
                    ProcessJobKind.TTS_AUDIO, ProcessJobKind.VISUAL_GENERATION,
                    ProcessJobKind.VIDEO_RENDER, ProcessJobKind.FINAL_EXPORT};
            case THEATRE_WORK_VIDEO -> new ProcessJobKind[] {
                    ProcessJobKind.TTS_AUDIO, ProcessJobKind.VISUAL_GENERATION,
                    ProcessJobKind.VIDEO_RENDER, ProcessJobKind.FINAL_EXPORT};
            case THEATRE_SPATIAL_MAP_VIDEO, THEATRE_PORTION_VIDEO -> new ProcessJobKind[] {
                    ProcessJobKind.TTS_AUDIO, ProcessJobKind.VIDEO_RENDER, ProcessJobKind.FINAL_EXPORT};
            default -> new ProcessJobKind[] {ProcessJobKind.FINAL_EXPORT};
        };
    }
}
