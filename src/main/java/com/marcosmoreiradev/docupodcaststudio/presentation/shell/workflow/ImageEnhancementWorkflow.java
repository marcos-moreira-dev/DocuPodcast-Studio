package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.RunImageRefinementUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageSuperResolutionSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Routes legacy enhancement actions through the composition-preserving refinement capability. */
public final class ImageEnhancementWorkflow {
    private final WorkspaceApplicationServices services;
    private final MediaCapabilityService mediaCapabilities;

    public ImageEnhancementWorkflow(WorkspaceApplicationServices services, MediaCapabilityService mediaCapabilities) {
        this.services = Objects.requireNonNull(services, "services");
        this.mediaCapabilities = Objects.requireNonNull(mediaCapabilities, "media capabilities");
    }

    public TheatreGeneratedImageCandidate enhanceCandidate(ProjectSession session,
                                                           TheatreGeneratedImageCandidate candidate,
                                                           ImageEnhancementOutputProfile profile,
                                                           ImageAspectStrategy strategy) throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(candidate, "candidate");
        profile = profile == null ? ImageEnhancementOutputProfile.FHD_1080 : profile;
        strategy = strategy == null ? ImageAspectStrategy.OUTPAINT_TO_TARGET : strategy;
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de mejorar imagenes."));
        Path projectRoot = projectFile.toAbsolutePath().normalize().getParent();
        Path sourceImage = candidate.outputPath();
        if (sourceImage == null) {
            throw new IOException("El candidato seleccionado no tiene una imagen de origen.");
        }

        String jobId = jobId(candidate, profile);
        ImageRefinementRequest request = request(sourceImage,
                projectRoot.resolve("generated/image-enhancement").resolve(jobId),
                jobId, strategy);
        var operational = services.administration().settings().loadOperationalSettings().load();
        ExecutionContext context = new ExecutionContext(jobId, CancellationToken.NONE, ProgressSink.NONE,
                new ExecutionPolicy(Duration.ofSeconds(operational.imageGeneration().timeoutSeconds()),
                        operational.imageGeneration().maxAttempts()), ResourceLease.NONE);
        Path finalImage;
        try {
            ImageRefinementResult refined = new RunImageRefinementUseCase(
                    services.administration().capabilities()).run(
                    SelectedMediaEngines.from(operational).image(),
                    new EngineId(ImageSuperResolutionSettings.DEFAULT_REFINEMENT_ENGINE),
                    request,
                    context);
            if (!refined.refined()) {
                throw new IOException(refined.warning().isBlank()
                        ? "El motor no produjo una imagen mejorada."
                        : refined.warning());
            }
            finalImage = refined.image();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("La mejora de imagen fue cancelada.", interrupted);
        }
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, finalImage);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedImageCandidate(
                candidate.unitId() + "-" + profile.workflowId(),
                candidate.sceneId(),
                candidate.interventionId(),
                candidate.segmentId(),
                imported.imageAsset().id(),
                finalImage,
                false);
    }

    static ImageRefinementRequest request(Path sourceImage,
                                          Path outputDirectory,
                                          String filenamePrefix,
                                          ImageAspectStrategy strategy) {
        ImageAspectStrategy aspect = strategy == null ? ImageAspectStrategy.OUTPAINT_TO_TARGET : strategy;
        String prompt = aspect == ImageAspectStrategy.OUTPAINT_TO_TARGET
                ? "clean coherent details, preserve identity, wardrobe, props, lighting and composition"
                : "clean coherent details while preserving all visual content, identity and composition";
        return new ImageRefinementRequest(sourceImage, outputDirectory, filenamePrefix,
                prompt, ImageRefinementRequest.CONSERVATIVE, 0L,
                Map.of("sourceKind", "legacy-enhancement-action"));
    }

    private static String jobId(TheatreGeneratedImageCandidate candidate, ImageEnhancementOutputProfile profile) {
        String base = (candidate.interventionId() == null || candidate.interventionId().isBlank())
                ? "intervencion"
                : candidate.interventionId().toLowerCase(Locale.ROOT);
        return "enhance-" + base.replaceAll("[^a-z0-9._-]+", "-") + "-" + profile.workflowId() + "-" + System.currentTimeMillis();
    }
}
