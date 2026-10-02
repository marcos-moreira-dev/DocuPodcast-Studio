package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Turns canonical theatre context into a provider-neutral, auditable conditioning request. */
public final class BuildTheatreConditionedGenerationPlanUseCase {
    public TheatreConditionedGenerationPlan execute(TheatreVisualGenerationContext context,
                                                     TheatreImageAspectRatio aspectRatio,
                                                     long seed) throws IOException {
        if (context == null || context.unit() == null) {
            throw new IOException("No existe una intervención seleccionada para generar.");
        }
        if (context.characters().isEmpty()
                && !context.unit().speaker().isBlank()) {
            throw new IOException("No se pudo resolver el perfil visual del personaje presente: "
                    + context.unit().speaker()
                    + ". Vincula el personaje y su referencia antes de generar.");
        }
        List<TheatreCharacterGenerationContext> missing = context.missingCharacterIdentities();
        if (!missing.isEmpty()) {
            throw new IOException("Faltan referencias visuales de identidad para: "
                    + String.join(", ", missing.stream()
                    .map(TheatreCharacterGenerationContext::displayName).toList())
                    + ". Configura al menos una imagen válida por personaje antes de generar.");
        }
        if (context.activeStoryboardFrame() != null
                && !usable(context.activeStoryboardFrame())) {
            throw new IOException("La variante activa del storyboard no está disponible: "
                    + context.activeStoryboardFrame().relativePath()
                    + ". Corrige la variante activa; no se sustituirá silenciosamente.");
        }

        ArrayList<MediaReference> references = new ArrayList<>();
        LinkedHashMap<String, String> provenance = new LinkedHashMap<>();
        int characterIndex = 0;
        int characterCount = Math.max(1, context.characters().size());
        for (TheatreCharacterGenerationContext character : context.characters()) {
            TheatreConditioningRegion region = region(character.location(), characterIndex++, characterCount);
            for (TheatreImageContextAsset asset : character.references()) {
                if (!usable(asset)) continue;
                LinkedHashMap<String, String> metadata = new LinkedHashMap<>(region.metadata());
                metadata.put("subjectId", character.characterId());
                metadata.put("subjectName", character.displayName());
                metadata.put("view", asset.metadata().getOrDefault("view", ""));
                metadata.put("characterNotes", character.notes());
                metadata.put("location", character.location());
                references.add(reference(asset, MediaReferenceRole.REGIONAL_IDENTITY, 0.82, metadata));
            }
            provenance.put("character." + character.characterId(),
                    character.displayName() + " · " + character.references().size() + " referencia(s) · "
                            + character.location());
        }

        add(references, context.objectReferences(), MediaReferenceRole.OBJECT, 0.28, Map.of());
        add(references, context.environmentReferences(), MediaReferenceRole.ENVIRONMENT, 0.24, Map.of());
        add(references, context.cameraGuide(), MediaReferenceRole.CAMERA_GUIDE, 0.20, Map.of());
        add(references, context.previousFrame(), MediaReferenceRole.PREVIOUS_FRAME, 0.16, Map.of());
        add(references, context.nextFrame(), MediaReferenceRole.NEXT_FRAME, 0.16, Map.of());
        if (context.activeStoryboardFrame() != null) {
            boolean drawn = "drawn".equalsIgnoreCase(context.activeStoryboardVariant());
            add(references, context.activeStoryboardFrame(),
                    drawn ? MediaReferenceRole.DRAWN_GUIDE : MediaReferenceRole.COMPOSITION_GUIDE,
                    drawn ? 0.78 : 0.68,
                    Map.of("activeVariant", context.activeStoryboardVariant()));
            provenance.put("storyboard.activeVariant", context.activeStoryboardVariant());
            provenance.put("storyboard.assetId", context.activeStoryboardFrame().assetId());
        } else {
            provenance.put("storyboard.activeVariant", "none");
        }

        TheatreImageGenerationUnit unit = context.unit();
        String ratioPrompt = (aspectRatio == null
                ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio).promptText();
        String prompt = structuredPrompt(context, unit, ratioPrompt);
        String negative = "low quality, blurry, deformed anatomy, deformed hands, unreadable text, watermark, "
                + "duplicate person, extra character, mixed identities, swapped faces, swapped moustaches, "
                + "changed costume, swapped clothes, missing goggles, missing scarf, inconsistent wardrobe";
        provenance.put("interventionId", unit.interventionId());
        provenance.put("segmentId", unit.segmentId());
        provenance.put("seed", Long.toString(Math.max(0L, seed)));
        provenance.put("conditioning", "sd15-regional-ipadapter");
        return new TheatreConditionedGenerationPlan(prompt, negative, seed, references,
                List.of(), provenance);
    }

