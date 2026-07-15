package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        Set<String> audioIds = completedAudioIds(jobs, projectDirectory);
        if (audioIds.isEmpty()) {
            return false;
        }
        for (NarrationSegment segment : script.segments()) {
            if (segment.narratable() && !hasAudioForSegment(audioIds, segment.id())) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> completedAudioIds(List<AudioJobSnapshot> jobs, Path projectDirectory) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (AudioJobSnapshot job : jobs) {
            for (AudioSegmentSnapshot segment : job.segments()) {
                if (usableAudio(projectDirectory, segment)) {
                    result.add(segment.segmentId());
                }
            }
        }
        return result;
    }

    private static boolean hasAudioForSegment(Set<String> audioIds, String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return false;
        }
        if (audioIds.contains(segmentId)) {
            return true;
        }
        String unitPrefix = segmentId + "-";
        return audioIds.stream().anyMatch(id -> id.startsWith(unitPrefix));
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
