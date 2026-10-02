package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;
import java.io.IOException;
import java.nio.file.Path;

public interface DocumentVideoBatchRepository {
    void save(DocumentVideoBatchProject project, Path descriptor) throws IOException;
    DocumentVideoBatchProject open(Path descriptor) throws IOException;
}
