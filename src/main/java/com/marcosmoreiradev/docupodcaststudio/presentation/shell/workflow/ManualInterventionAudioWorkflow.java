package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;
import com.marcosmoreiradev.docupodcaststudio.application.recording.RecordingActionPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Coordinates manual intervention recordings without growing the shell view-model. */
public final class ManualInterventionAudioWorkflow {
    public List<AudioInputDevice> inputDevices(WorkspaceApplicationServices services) {
        try { return services.playback().recording().startAudioRecording().inputDevices(); }
        catch (RuntimeException ex) { return List.of(AudioInputDevice.systemDefault()); }
    }

    public Optional<NarrationSegment> segmentForBlock(NarrationScriptDocument script, String blockId,
                                                      Optional<NarrationSegment> fallback) {
        if (script != null && !script.empty() && blockId != null && !blockId.isBlank()) {
            Optional<NarrationSegment> linked = script.segments().stream()
                    .filter(segment -> segment.sourceBlockIds().stream().anyMatch(blockId::equals))
                    .findFirst();
            if (linked.isPresent()) return linked;
        }
        return fallback == null ? Optional.empty() : fallback;
    }

    public Path startRecording(WorkspaceApplicationServices services, ProjectSession session, NarrationSegment segment,
                               String inputDeviceId) throws IOException {
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de grabar audio manual."));
        ScriptTextRange range = new ScriptTextRange(segment.id(), 0, segment.narrationText().length());
        RecordingActionPlan plan = new RecordingActionPlan(RecordingPurpose.HUMAN_VOICE_FOR_TEXT, range,
                segment.id() + "-manual.wav", "Grabacion manual para " + segment.id(), true, true);
        return services.playback().recording().startAudioRecording()
                .startInDirectory(projectFile.toAbsolutePath().normalize().getParent(), plan, inputDeviceId);
    }

    public AudioJobSnapshot applyRecording(WorkspaceApplicationServices services, AudioWorkflowCoordinator audioWorkflow,
                                           PlayableAudioJobSelector selector, ProjectSession session,
                                           NarrationScriptDocument script, String activeJobId,
                                           NarrationSegment segment, String displayName, Path recorded) throws IOException {
        Path projectDirectory = projectDirectory(session);
        AudioJobSnapshot target = targetSnapshot(audioWorkflow, selector, projectDirectory, script, activeJobId).orElse(null);
        return services.playback().audio().manualAudioSegmentJob().apply(projectDirectory, target, session.title(), segment.id(),
                displayName(displayName, segment), recorded, baseline(audioWorkflow, session, script, projectDirectory));
    }

    public Path playableAudioForSegment(AudioWorkflowCoordinator audioWorkflow, PlayableAudioJobSelector selector,
                                        Path projectDirectory, NarrationScriptDocument script, String activeJobId,
                                        String segmentId) throws IOException {
        AudioJobSnapshot snapshot = manualSnapshotForSegment(audioWorkflow, selector, projectDirectory, script,
                activeJobId, segmentId, false).orElseThrow(() -> new IOException("Esa intervencion todavia no tiene audio reproducible."));
        return audioFileForSegment(projectDirectory, snapshot, segmentId)
                .orElseThrow(() -> new IOException("No se encontro el WAV aplicado para " + segmentId + "."));
    }

    public AudioJobSnapshot deleteManualAudio(WorkspaceApplicationServices services, AudioWorkflowCoordinator audioWorkflow,
                                             PlayableAudioJobSelector selector, ProjectSession session,
                                             NarrationScriptDocument script, String activeJobId,
                                             NarrationSegment segment, String displayName) throws IOException {
        Path projectDirectory = projectDirectory(session);
        AudioJobSnapshot target = manualSnapshotForSegment(audioWorkflow, selector, projectDirectory, script,
                activeJobId, segment.id(), true)
                .orElseThrow(() -> new IOException("No hay audio manual aplicado para " + segment.id() + "."));
        return services.playback().audio().manualAudioSegmentJob().delete(projectDirectory, target, segment.id(),
                displayName(displayName, segment), baseline(audioWorkflow, session, script, projectDirectory));
    }

    private Optional<AudioJobSnapshot> targetSnapshot(AudioWorkflowCoordinator audioWorkflow, PlayableAudioJobSelector selector,
                                                      Path projectDirectory, NarrationScriptDocument script,
                                                      String activeJobId) throws IOException {
        List<AudioJobSnapshot> snapshots = audioWorkflow.persistedJobs(projectDirectory);
        Optional<AudioJobSnapshot> playable = selector.select(snapshots, activeJobId, script);
        return playable.isPresent() ? playable : audioWorkflow.selectedSnapshot(snapshots, activeJobId);
    }

    private Optional<AudioJobSnapshot> manualSnapshotForSegment(AudioWorkflowCoordinator audioWorkflow,
                                                               PlayableAudioJobSelector selector,
                                                               Path projectDirectory, NarrationScriptDocument script,
                                                               String activeJobId, String segmentId,
                                                               boolean requireManual) throws IOException {
        List<AudioJobSnapshot> snapshots = audioWorkflow.persistedJobs(projectDirectory);
        String manualSuffix = "/" + segmentId + "-manual.wav";
        Optional<AudioJobSnapshot> manual = snapshots.stream()
                .filter(snapshot -> snapshot.segments().stream().anyMatch(segment -> segment.segmentId().equals(segmentId)
                        && segment.completed() && segment.audioRelativePath().replace('\\', '/').endsWith(manualSuffix)))
                .findFirst();
        if (manual.isPresent() || requireManual) return manual;
        return selector.select(snapshots, activeJobId, script);
    }

    private Optional<Path> audioFileForSegment(Path projectDirectory, AudioJobSnapshot snapshot, String segmentId) {
        Path root = projectDirectory.toAbsolutePath().normalize();
        return snapshot.segments().stream()
                .filter(segment -> segment.segmentId().equals(segmentId))
                .filter(AudioSegmentSnapshot::completed)
                .map(AudioSegmentSnapshot::audioRelativePath)
                .filter(path -> path != null && !path.isBlank())
                .map(path -> root.resolve(path).normalize())
                .filter(path -> path.startsWith(root) && Files.isRegularFile(path))
                .findFirst();
    }

    private List<AudioSegmentSnapshot> baseline(AudioWorkflowCoordinator audioWorkflow, ProjectSession session,
                                                NarrationScriptDocument script, Path projectDirectory) {
        if (script == null || script.empty()) return List.of();
        try {
            AudioGenerationRequest request = audioWorkflow.buildGenerationRequest(session, script, projectDirectory, session.title());
            return request.generationUnits().stream()
                    .map(unit -> AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()))
                    .toList();
        } catch (RuntimeException ex) {
            return script.segments().stream()
                    .filter(NarrationSegment::narratable)
                    .map(segment -> AudioSegmentSnapshot.pending(segment.id(), segment.title()))
                    .toList();
        }
    }

    private static Path projectDirectory(ProjectSession session) throws IOException {
        return session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de modificar audio manual."))
                .toAbsolutePath().normalize().getParent();
    }

    public static String displayName(String displayName, NarrationSegment segment) {
        String label = displayName == null ? "" : displayName.strip();
        if (!label.isBlank()) return label;
        return segment == null || segment.title().isBlank() ? "Intervencion manual" : segment.title();
    }
}
