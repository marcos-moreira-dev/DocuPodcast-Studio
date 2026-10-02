package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreExperienceController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreConditionedGenerationPlanUseCaseTest {
    @TempDir Path temporary;

    @Test
    void buildsAuditableRegionalPlanFromCharactersBlockingAndActiveStoryboard() throws Exception {
        TheatreImageContextAsset bigoteFront = image(
                "BIGOTE-FRONT", "CAPITÁN BIGOTE - Frontal", "bigote-front.png",
                Map.of("view", "frontal"));
        TheatreImageContextAsset bigoteSide = image(
                "BIGOTE-SIDE", "CAPITÁN BIGOTE - Lateral", "bigote-side.png",
                Map.of("view", "lateral"));
        TheatreImageContextAsset tornillo = image(
                "TORNILLO-FRONT", "TENIENTE TORNILLO - Frontal", "tornillo.png",
                Map.of("view", "frontal"));
        TheatreImageContextAsset storyboard = image(
                "STORYBOARD", "Frame activo generated", "storyboard.png",
                Map.of("activeVariant", "generated"));
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "El vuelo", "SCENE-1", "El hangar",
                "INTERVENCION-2", "SEG-2", "CAPITÁN BIGOTE",
                "¡Ese tornillo no va ahí!", "CAPITÁN BIGOTE está en centro derecha.", null);
        TheatreVisualGenerationContext context = new TheatreVisualGenerationContext(
                unit,
                List.of(bigoteFront, bigoteSide, tornillo),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                storyboard,
                "generated",
                List.of(
                        new TheatreCharacterGenerationContext(
                                "CAPITAN-BIGOTE", "CAPITÁN BIGOTE",
                                "bigote, gafas, chaqueta, pañuelo, pantalón y botas",
                                "centro derecha", true, List.of(bigoteFront, bigoteSide)),
                        new TheatreCharacterGenerationContext(
                                "TENIENTE-TORNILLO", "TENIENTE TORNILLO",
                                "gafas, chaqueta, pañuelo, pantalón y botas",
                                "centro izquierda", false, List.of(tornillo))),
                "Comedia de aviadores",
                "Hangar al amanecer");

        TheatreConditionedGenerationPlan plan =
                new BuildTheatreConditionedGenerationPlanUseCase().execute(
                        context, TheatreImageAspectRatio.WIDE_16_9, 1234L);

        assertEquals(4, plan.references().size());
        assertEquals(3, plan.references().stream()
                .filter(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role())).count());
        assertEquals(1, plan.references().stream()
                .filter(reference -> MediaReferenceRole.COMPOSITION_GUIDE.equals(reference.role())).count());
        assertEquals(2, plan.references().stream()
                .filter(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))
                .map(reference -> reference.metadata().get("subjectId")).distinct().count());
        var regions = plan.references().stream()
                .filter(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))
                .collect(java.util.stream.Collectors.toMap(
                        reference -> reference.metadata().get("subjectId"),
                        reference -> reference.metadata(),
                        (first, ignored) -> first));
        double bigoteRight = Double.parseDouble(regions.get("CAPITAN-BIGOTE").get("regionX"))
                + Double.parseDouble(regions.get("CAPITAN-BIGOTE").get("regionWidth"));
        double tornilloLeft = Double.parseDouble(regions.get("TENIENTE-TORNILLO").get("regionX"));
        assertTrue(Double.parseDouble(regions.get("CAPITAN-BIGOTE").get("regionX"))
                < Double.parseDouble(regions.get("TENIENTE-TORNILLO").get("regionX")),
                "La derecha del actor mirando al público debe proyectarse a la izquierda de cámara");
        assertTrue(tornilloLeft >= bigoteRight || bigoteRight >= tornilloLeft
                + Double.parseDouble(regions.get("TENIENTE-TORNILLO").get("regionWidth")));
        assertTrue(plan.prompt().contains("¡Ese tornillo no va ahí!"));
        assertTrue(plan.prompt().contains("bigote, gafas, chaqueta, pañuelo, pantalón y botas"));
        assertTrue(plan.prompt().contains("centro derecha"));
        assertTrue(plan.negativePrompt().contains("swapped clothes"));
        assertEquals("generated", plan.provenance().get("storyboard.activeVariant"));
        assertEquals("1234", plan.provenance().get("seed"));
    }

    @Test
    void blocksWhenAnyPresentCharacterHasNoUsableVisualIdentity() throws Exception {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "", "Acto", "", "Escena", "INTERVENCION-2", "SEG-2",
                "CAPITÁN BIGOTE", "Texto", "", null);
        TheatreVisualGenerationContext context = new TheatreVisualGenerationContext(
                unit, List.of(), List.of(), List.of(), null, null, null, null,
                null, "", List.of(new TheatreCharacterGenerationContext(
                "CAPITAN-BIGOTE", "CAPITÁN BIGOTE", "", "centro", true, List.of())), "", "");

        IOException failure = assertThrows(IOException.class,
                () -> new BuildTheatreConditionedGenerationPlanUseCase().execute(
                        context, TheatreImageAspectRatio.WIDE_16_9, 1L));

        assertTrue(failure.getMessage().contains("CAPITÁN BIGOTE"));
        assertTrue(failure.getMessage().contains("referencias visuales"));
    }

    @Test
    void neverFallsBackWhenDeclaredActiveStoryboardFileIsMissing() throws Exception {
        TheatreImageContextAsset character = image(
                "ACTOR", "Actor frontal", "actor.png", Map.of("view", "frontal"));
        TheatreImageContextAsset missing = new TheatreImageContextAsset(
                "storyboard", "Frame activo drawn", "DRAWN",
                temporary.resolve("missing.png").toString(), "", Map.of("activeVariant", "drawn"));
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "", "Acto", "", "Escena", "INTERVENCION-2", "SEG-2",
                "Actor", "Texto", "", null);
        TheatreVisualGenerationContext context = new TheatreVisualGenerationContext(
                unit, List.of(character), List.of(), List.of(), null, null, missing, null,
                missing, "drawn", List.of(new TheatreCharacterGenerationContext(
                "ACTOR", "Actor", "", "centro", true, List.of(character))), "", "");

        IOException failure = assertThrows(IOException.class,
                () -> new BuildTheatreConditionedGenerationPlanUseCase().execute(
                        context, TheatreImageAspectRatio.WIDE_16_9, 1L));

        assertTrue(failure.getMessage().contains("variante activa"));
        assertTrue(failure.getMessage().contains("no se sustituirá"));
    }

    @Test
    void contextualRequestHonorsTheSelectedGenerationAndDeliveryProfile() {
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "", "Acto", "", "Escena", "INTERVENCION-2", "SEG-2",
                "Actor", "Texto", "", null);

        var request = TheatreExperienceController.imageRequest(
                unit,
                TheatreImageGenerationPreset.CONTEXTUAL_4GB_SD15,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                temporary,
                "intervencion-2",
                "prompt",
                List.of());

        assertEquals(1920, request.width());
        assertEquals(1080, request.height());
        assertEquals("1920", request.options().get("deliveryWidth"));
        assertEquals("1080", request.options().get("deliveryHeight"));
        assertEquals(1, request.batchSize());
        assertEquals("sd15-regional-identity", request.presetId().value());
    }

    private TheatreImageContextAsset image(
            String id,
            String label,
            String filename,
            Map<String, String> metadata) throws Exception {
        Path file = Files.write(temporary.resolve(filename), new byte[]{1, 2, 3});
        return new TheatreImageContextAsset(
                "personaje", label, id, filename, file.toUri().toString(), metadata);
    }
}
