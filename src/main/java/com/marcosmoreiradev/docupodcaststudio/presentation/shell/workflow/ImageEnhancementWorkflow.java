package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementProvider;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementRequest;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementResult;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Coordinates enhancement/upscale/adaptation jobs and imports their results as reviewable assets. */
public final class ImageEnhancementWorkflow {
    private final ApplicationServices services;
    private final ImageEnhancementProvider provider;

    public ImageEnhancementWorkflow(ApplicationServices services) {
        this(services, new ComfyUiImageEnhancementProvider());
    }

    ImageEnhancementWorkflow(ApplicationServices services, ImageEnhancementProvider provider) {
        this.services = Objects.requireNonNull(services, "services");
        this.provider = Objects.requireNonNull(provider, "provider");
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
        if (sourceImage == null || !Files.isRegularFile(sourceImage)) {
            throw new IOException("El candidato seleccionado no tiene PNG de origen.");
        }

        ImageEnhancementRequest request = new ImageEnhancementRequest(
                jobId(candidate, profile),
                sourceImage.toAbsolutePath().normalize(),
                projectRoot,
                profile,
                strategy,
                profile.pipelineProfile(),
                provider.id(),
                profile.workflowId(),
                "",
                profile.professional() ? 32 : 24,
                strategy.requiresOutpainting() ? 0.42 : 0.25,
                profile.tileSize(),
                profile.tileOverlap(),
                profile.pipelineProfile().supportsLora(),
                "");
        ImageEnhancementResult result = provider.enhance(request);
        if (!result.success() || result.finalImage() == null || !Files.isRegularFile(result.finalImage())) {
            throw new IOException(result.message().isBlank() ? "La mejora no genero PNG final." : result.message());
        }
        var imported = services.storyboard().importImageAsset().importImage(session.project(), projectFile, result.finalImage());
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedImageCandidate(
                candidate.unitId() + "-" + profile.workflowId(),
                candidate.sceneId(),
                candidate.interventionId(),
                candidate.segmentId(),
                imported.imageAsset().id(),
                result.finalImage(),
                false);
    }

    private static String jobId(TheatreGeneratedImageCandidate candidate, ImageEnhancementOutputProfile profile) {
        String base = (candidate.interventionId() == null || candidate.interventionId().isBlank())
                ? "intervencion"
                : candidate.interventionId().toLowerCase(Locale.ROOT);
        return "enhance-" + base.replaceAll("[^a-z0-9._-]+", "-") + "-" + profile.workflowId() + "-" + System.currentTimeMillis();
    }
}
