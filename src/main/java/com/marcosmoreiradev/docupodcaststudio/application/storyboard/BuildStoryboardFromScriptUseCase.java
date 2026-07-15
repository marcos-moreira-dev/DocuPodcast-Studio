package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Objects;

/** Builds the initial storyboard manifest from the current narration script. */
public final class BuildStoryboardFromScriptUseCase {
    public StoryboardDocument build(NarrationScriptDocument script) {
        Objects.requireNonNull(script, "script");
        if (script.empty()) {
            throw new IllegalArgumentException("No se puede crear panel visual desde una lectura vacía");
        }
        return StoryboardDocument.createForScript(script);
    }
}
