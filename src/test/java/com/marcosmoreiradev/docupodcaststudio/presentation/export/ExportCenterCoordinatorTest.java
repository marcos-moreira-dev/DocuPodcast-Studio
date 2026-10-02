package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.DocuPodcastExportFormat;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterCoordinatorTest {
    private final ExportCenterCoordinator coordinator = new ExportCenterCoordinator();

    @Test
    void documentaryStudioShowsOnlyAudioAndTextAudioVideo() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(ProjectMode.DOCUMENTARY_STUDIO, report(
                exportable(ExportableArtifactKind.PROJECT_BUNDLE, DocuPodcastExportFormat.DIRECTORY),
                exportable(ExportableArtifactKind.DIAGNOSTIC_REPORT, DocuPodcastExportFormat.MARKDOWN),
                exportable(ExportableArtifactKind.PODCAST_WAV, DocuPodcastExportFormat.WAV),
                exportable(ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.FINAL_VIDEO_MP4, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.STORYBOARD_SUMMARY, DocuPodcastExportFormat.MARKDOWN),
                exportable(ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE, DocuPodcastExportFormat.VIDEO_PACKAGE))));

        assertEquals(List.of(AppCommandId.EXPORT_PODCAST_WAV, AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO), commandIds(targets));
        assertTrue(targets.stream().allMatch(ExportTargetPresentation::executable));
    }

    @Test
    void narrativeVideoShowsAudioAndFinalMp4ThroughNarrativeCommand() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(ProjectMode.NARRATIVE_VIDEO, report(
                exportable(ExportableArtifactKind.PODCAST_WAV, DocuPodcastExportFormat.WAV),
                exportable(ExportableArtifactKind.FINAL_VIDEO_MP4, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE, DocuPodcastExportFormat.VIDEO_PACKAGE))));

        assertEquals(List.of(AppCommandId.EXPORT_PODCAST_WAV, AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE), commandIds(targets));
        assertEquals("video-narrativo-final.mp4", targets.get(1).targetHint());
        assertTrue(targets.stream().allMatch(ExportTargetPresentation::executable));
    }

    @Test
    void theatreProductionShowsOnlyTheatreVideoTargetsBeyondAudio() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(ProjectMode.THEATRE_PRODUCTION, report(
                exportable(ExportableArtifactKind.PODCAST_WAV, DocuPodcastExportFormat.WAV),
                exportable(ExportableArtifactKind.THEATRE_WORK_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.THEATRE_PORTION_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.FINAL_VIDEO_MP4, DocuPodcastExportFormat.MP4))));

        assertEquals(List.of(
                AppCommandId.EXPORT_PODCAST_WAV,
                AppCommandId.EXPORT_THEATRE_WORK,
                AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW,
                AppCommandId.EXPORT_THEATRE_PORTION), commandIds(targets));
        assertEquals(List.of("Audio final", "Video teatral limpio", "Mapa teatral", "Acto o escena teatral"),
                targets.stream().map(ExportTargetPresentation::title).toList());
    }

    @Test
    void nonAudioBlockerExplainsAndDisablesFinalExport() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(ProjectMode.NARRATIVE_VIDEO, report(
                exportable(ExportableArtifactKind.PODCAST_WAV, DocuPodcastExportFormat.WAV),
                blocked(ExportableArtifactKind.FINAL_VIDEO_MP4, "Guarda el proyecto antes de exportar MP4 final."))));

        ExportTargetPresentation video = targets.stream()
                .filter(target -> target.commandId() == AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE)
                .findFirst()
                .orElseThrow();
        assertFalse(video.executable());
        assertEquals("Guarda el proyecto antes de exportar MP4 final.", video.readinessLabel());
    }

    @Test
    void audioOnlyBlockerCanOpenExplicitGenerateAndExportFlow() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(ProjectMode.THEATRE_PRODUCTION, report(
                blocked(ExportableArtifactKind.PODCAST_WAV, "Genera audio antes de exportar podcast WAV."),
                blocked(ExportableArtifactKind.THEATRE_WORK_VIDEO, "Falta audio listo en 1 intervencion(es)."),
                blocked(ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO, "Configura posiciones o acciones teatrales para el mapa espacial."),
                blocked(ExportableArtifactKind.THEATRE_PORTION_VIDEO, "Falta audio listo en 1 intervencion(es)."))));

        ExportTargetPresentation audio = targets.get(0);
        ExportTargetPresentation map = targets.stream()
                .filter(target -> target.commandId() == AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW)
                .findFirst()
                .orElseThrow();
        assertTrue(audio.executable());
        assertTrue(audio.detail().contains("confirmacion"));
        assertFalse(map.executable());
    }

    @Test
    void documentaryMissingNarrationIsPreparableButNotReadyOnlyExecutable() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(
                ProjectMode.DOCUMENTARY_STUDIO, report(
                        blocked(ExportableArtifactKind.PODCAST_WAV,
                                "Prepara la lectura antes de generar o exportar audio final."),
                        blocked(ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO,
                                "Prepara la lectura del documento antes de exportar video documental texto+audio."))));

        assertTrue(targets.stream().allMatch(ExportTargetPresentation::preparable));
        assertFalse(targets.get(1).executable());
    }

    @Test
    void unsavedDocumentaryProjectRemainsHardBlocked() {
        List<ExportTargetPresentation> targets = coordinator.targets(state(
                ProjectMode.DOCUMENTARY_STUDIO, report(
                        blocked(ExportableArtifactKind.PODCAST_WAV,
                                "Guarda el proyecto antes de exportar audio final."),
                        blocked(ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO,
                                "Guarda el proyecto antes de exportar video documental texto+audio."))));

        assertTrue(targets.stream().noneMatch(ExportTargetPresentation::preparable));
    }

    @Test
    void selectedTargetReadinessIgnoresBlockedHiddenSupportArtifacts() {
        ExportCenterState state = state(ProjectMode.THEATRE_PRODUCTION, report(
                exportable(ExportableArtifactKind.PODCAST_WAV, DocuPodcastExportFormat.WAV),
                exportable(ExportableArtifactKind.THEATRE_WORK_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO, DocuPodcastExportFormat.MP4),
                exportable(ExportableArtifactKind.THEATRE_PORTION_VIDEO, DocuPodcastExportFormat.MP4),
                blocked(ExportableArtifactKind.STORYBOARD_SUMMARY, "Panel visual pendiente.")));

        var decision = coordinator.readinessDecision(state, AppCommandId.EXPORT_THEATRE_WORK).orElseThrow();

        assertEquals("Todo está bien", decision.headline());
        assertTrue(decision.requiresDialog());
        assertFalse(decision.message().contains("Panel visual pendiente"));
    }

    private static ExportCenterState state(ProjectMode mode, ExportReadinessReport report) {
        return new ExportCenterState(mode, true, report, List.of());
    }

    private static ExportReadinessReport report(ExportReadinessItem... items) {
        return ExportReadinessReport.from("Demo", List.of(items));
    }

    private static ExportReadinessItem exportable(ExportableArtifactKind kind, DocuPodcastExportFormat format) {
        return ExportReadinessItem.exportable(kind, format, target(kind), List.of("Listo."), List.of("No genera faltantes."));
    }

    private static ExportReadinessItem blocked(ExportableArtifactKind kind, String missing) {
        return ExportReadinessItem.blocked(kind, DocuPodcastExportFormat.MP4, target(kind), List.of(missing), List.of("No genera faltantes."));
    }

    private static String target(ExportableArtifactKind kind) {
        return switch (kind) {
            case PODCAST_WAV -> "audio-final.wav / .mp3 / .aac";
            case DOCUMENT_TEXT_AUDIO_VIDEO -> "estudio-documental-texto-audio.mp4";
            case FINAL_VIDEO_MP4 -> "video-final.mp4";
            case THEATRE_WORK_VIDEO -> "docupodcast-teatro-limpio.mp4";
            case THEATRE_SPATIAL_MAP_VIDEO -> "docupodcast-mapa-teatral.mp4";
            case THEATRE_PORTION_VIDEO -> "docupodcast-porcion-obra.mp4";
            default -> "soporte/";
        };
    }

    private static List<AppCommandId> commandIds(List<ExportTargetPresentation> targets) {
        return targets.stream().map(ExportTargetPresentation::commandId).toList();
    }
}
