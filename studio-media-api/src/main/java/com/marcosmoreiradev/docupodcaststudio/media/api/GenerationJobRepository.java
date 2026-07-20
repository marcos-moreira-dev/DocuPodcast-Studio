package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Optional;

/** Persistence port implemented by desktop infrastructure. */
public interface GenerationJobRepository {
    void save(GenerationJobSnapshot snapshot);
    Optional<GenerationJobSnapshot> find(GenerationJobId id);
    List<GenerationJobSnapshot> list();
}
