package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Repository for persisted audio job snapshots under the project folder. */
public interface AudioJobRepository {
    void save(Path projectDirectory, AudioJobSnapshot snapshot) throws IOException;

    Optional<AudioJobSnapshot> load(Path projectDirectory, String jobId) throws IOException;

    List<AudioJobSnapshot> list(Path projectDirectory) throws IOException;

    void deleteAll(Path projectDirectory) throws IOException;
}
