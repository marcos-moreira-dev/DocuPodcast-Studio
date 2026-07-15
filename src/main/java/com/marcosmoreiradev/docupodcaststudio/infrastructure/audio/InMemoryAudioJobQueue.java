package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory queue used by the mock audio gateway. Persistence comes in a later tanda. */
public final class InMemoryAudioJobQueue {
    private final Map<String, AudioJobStatusDto> statuses = new ConcurrentHashMap<>();

    public void update(AudioJobStatusDto status) {
        if (status != null && !status.jobId().isBlank()) {
            statuses.put(status.jobId(), status);
        }
    }

    public Optional<AudioJobStatusDto> byId(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(statuses.get(jobId.strip()));
    }

    public List<AudioJobStatusDto> list() {
        ArrayList<AudioJobStatusDto> list = new ArrayList<>(statuses.values());
        list.sort(Comparator.comparing(AudioJobStatusDto::jobId).reversed());
        return List.copyOf(list);
    }
}
