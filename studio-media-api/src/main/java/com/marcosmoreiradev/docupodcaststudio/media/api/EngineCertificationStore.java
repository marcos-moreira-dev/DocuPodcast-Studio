package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.Optional;

/** Certification storage is operational state and must remain outside project files. */
public interface EngineCertificationStore {
    Optional<EngineCertificationRecord> find(EngineId engineId, String modelId) throws IOException;

    void save(EngineCertificationRecord record) throws IOException;

    void invalidate(EngineId engineId, String modelId) throws IOException;

    static EngineCertificationStore none() {
        return new EngineCertificationStore() {
            @Override public Optional<EngineCertificationRecord> find(
                    EngineId engineId, String modelId) { return Optional.empty(); }
            @Override public void save(EngineCertificationRecord record) { }
            @Override public void invalidate(EngineId engineId, String modelId) { }
        };
    }
}
