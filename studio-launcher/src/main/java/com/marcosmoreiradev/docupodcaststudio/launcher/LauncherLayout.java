package com.marcosmoreiradev.docupodcaststudio.launcher;

import java.nio.file.Path;

record LauncherLayout(Path installationRoot, Path runtimeRoot, String installationSource, String runtimeSource) {
    LauncherLayout {
        installationRoot = installationRoot.toAbsolutePath().normalize();
        runtimeRoot = runtimeRoot.toAbsolutePath().normalize();
    }
}
