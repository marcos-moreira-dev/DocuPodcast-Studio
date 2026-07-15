package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimeline;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.UpsertTheatreAudioTrackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.media.PreparedAudioAsset;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Coordinates theatre background-track editing without adding orchestration to the shell view model. */
public final class TheatreAudioTrackWorkflow {
    public TheatreAudioTrackTimeline timeline(ApplicationServices services,
                                              Optional<ProjectSession> session,
                                              NarrationScriptDocument script,
                                              PlaybackManifest manifest) {
        return session.map(value -> services.theatre().buildAudioTrackTimeline()
                        .execute(value.project(), script, manifest))
                .orElseGet(() -> new TheatreAudioTrackTimeline(List.of()));
    }

    public Optional<String> interventionForSelection(Optional<ProjectSession> session,
                                                     NarrationScriptDocument script,
                                                     String segmentId,
                                                     String blockId) {
        if (session.isEmpty()) return Optional.empty();
        String selectedBlock = normalized(blockId);
        if (selectedBlock.isBlank() && script != null) {
            selectedBlock = script.segmentById(segmentId).flatMap(TheatreAudioTrackWorkflow::firstBlock).orElse("");
        }
        String target = selectedBlock;
        return session.get().project().theatre().intervenciones().stream()
                .filter(intervention -> intervention.blockId().equals(target))
                .map(TheatreProjectLayer.Intervencion::id)
                .findFirst();
    }

    public Optional<String> blockForIntervention(Optional<ProjectSession> session, String interventionId) {
        String target = normalized(interventionId);
        return session.stream().flatMap(value -> value.project().theatre().intervenciones().stream())
                .filter(intervention -> intervention.id().equals(target))
                .map(TheatreProjectLayer.Intervencion::blockId)
                .findFirst();
    }

    public Result save(ApplicationServices services,
                       ProjectSession session,
                       NarrationScriptDocument script,
                       PlaybackManifest manifest,
                       String existingTrackId,
                       String startSegmentId,
                       String startInterventionId,
                       PreparedAudioAsset preparedAudio,
                       double sourceStartSeconds,
                       TheatreProjectLayer.AudioTrackEndMode endMode,
                       double sourceEndSeconds,
                       double volume,
                       boolean gentleFade,
                       boolean replaceConflicts) throws IOException {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(session, "session");
        DocuPodcastProject base = session.project();
        Optional<TheatreProjectLayer.TheatreAudioTrack> existing = trackById(base, existingTrackId);
        String assetId;
        double sourceDuration;
        Path projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de importar una pista de audio."));
        if (preparedAudio != null) {
            assetId = "PENDING-" + preparedAudio.token();
            sourceDuration = preparedAudio.durationSeconds();
        } else {
            TheatreProjectLayer.TheatreAudioTrack current = existing.orElseThrow(() ->
                    new IOException("Elige un archivo de audio antes de guardar la pista."));
            assetId = current.assetId();
            sourceDuration = current.sourceDurationSeconds();
        }
        String trackId = existing.map(TheatreProjectLayer.TheatreAudioTrack::id).orElse("");
        if (trackId.isBlank()) trackId = nextTrackId(base);
        TheatreProjectLayer.TheatreAudioTrack candidate = new TheatreProjectLayer.TheatreAudioTrack(
                trackId, assetId, startInterventionId, startSegmentId, sourceStartSeconds, sourceEndSeconds,
                endMode, volume, sourceDuration, gentleFade);
        UpsertTheatreAudioTrackUseCase.Result validation = services.theatre().upsertAudioTrack()
                .execute(base, script, manifest, candidate, replaceConflicts);
        if (!validation.saved()) {
            return new Result(false, candidate, validation.conflicts().stream().map(entry -> entry.track().id()).toList(),
                    validation.message());
        }
        if (preparedAudio != null) {
            var imported = services.media().importUserMediaAsset().commitPreparedAudio(base, projectFile, preparedAudio);
            base = imported.project();
            assetId = imported.audioAsset().id();
            candidate = new TheatreProjectLayer.TheatreAudioTrack(trackId, assetId, startInterventionId, startSegmentId,
                    sourceStartSeconds, sourceEndSeconds, endMode, volume, sourceDuration, gentleFade);
        }
        UpsertTheatreAudioTrackUseCase.Result outcome = services.theatre().upsertAudioTrack()
                .execute(base, script, manifest, candidate, replaceConflicts);
        session.replaceProject(outcome.project(), true);
        if (outcome.saved()) {
            cleanupUnusedTrackAssets(services, session, projectFile, base, outcome.project(), existing.map(TheatreProjectLayer.TheatreAudioTrack::assetId).stream().toList());
            List<String> replacedAssets = outcome.conflicts().stream().map(entry -> entry.track().assetId()).toList();
            cleanupUnusedTrackAssets(services, session, projectFile, base, outcome.project(), replacedAssets);
        }
        return new Result(outcome.saved(), candidate, outcome.conflicts().stream()
                .map(entry -> entry.track().id()).toList(), outcome.message());
    }

