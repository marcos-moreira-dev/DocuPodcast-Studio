package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Selects the persisted audio job that is most useful for playback.
 *
 * <p>Runtime playback must not blindly reuse a stale early manifest. In long documents it is common
 * to have several persisted jobs or an early manifest created while only one/two chunks were ready.
 * This selector ranks jobs by: explicit active job, completed audio that matches the current script,
 * completed count and recency.</p>
 */
public final class PlayableAudioJobSelector {
    public Optional<AudioJobSnapshot> select(List<AudioJobSnapshot> snapshots, String preferredJobId, NarrationScriptDocument script) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Optional.empty();
        }
        Set<String> scriptIds = scriptIds(script);
        String preferred = preferredJobId == null ? "" : preferredJobId.strip();
        return snapshots.stream()
                .filter(this::hasPlayableAudio)
                .max((left, right) -> Long.compare(score(left, preferred, scriptIds), score(right, preferred, scriptIds)));
    }

    public String summary(AudioJobSnapshot snapshot, NarrationScriptDocument script) {
        if (snapshot == null) {
            return "sin job reproducible";
        }
        Set<String> scriptIds = scriptIds(script);
        long matching = snapshot.segments().stream()
                .filter(this::playableSegment)
                .filter(segment -> scriptIds.isEmpty() || matchesScriptSegment(segment.segmentId(), scriptIds))
                .count();
        return snapshot.jobId() + " · " + matching + "/" + snapshot.completedSegments()
                + " fragmentos compatibles · actualizado " + snapshot.updatedAt();
    }

    private long score(AudioJobSnapshot snapshot, String preferredJobId, Set<String> scriptIds) {
        long preferredScore = !preferredJobId.isBlank() && preferredJobId.equals(snapshot.jobId()) ? 100_000L : 0L;
        long matchingCompleted = snapshot.segments().stream()
                .filter(this::playableSegment)
                .filter(segment -> scriptIds.isEmpty() || matchesScriptSegment(segment.segmentId(), scriptIds))
                .count();
        long completed = snapshot.segments().stream().filter(this::playableSegment).count();
        long recency = snapshot.updatedAt() == null ? 0L : Math.max(0L, snapshot.updatedAt().getEpochSecond());
        return preferredScore + matchingCompleted * 1_000_000L + completed * 1_000L + Math.min(recency, 999L);
    }

    private boolean hasPlayableAudio(AudioJobSnapshot snapshot) {
        return snapshot != null && snapshot.segments().stream().anyMatch(this::playableSegment);
    }

    private boolean playableSegment(AudioSegmentSnapshot segment) {
        return segment != null && segment.completed() && !segment.audioRelativePath().isBlank();
    }

    private Set<String> scriptIds(NarrationScriptDocument script) {
        HashSet<String> ids = new HashSet<>();
        if (script == null) {
            return ids;
        }
        for (NarrationSegment segment : script.segments()) {
            ids.add(segment.id());
        }
        return ids;
    }

    private boolean matchesScriptSegment(String audioSegmentId, Set<String> scriptIds) {
        String id = audioSegmentId == null ? "" : audioSegmentId.strip();
        if (id.isBlank()) {
            return false;
        }
        if (scriptIds.contains(id)) {
            return true;
        }
        return scriptIds.stream().anyMatch(scriptId -> id.startsWith(scriptId + "-U"));
    }
}
