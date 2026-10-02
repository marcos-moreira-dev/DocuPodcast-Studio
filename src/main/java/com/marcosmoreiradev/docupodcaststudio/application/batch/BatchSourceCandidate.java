package com.marcosmoreiradev.docupodcaststudio.application.batch;

import java.nio.file.Path;

public record BatchSourceCandidate(Path absolutePath, String relativePath, String format, String sha256,
                                   long bytes, int embeddedMediaCount) { }
