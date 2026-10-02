package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadAdministration;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadDecision;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Explicit, opt-in preparation smoke for the heavy contextual-image dependencies.
 * It never participates in the normal suite and still executes every managed preflight.
 */
final class PrepareTheatreConditioningRealTest {
    @Test
    void preparesOfficialConditioningResourcesThroughManagedDownloads() throws Exception {
        assumeTrue(Boolean.getBoolean("docupodcast.prepareTheatreConditioning"));
        Path root = Path.of(System.getProperty("docupodcast.repositoryRoot", "."))
                .toAbsolutePath().normalize();
        var platform = LocalMediaAdapters.create(LocalMediaLayout.development(root));
        var administration = (ManagedDownloadAdministration) platform.administration()
                .require(ComfyUiImageEngine.ID);
        AtomicInteger lastPercent = new AtomicInteger(-1);
        ExecutionContext context = new ExecutionContext(
                "prepare-theatre-conditioning-real",
                CancellationToken.NONE,
                (stage, amount, message) -> {
                    int percent = Math.max(0, Math.min(100, (int) Math.floor(amount * 100.0)));
                    if (percent == 100 || percent >= lastPercent.get() + 5) {
                        lastPercent.set(percent);
                        System.out.printf("[%s] %3d%% %s%n", stage, percent, message);
                    }
                },
                new ExecutionPolicy(Duration.ofHours(2), 1),
                ResourceLease.NONE);

        for (EngineActionId action : List.of(
                ComfyUiImageEngineAdministration.DOWNLOAD_IPADAPTER_NODE,
                ComfyUiImageEngineAdministration.DOWNLOAD_IPADAPTER_MODEL,
                ComfyUiImageEngineAdministration.DOWNLOAD_CLIP_VISION,
                ComfyUiImageEngineAdministration.DOWNLOAD_SCRIBBLE,
                ComfyUiImageEngineAdministration.DOWNLOAD_DREAMSHAPER)) {
            EngineActionRequest request = new EngineActionRequest(
                    ComfyUiImageEngine.ID, action, Map.of());
            var preflight = administration.inspectDownload(request);
            ManagedDownloadDecision decision = preflight.valid()
                    ? ManagedDownloadDecision.USE_EXISTING
                    : ManagedDownloadDecision.REDOWNLOAD;
            var result = administration.executeDownload(request, decision, context);
            assertTrue(result.success(), () -> action.value() + ": " + result.message());
            assertTrue(administration.inspectDownload(request).valid(),
                    () -> action.value() + " no quedó válido después de la preparación");
        }
    }
}
