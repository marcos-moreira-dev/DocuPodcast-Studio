package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectVisualProcessingSettings;

import java.io.IOException;
import java.util.Map;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;
import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.*;

final class VisualProcessingProjectJsonCodec {
    private VisualProcessingProjectJsonCodec() {
    }

    static ProjectVisualProcessingSettings read(Map<String, Object> section) throws IOException {
        if (section == null || section.isEmpty()) return ProjectVisualProcessingSettings.inherited();
        Boolean enabled = section.containsKey("upscaleEnabled")
                ? booleanOrDefault(section.get("upscaleEnabled"), false) : null;
        Boolean refine = section.containsKey("refineAfterUpscale")
                ? booleanOrDefault(section.get("refineAfterUpscale"), false) : null;
        return new ProjectVisualProcessingSettings(
                stringOrDefault(section.get("generationProfile"), ""),
                enabled,
                stringOrDefault(section.get("upscaleTargetProfile"), ""),
                stringOrDefault(section.get("upscaleModelName"), ""),
                refine,
                stringOrDefault(section.get("refinementPreset"), ""),
                stringOrDefault(section.get("refinementEngineId"), ""));
    }

    static void write(StringBuilder out, ProjectVisualProcessingSettings settings) {
        ProjectVisualProcessingSettings current = settings == null
                ? ProjectVisualProcessingSettings.inherited() : settings;
        indent(out, 1).append("\"visualProcessing\": {\n");
        boolean comma = false;
        if (!current.generationProfile().isBlank()) {
            field(out, 2, "generationProfile", quote(current.generationProfile()));
            comma = true;
        }
        if (current.upscaleEnabled() != null) {
            if (comma) out.append(",\n");
            field(out, 2, "upscaleEnabled", current.upscaleEnabled().toString());
            comma = true;
        }
        if (!current.upscaleTargetProfile().isBlank()) {
            if (comma) out.append(",\n");
            field(out, 2, "upscaleTargetProfile", quote(current.upscaleTargetProfile()));
            comma = true;
        }
        if (!current.upscaleModelName().isBlank()) {
            if (comma) out.append(",\n");
            field(out, 2, "upscaleModelName", quote(current.upscaleModelName()));
            comma = true;
        }
        if (current.refineAfterUpscale() != null) {
            if (comma) out.append(",\n");
            field(out, 2, "refineAfterUpscale", current.refineAfterUpscale().toString());
            comma = true;
        }
        if (!current.refinementPreset().isBlank()) {
            if (comma) out.append(",\n");
            field(out, 2, "refinementPreset", quote(current.refinementPreset()));
            comma = true;
        }
        if (!current.refinementEngineId().isBlank()) {
            if (comma) out.append(",\n");
            field(out, 2, "refinementEngineId", quote(current.refinementEngineId()));
        }
        out.append("\n");
        indent(out, 1).append("}");
    }
}