    public Result remove(ApplicationServices services, ProjectSession session, String trackId) throws IOException {
        DocuPodcastProject before = session.project();
        Optional<TheatreProjectLayer.TheatreAudioTrack> removed = trackById(before, trackId);
        DocuPodcastProject updated = services.theatre().removeAudioTrack().execute(before, trackId);
        session.replaceProject(updated, true);
        if (removed.isPresent() && session.projectFile().isPresent()) {
            cleanupUnusedTrackAssets(services, session, session.projectFile().get(), before, updated,
                    List.of(removed.get().assetId()));
        }
        return new Result(true, null, List.of(), "Pista eliminada.");
    }

    private static void cleanupUnusedTrackAssets(ApplicationServices services, ProjectSession session, Path projectFile,
                                                 DocuPodcastProject before, DocuPodcastProject after,
                                                 List<String> candidateAssetIds) throws IOException {
        for (String assetId : candidateAssetIds) {
            if (assetId == null || assetId.isBlank()
                    || after.theatre().audioTracks().stream().anyMatch(track -> track.assetId().equals(assetId))
                    || after.narrativeLayerAssignments().stream().anyMatch(layer -> layer.targetId().equals(assetId))) continue;
            var reference = before.assets().byId(assetId).orElse(null);
            if (reference == null || !(assetId.startsWith("AUDIO-USER-") || assetId.startsWith("AUDIO-NORMALIZED-"))) continue;
            Path root = projectFile.toAbsolutePath().normalize().getParent();
            if (root != null) services.media().importUserMediaAsset().deleteProjectAudio(projectFile, root.resolve(reference.relativePath()).normalize());
            DocuPodcastProject without = session.project().withoutAsset(assetId);
            session.replaceProject(without, true);
        }
    }

    public Optional<String> blockForSegment(NarrationScriptDocument script, String segmentId) {
        return script == null ? Optional.empty() : script.segmentById(normalized(segmentId)).flatMap(TheatreAudioTrackWorkflow::firstBlock);
    }

    public Result saveCandidate(ApplicationServices services,
                                ProjectSession session,
                                NarrationScriptDocument script,
                                PlaybackManifest manifest,
                                TheatreProjectLayer.TheatreAudioTrack candidate,
                                boolean replaceConflicts) {
        UpsertTheatreAudioTrackUseCase.Result outcome = services.theatre().upsertAudioTrack()
                .execute(session.project(), script, manifest, candidate, replaceConflicts);
        session.replaceProject(outcome.project(), true);
        return new Result(outcome.saved(), candidate, outcome.conflicts().stream()
                .map(entry -> entry.track().id()).toList(), outcome.message());
    }

    private static Optional<TheatreProjectLayer.TheatreAudioTrack> trackById(DocuPodcastProject project, String trackId) {
        String target = normalized(trackId);
        return project.theatre().audioTracks().stream().filter(track -> track.id().equals(target)).findFirst();
    }

    private static Optional<String> firstBlock(NarrationSegment segment) {
        return segment.sourceBlockIds().stream().findFirst();
    }

    private static String nextTrackId(DocuPodcastProject project) {
        int suffix = 1;
        while (true) {
            String id = "THEATRE-TRACK-" + String.format(java.util.Locale.ROOT, "%03d", suffix++);
            if (project.theatre().audioTracks().stream().noneMatch(track -> track.id().equals(id))) return id;
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip();
    }

    public record Result(boolean saved,
                         TheatreProjectLayer.TheatreAudioTrack track,
                         List<String> conflictingTrackIds,
                         String message) {
        public Result {
            conflictingTrackIds = conflictingTrackIds == null ? List.of() : List.copyOf(conflictingTrackIds);
            message = message == null ? "" : message.strip();
        }

        public boolean conflict() {
            return !saved && !conflictingTrackIds.isEmpty();
        }
    }
}
