package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders user-verified ComfyUI API workflows without hard-coding custom-node schemas. */
final class ConditionedWorkflowTemplateRenderer {
    private static final Pattern UNRESOLVED = Pattern.compile("\\{\\{[A-Z0-9_]+}}");

    String render(Path template, VisualEngineRequest request, ComfyUiWorkflowSpec workflow) throws IOException {
        String body = Files.readString(template, StandardCharsets.UTF_8);
        int width = workflow.generationWidth(request);
        int height = workflow.generationHeight(request);
        body = replace(body, "PROMPT", request.prompt());
        body = replace(body, "NEGATIVE_PROMPT", request.negativePrompt());
        body = replace(body, "CHECKPOINT", request.checkpointName());
        body = replace(body, "MODEL", workflow.modelName());
        body = replace(body, "VAE", workflow.vaeName());
        body = replace(body, "CLIP_L", workflow.clipLName());
        body = replace(body, "T5", workflow.t5Name());
        body = replaceRaw(body, "WIDTH", Integer.toString(width));
        body = replaceRaw(body, "HEIGHT", Integer.toString(height));
        body = replaceRaw(body, "STEPS", Integer.toString(request.steps()));
        body = replaceRaw(body, "CFG", Double.toString(request.cfg()));
        body = replaceRaw(body, "GUIDANCE", Double.toString(workflow.guidance()));
        body = replaceRaw(body, "SEED", Long.toString(new Random().nextLong() & Long.MAX_VALUE));
        body = replace(body, "FILENAME_PREFIX", request.filenamePrefix());
        body = replaceReferences(body, request.conditioningReferences());
        Matcher unresolved = UNRESOLVED.matcher(body);
        if (unresolved.find()) {
            throw new IOException("El workflow condicionado conserva un placeholder sin resolver: " + unresolved.group());
        }
        String stripped = body.strip();
        return stripped.contains("\"prompt\"") ? stripped : "{\"prompt\":" + stripped + "}";
    }

    private static String replaceReferences(String body, List<VisualConditioningReference> references) throws IOException {
        Map<VisualConditioningRole, Integer> counts = new EnumMap<>(VisualConditioningRole.class);
        String result = body;
        for (VisualConditioningReference reference : references) {
            if (reference.engineImageName().isBlank()) {
                throw new IOException("La referencia no fue subida a ComfyUI: " + reference.label());
            }
            int index = counts.merge(reference.role(), 1, Integer::sum);
            String key = switch (reference.role()) {
                case IDENTITY -> "IDENTITY_IMAGE_" + index;
                case OBJECT -> "OBJECT_IMAGE_" + index;
                case ENVIRONMENT -> "ENVIRONMENT_IMAGE_" + index;
                case PREVIOUS_FRAME -> "PREVIOUS_FRAME";
                case NEXT_FRAME -> "NEXT_FRAME";
                case STRUCTURE_GUIDE -> "STRUCTURE_GUIDE";
            };
            result = replace(result, key, reference.engineImageName());
            result = replaceRaw(result, key + "_STRENGTH", Double.toString(reference.strength()));
        }
        return result;
    }

    private static String replace(String body, String key, String value) {
        return body.replace("{{" + key + "}}", escape(value));
    }

    private static String replaceRaw(String body, String key, String value) {
        return body.replace("{{" + key + "}}", value == null ? "" : value);
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
