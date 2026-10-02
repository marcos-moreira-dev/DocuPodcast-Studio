package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface TheatreImportStateRepository {
    Optional<TheatreImportState> open(Path projectRoot) throws IOException;
    void save(TheatreImportState state, Path projectRoot) throws IOException;
}
