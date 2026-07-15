package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

/** Asset reference produced when the internal prepared-reading snapshot is written to disk. */
public record MaterializedNarrationScript(ProjectAssetReference narrationScriptAsset) {
}
