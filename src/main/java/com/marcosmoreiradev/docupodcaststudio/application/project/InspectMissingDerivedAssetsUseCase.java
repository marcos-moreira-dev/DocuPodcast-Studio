package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Detects missing recoverable files without materializing or regenerating them. */
public final class InspectMissingDerivedAssetsUseCase {
    private final ProjectAssetAuthorityPolicy authority = new ProjectAssetAuthorityPolicy();

    public ProjectDerivedAssetReport inspect(DocuPodcastProject project, Path projectFile) {
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) return ProjectDerivedAssetReport.empty();
        ArrayList<String> warnings = new ArrayList<>();
        project.assets().references().stream()
                .filter(asset -> authority.recoverable(asset.kind()))
                .filter(asset -> !Files.isRegularFile(root.resolve(asset.relativePath()).normalize()))
                .forEach(asset -> warnings.add("Derivado ausente " + asset.id()
                        + " (" + asset.kind() + "): " + asset.relativePath()
                        + ". Requiere regeneración cuando vuelva a utilizarse."));
        return new ProjectDerivedAssetReport(warnings);
    }
}
