package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;

import java.io.IOException;
import java.nio.file.Path;

/** Reads an external theatre package without mutating it. */
public interface TheatrePackageScanner {
    TheatrePackageInventory scan(Path sourceRoot) throws IOException;
}
