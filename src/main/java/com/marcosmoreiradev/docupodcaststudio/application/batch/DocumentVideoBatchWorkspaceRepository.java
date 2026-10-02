package com.marcosmoreiradev.docupodcaststudio.application.batch;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchDraft;
import java.io.IOException;
import java.nio.file.Path;
/** Persistent batch setup and production queue. */
public interface DocumentVideoBatchWorkspaceRepository extends DocumentVideoBatchRepository {
    void saveDraft(DocumentVideoBatchDraft draft, Path descriptor) throws IOException;
    DocumentVideoBatchDraft openDraft(Path descriptor) throws IOException;
}
