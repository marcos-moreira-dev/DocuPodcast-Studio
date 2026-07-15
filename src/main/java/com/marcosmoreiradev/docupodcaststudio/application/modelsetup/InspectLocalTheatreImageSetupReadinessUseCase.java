package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Verifies the local, app-contained setup for theatre image generation. */
public final class InspectLocalTheatreImageSetupReadinessUseCase {
    private final InspectLocalModelFolderUseCase inspectLocalModelFolder;
    private final InspectLocalTheatreImageEngineArtifactsUseCase inspectArtifacts;

    public InspectLocalTheatreImageSetupReadinessUseCase() {
        this(new InspectLocalModelFolderUseCase(), new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public InspectLocalTheatreImageSetupReadinessUseCase(InspectLocalModelFolderUseCase inspectLocalModelFolder) {
        this(inspectLocalModelFolder, new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public InspectLocalTheatreImageSetupReadinessUseCase(InspectLocalModelFolderUseCase inspectLocalModelFolder,
                                                         InspectLocalTheatreImageEngineArtifactsUseCase inspectArtifacts) {
        this.inspectLocalModelFolder = Objects.requireNonNull(inspectLocalModelFolder, "inspectLocalModelFolder");
        this.inspectArtifacts = Objects.requireNonNull(inspectArtifacts, "inspectArtifacts");
    }

    public LocalTheatreImageSetupReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path runtime = root.resolve("tools/image").normalize();
        Path models = root.resolve("models/image").normalize();
        ImageEngineArtifactInspectionReport artifacts = inspectArtifacts.inspect(settings, root);
        ModelInspectionResult inspection = genericInspection(models);
        return new LocalTheatreImageSetupReadinessReport(root, runtime, models, inspection, artifacts,
                artifacts.runtimeReady(), new ArrayList<>(artifacts.missingRequirements()), artifacts.userMessage());
    }

    private ModelInspectionResult genericInspection(Path models) {
        try {
            return inspectLocalModelFolder.inspect(ModelFolderContract.localTheatreImage(), models);
        } catch (IOException ex) {
            return new ModelInspectionResult(
                    ModelFolderContract.localTheatreImage().engineId(),
                    models,
                    ModelInspectionStatus.MISSING_REQUIRED_FILES,
                    List.of("No se pudo inspeccionar models/image: " + ex.getMessage()),
                    List.of(),
                    false,
                    "No se pudo verificar Imagen IA teatral local.");
        }
    }
}
