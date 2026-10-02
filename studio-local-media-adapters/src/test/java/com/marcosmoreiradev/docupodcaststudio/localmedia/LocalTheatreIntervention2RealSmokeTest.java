package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageRefinementRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in GPU smoke for the Aviadores intervention:
 * -Ddocupodcast.realTheatreInterventionSmoke=true
 */
final class LocalTheatreIntervention2RealSmokeTest {
    @Test
    void generatesOnlyInterventionTwoWithoutUsingThePrebuiltUserFrame() throws Exception {
        assumeTrue(Boolean.getBoolean("docupodcast.realTheatreInterventionSmoke"));
        Path root = Path.of(System.getProperty("docupodcast.repositoryRoot", "."))
                .toAbsolutePath().normalize();
        Path output = root.resolve("diagnostics/theatre-contextual-real-smoke");
        Path projectRoot = Path.of(System.getProperty(
                "docupodcast.aviadoresProjectRoot",
                Path.of(System.getProperty("user.home"), "Desktop", "Aviadores Comicos Demo").toString()))
                .toAbsolutePath().normalize();
        Path projectImages = projectRoot.resolve("media/images");
        boolean useProjectAssets = Files.isDirectory(projectImages);
        Path referencesRoot = useProjectAssets
                ? projectImages
                : root.resolve("src/main/resources/examples/aviadores-comicos/assets");
        List<MediaReference> references = List.of(
                identity(resolveReference(referencesRoot, useProjectAssets,
                                "img-031-capitan_bigote_01_frontal.png",
                                "personajes/capitan_bigote/capitan_bigote_01_frontal.png"),
                        "CAPITAN-BIGOTE", "CAPITÁN BIGOTE", "frontal",
                        0.10, 0.12, 0.30, 0.76),
                identity(resolveReference(referencesRoot, useProjectAssets,
                                "img-032-capitan_bigote_02_lateral_izquierdo.png",
                                "personajes/capitan_bigote/capitan_bigote_02_lateral_izquierdo.png"),
                        "CAPITAN-BIGOTE", "CAPITÁN BIGOTE", "lateral",
                        0.10, 0.12, 0.30, 0.76),
                identity(resolveReference(referencesRoot, useProjectAssets,
                                "img-036-teniente_tornillo_01_frontal.png",
                                "personajes/teniente_tornillo/teniente_tornillo_01_frontal.png"),
                        "TENIENTE-TORNILLO", "TENIENTE TORNILLO", "frontal",
                        0.60, 0.12, 0.30, 0.76),
                identity(resolveReference(referencesRoot, useProjectAssets,
                                "img-037-teniente_tornillo_02_lateral_izquierdo.png",
                                "personajes/teniente_tornillo/teniente_tornillo_02_lateral_izquierdo.png"),
                        "TENIENTE-TORNILLO", "TENIENTE TORNILLO", "lateral",
                        0.60, 0.12, 0.30, 0.76),
                objectReference("AVION-TORNILLO-DORADO",
                        resolveReference(referencesRoot, useProjectAssets,
                                "img-041-avion_tornillo_dorado_01.png",
                                "utileria/avion_tornillo_dorado_01.png"),
                        "Avión Tornillo Dorado"),
                compositionReference("ESCENARIO-TEATRO",
                        resolveReference(referencesRoot, useProjectAssets,
                                "img-028-teatro-vacio.png",
                                "mapas/teatro-vacio.png"),
                        "Escenario teatral asignado"));
        for (MediaReference reference : references) {
            assertTrue(Files.isRegularFile(reference.file()), "Falta " + reference.file());
        }
        Map<Path, String> originalHashes = new LinkedHashMap<>();
        for (MediaReference reference : references) {
            originalHashes.put(reference.file(), ManagedDownloadPreflightInspector.sha256(reference.file()));
        }

        var platform = LocalMediaAdapters.create(LocalMediaLayout.development(root));
        var administration = platform.administration().require(ComfyUiImageEngine.ID);
        ExecutionContext context = new ExecutionContext(
                "real-theatre-intervention-2-smoke",
                CancellationToken.NONE,
                (stage, amount, message) -> System.out.printf(
                        "[%s] %3.0f%% %s%n", stage, amount * 100.0, message),
                new ExecutionPolicy(Duration.ofHours(4), 1),
                ResourceLease.NONE);
        boolean lifecycleReached = false;
        try {
            administration.execute(new EngineActionRequest(
                    ComfyUiImageEngine.ID,
                    EngineActionId.START,
                    Map.of("memoryProfile", "VRAM_RAM_OFFLOAD")),
                    context);
            lifecycleReached = true;
            var result = platform.imageEngines().require(ComfyUiImageEngine.ID).generate(
                    new ImageGenerationRequest(
                            "Acto El vuelo. Escena El hangar. CAPITÁN BIGOTE habla con TENIENTE TORNILLO. "
                                    + "Exactamente dos aviadores adultos, uno por personaje; ninguna otra persona. "
                                    + "Conservar exactamente bigotes, gafas, chaquetas, pañuelos, pantalones y botas. "
                                    + "CAPITÁN BIGOTE está en centro derecha mirando a TENIENTE TORNILLO, "
                                    + "ubicado en centro izquierda. Convención teatral: la derecha del personaje "
                                    + "que mira al público aparece a la izquierda de la cámara; por tanto CAPITÁN "
                                    + "BIGOTE aparece a la izquierda y TENIENTE TORNILLO a la derecha de la imagen. "
                                    + "Respetar el avión Tornillo Dorado, el hangar, la postura, utilería, fondo "
                                    + "y un plano cinematográfico cerca centro nivel. "
                                    + "Ilustración cinematográfica detallada, coherente y limpia.",
                            "duplicate person, mixed identities, swapped faces, swapped clothes, altered moustache, "
                                    + "altered goggles, extra character, missing character, changed camera, "
                                    + "changed composition, unreadable text, watermark, low detail",
                            960,
                            544,
                            references.stream().map(MediaReference::file).toList(),
                            output,
                            "intervencion-2-contextual-540p",
                            Map.of("deliveryWidth", "960", "deliveryHeight", "540", "steps", "40"),
                            ComfyUiImageEngine.SD15_REGIONAL_IDENTITY,
                            references,
                            424242L,
                            1),
                    context);

            Path png = result.images().getFirst();
            var image = ImageIO.read(png.toFile());
            assertEquals(960, image.getWidth());
            assertEquals(540, image.getHeight());
            assertEquals("gpu-first-adaptive-offload", result.diagnostics().get("computeMode"));

            var upscale = platform.imageSuperResolutionEngines()
                    .require(ComfyUiSuperResolutionEngine.ID)
                    .upscale(new ImageSuperResolutionRequest(
                            png,
                            output,
                            "intervencion-2-contextual-4k",
                            3840,
                            2160,
                            ImageSuperResolutionRequest.DEFAULT_MODEL,
                            false,
                            Map.of()),
                            context);
            var upscaled = ImageIO.read(upscale.image().toFile());
            assertEquals(3840, upscaled.getWidth());
            assertEquals(2160, upscaled.getHeight());

            Path candidate = upscale.image();
            if (Boolean.getBoolean("docupodcast.theatreSmokeRefine")) {
                var refinement = platform.imageRefinementEngines()
                        .require(ComfyUiTileRefinementEngine.ID)
                        .refine(new ImageRefinementRequest(
                                upscale.image(),
                                output,
                                "intervencion-2-contextual-4k-mejorada",
                                "Conservar exactamente identidad, vestuario, pose, escenario y plano; "
                                        + "mejorar únicamente detalle fino, coherencia y textura.",
                                ImageRefinementRequest.CONSERVATIVE,
                                424242L,
                                Map.of("tileSize", "512")),
                                context);
                assertTrue(refinement.refined());
                var refined = ImageIO.read(refinement.image().toFile());
                assertEquals(3840, refined.getWidth());
                assertEquals(2160, refined.getHeight());
                candidate = refinement.image();
            }

            Path export = Path.of(System.getProperty(
                    "docupodcast.theatreSmokeOutput",
                    Path.of(System.getProperty("user.home"), "Desktop",
                            "Aviadores-Intervencion-2-contextual-4k.png").toString()))
                    .toAbsolutePath().normalize();
            Files.copy(candidate, export, StandardCopyOption.REPLACE_EXISTING);
            assertTrue(Files.isRegularFile(export));
            for (Map.Entry<Path, String> entry : originalHashes.entrySet()) {
                assertEquals(entry.getValue(), ManagedDownloadPreflightInspector.sha256(entry.getKey()));
            }
        } finally {
            if (lifecycleReached) {
                administration.execute(new EngineActionRequest(
                        ComfyUiImageEngine.ID, EngineActionId.STOP, Map.of()), context);
            }
        }
    }

