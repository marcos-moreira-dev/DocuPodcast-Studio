package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Regenerates one theatre intervention and atomically patches the current playable job. */
public final class TheatreInterventionAudioRegenerationWorkflow {
    public void start(WorkspaceApplicationServices services, AudioWorkflowCoordinator audioWorkflow,
                      PlayableAudioJobSelector selector, ProjectSession session,
                      NarrationScriptDocument script, String activeJobId, NarrationSegment segment,
                      Consumer<AudioJobStatusDto> progress, Consumer<AudioJobSnapshot> success,
                      Consumer<Throwable> failure) throws IOException {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(segment, "segment");
        Path projectDirectory = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de regenerar audio."))
                .toAbsolutePath().normalize().getParent();
        List<AudioJobSnapshot> snapshots = audioWorkflow.persistedJobs(projectDirectory);
        AudioJobSnapshot target = selector.select(snapshots, activeJobId, script)
                .orElseThrow(() -> new IOException("No hay un job reproducible que actualizar."));
        if (target.segments().stream().anyMatch(item -> belongsTo(item, segment.id()) && manual(item)))
            throw new IOException("La intervencion usa audio humano importado. Retiralo antes de volver a TTS.");
        AudioGenerationRequest request = audioWorkflow.buildInterventionRegenerationRequest(
                session, script, projectDirectory, session.title() + " - regeneracion puntual", segment.id());
        List<String> expected = request.generationUnits().stream().map(unit -> unit.id()).toList();
        services.playback().audio().regenerateTheatreInterventionAudio()
                .regenerate(request, target, expected, progress, success, failure);
    }

    private static boolean belongsTo(AudioSegmentSnapshot item, String segmentId) {
        return item.segmentId().equals(segmentId) || item.segmentId().startsWith(segmentId + "-U");
    }

    private static boolean manual(AudioSegmentSnapshot item) {
        return item.audioRelativePath().replace('\\', '/').endsWith("-manual.wav");
    }
}
