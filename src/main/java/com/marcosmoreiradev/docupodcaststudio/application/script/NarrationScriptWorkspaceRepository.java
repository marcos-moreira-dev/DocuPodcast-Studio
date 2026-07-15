package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Writes the editable narration script representation into the project folder. */
public interface NarrationScriptWorkspaceRepository {
    MaterializedNarrationScript materialize(NarrationScriptDocument script, Path projectFile) throws IOException;

    Optional<NarrationScriptDocument> load(Path projectFile) throws IOException;
}