    private static String structuredPrompt(TheatreVisualGenerationContext context,
                                           TheatreImageGenerationUnit unit,
                                           String ratioPrompt) {
        StringBuilder prompt = new StringBuilder("theatrical aviation comedy scene");
        append(prompt, "Act", unit.actName());
        append(prompt, "Act direction", context.actNotes());
        append(prompt, "Scene", unit.sceneName());
        append(prompt, "Scene direction", context.sceneNotes());
        append(prompt, "Current intervention and action", unit.fullText());
        append(prompt, "Spatial blocking", unit.spatialContextText());
        for (TheatreCharacterGenerationContext character : context.characters()) {
            String identity = character.displayName()
                    + (character.notes().isBlank() ? "" : ": " + character.notes())
                    + (character.location().isBlank() ? "" : "; placed at " + character.location())
                    + "; preserve exactly the face, body traits, goggles, moustache, hair and complete wardrobe "
                    + "shown in this character's regional image references";
            append(prompt, character.speaker() ? "Speaking character" : "Present character", identity);
        }
        appendLabels(prompt, "Required scenic objects", context.objectReferences());
        appendLabels(prompt, "Stage environment", context.environmentReferences());
        append(prompt, "Camera plan", context.cameraGuide() == null ? "" : context.cameraGuide().label());
        append(prompt, "Composition source", context.activeStoryboardFrame() == null ? ""
                : context.activeStoryboardVariant() + ": " + context.activeStoryboardFrame().label());
        append(prompt, "Framing", ratioPrompt);
        prompt.append(". coherent vintage stage photography, warm cinematic light, consistent scale and eyelines");
        return prompt.toString();
    }

    private static void appendLabels(StringBuilder prompt, String label,
                                     List<TheatreImageContextAsset> assets) {
        if (assets == null || assets.isEmpty()) return;
        append(prompt, label, String.join(", ", assets.stream()
                .map(TheatreImageContextAsset::label).filter(value -> !value.isBlank()).toList()));
    }

    private static void append(StringBuilder prompt, String label, String value) {
        if (value == null || value.isBlank()) return;
        prompt.append(". ").append(label).append(": ").append(value.strip());
    }

    private static TheatreConditioningRegion region(String location, int index, int count) {
        String normalized = normalize(location);
        double width = count <= 2 ? 0.30 : Math.max(0.20, 0.84 / count);
        double x;
        // Theatre blocking is expressed from the actor's perspective while facing
        // the audience. Image coordinates are the audience/camera perspective, so
        // horizontal positions must be mirrored before building regional masks.
        if (normalized.contains("centro izquierda")) x = 0.60;
        else if (normalized.contains("centro derecha")) x = 0.10;
        else if (normalized.contains("izquierda")) x = 0.66;
        else if (normalized.contains("derecha")) x = 0.04;
        else if (normalized.contains("centro")) x = 0.35;
        else x = 0.04 + index * (0.92 - width) / Math.max(1, count - 1);
        double y = normalized.contains("fondo") ? 0.20 : 0.12;
        double height = normalized.contains("fondo") ? 0.66 : 0.76;
        return new TheatreConditioningRegion(x, y, width, height);
    }

    private static void add(List<MediaReference> target,
                            List<TheatreImageContextAsset> assets,
                            MediaReferenceRole role,
                            double strength,
                            Map<String, String> metadata) {
        if (assets == null) return;
        assets.forEach(asset -> add(target, asset, role, strength, metadata));
    }

    private static void add(List<MediaReference> target,
                            TheatreImageContextAsset asset,
                            MediaReferenceRole role,
                            double strength,
                            Map<String, String> metadata) {
        if (!usable(asset)) return;
        target.add(reference(asset, role, strength, metadata));
    }

    private static MediaReference reference(TheatreImageContextAsset asset,
                                            MediaReferenceRole role,
                                            double strength,
                                            Map<String, String> metadata) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>(asset.metadata());
        values.putAll(metadata);
        values.put("label", asset.label());
        return new MediaReference(asset.assetId(), path(asset), role, strength, values);
    }

    private static boolean usable(TheatreImageContextAsset asset) {
        if (asset == null || asset.imageUri().isBlank()) return false;
        try {
            return Files.isRegularFile(path(asset));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Path path(TheatreImageContextAsset asset) {
        return Path.of(URI.create(asset.imageUri())).toAbsolutePath().normalize();
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        return Normalizer.normalize(safe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip();
    }
}
