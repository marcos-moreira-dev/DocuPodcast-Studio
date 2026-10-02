package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;

import java.nio.file.Path;
import java.util.Objects;

public record PreparedTheatreRefresh(
        DocuPodcastProject project,
        Path projectFile,
        Path sourceRoot,
        TheatrePackageInventory inventory,
        TheatreImportState previousState,
        TheatreRefreshPreflightReport preflight
) {
    public PreparedTheatreRefresh {
        project = Objects.requireNonNull(project, "project");
        projectFile = Objects.requireNonNull(projectFile, "projectFile").toAbsolutePath().normalize();
        sourceRoot = Objects.requireNonNull(sourceRoot, "sourceRoot").toAbsolutePath().normalize();
        inventory = Objects.requireNonNull(inventory, "inventory");
        preflight = Objects.requireNonNull(preflight, "preflight");
    }
}