    private static Path resolveReference(
            Path root,
            boolean projectAssets,
            String projectFilename,
            String builtInRelative) {
        return root.resolve(projectAssets ? projectFilename : builtInRelative);
    }

    private static MediaReference reference(
            String id,
            Path file,
            MediaReferenceRole role,
            double strength,
            String label) {
        return new MediaReference(id, file, role, strength,
                Map.of("label", label, "activeVariant",
                        MediaReferenceRole.COMPOSITION_GUIDE.equals(role) ? "official" : ""));
    }

    private static MediaReference compositionReference(String id, Path file, String label) {
        return new MediaReference(id, file, MediaReferenceRole.COMPOSITION_GUIDE, 0.64,
                Map.of(
                        "label", label,
                        "activeVariant", "environment-bootstrap",
                        "compositionDenoise", "0.76"));
    }

    private static MediaReference objectReference(String id, Path file, String label) {
        return new MediaReference(id, file, MediaReferenceRole.OBJECT, 0.68,
                Map.of(
                        "label", label,
                        "regionX", "0.32",
                        "regionY", "0.32",
                        "regionWidth", "0.38",
                        "regionHeight", "0.42"));
    }

    private static MediaReference identity(
            Path file,
            String subjectId,
            String subjectName,
            String view,
            double x,
            double y,
            double width,
            double height) {
        return new MediaReference(
                subjectId + '-' + view,
                file,
                MediaReferenceRole.REGIONAL_IDENTITY,
                0.94,
                Map.of(
                        "subjectId", subjectId,
                        "subjectName", subjectName,
                        "view", view,
                        "characterNotes", "rasgos y vestuario canónicos",
                        "regionX", Double.toString(x),
                        "regionY", Double.toString(y),
                        "regionWidth", Double.toString(width),
                        "regionHeight", Double.toString(height)));
    }

}
