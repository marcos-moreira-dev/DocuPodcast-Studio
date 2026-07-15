package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;
import java.util.Objects;

/** Lists known audio jobs from the current in-memory queue. */
public final class ListAudioGenerationJobsUseCase {
    private final AudioGenerationGateway gateway;

    public ListAudioGenerationJobsUseCase(AudioGenerationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public List<AudioJobStatusDto> list() {
        return gateway.listStatuses();
    }
}
