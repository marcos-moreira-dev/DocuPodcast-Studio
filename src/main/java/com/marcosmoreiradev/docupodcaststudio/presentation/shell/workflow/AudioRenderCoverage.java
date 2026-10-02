package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Checks whether persisted jobs cover the current script, including RenderUnit audio ids. */
public final class AudioRenderCoverage {
    private AudioRenderCoverage() {
    }

    public static boolean hasAllChunksRendered(NarrationScriptDocument script,
                                               List<AudioJobSnapshot> jobs,
                                               Path projectDirectory) {
        if (script == null || script.empty() || jobs == null || jobs.isEmpty()) {
            return false;
        }
        if (projectDirectory == null) return false;
        List<AudioGenerationUnit> units = script.segments().stream()
                .filter(NarrationSegment::narratable)
                .map(AudioGenerationUnit::fromSegment).toList();
        return new com.marcosmoreiradev.docupodcaststudio.application.audio.ReusableAudioCoverage()
                .resolve(units, jobs, projectDirectory).complete();
    }

    private static List<AudioSegmentSnapshot> completedAudio(List<AudioJobSnapshot> jobs, Path projectDirectory) {
        java.util.ArrayList<AudioSegmentSnapshot> result = new java.util.ArrayList<>();
        for (AudioJobSnapshot job : jobs) {
            for (AudioSegmentSnapshot segment : job.segments()) {
                if (usableAudio(projectDirectory, segment)) {
                    result.add(segment);
                }
            }
        }
        return result;
    }

    private static boolean hasCurrentAudioForSegment(List<AudioSegmentSnapshot> audio,
                                                     NarrationSegment narration) {
        String segmentId = narration.id();
        if (segmentId == null || segmentId.isBlank()) {
            return false;
        }
        var expected = AudioGenerationUnit.fromSegment(narration).sourceFingerprint();
        String unitPrefix = segmentId + "-";
        return audio.stream().anyMatch(clip -> (clip.segmentId().equals(segmentId)
                || clip.segmentId().startsWith(unitPrefix))
                && (manual(clip) || clip.reusableFor(expected)));
    }

    private static boolean manual(AudioSegmentSnapshot segment) {
        return segment.audioRelativePath().toLowerCase(java.util.Locale.ROOT).endsWith("-manual.wav");
    }

    private static boolean usableAudio(Path projectDirectory, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) {
            return false;
        }
        if (projectDirectory == null) {
            return true;
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path audio = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return audio.startsWith(root) && Files.isRegularFile(audio);
    }
}
