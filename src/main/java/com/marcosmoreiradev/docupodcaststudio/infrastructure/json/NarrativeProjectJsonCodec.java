package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.*;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;
import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.*;

final class NarrativeProjectJsonCodec implements ProjectJsonSectionCodec {
    static NarrativeProjectLayer readNarrative(Map<String, Object> narrative) throws IOException {
        if (narrative.isEmpty()) {
            return NarrativeProjectLayer.empty();
        }
        Map<String, Object> settings = optionalObject(narrative.get("videoConfiguration"));
        NarrativeVideoConfiguration defaults = NarrativeVideoConfiguration.verticalDefaults();
        NarrativeVideoConfiguration configuration = new NarrativeVideoConfiguration(
                intOrDefault(settings.get("width"), defaults.width()),
                intOrDefault(settings.get("height"), defaults.height()),
                intOrDefault(settings.get("framesPerSecond"), defaults.framesPerSecond()),
                doubleOrDefault(settings.get("maxClipDurationSeconds"), defaults.maxClipDurationSeconds()),
                stringOrDefault(settings.get("imageProfile"), defaults.imageProfile()),
                stringOrDefault(settings.get("videoProfile"), defaults.videoProfile()),
                stringOrDefault(settings.get("memoryMode"), defaults.memoryMode()),
                stringOrDefault(settings.get("customWorkflowPath"), defaults.customWorkflowPath())
        );
        return new NarrativeProjectLayer(
                configuration,
                stringOrDefault(narrative.get("documentFingerprint"), ""),
                stringOrDefault(narrative.get("normalizedDocumentText"), ""),
                readNarrativeContextReferences(narrative.get("contextReferences")),
                readNarrativeParagraphTakes(narrative.get("paragraphTakes"))
        );
    }

    @SuppressWarnings("unchecked")
    static List<NarrativeContextReference> readNarrativeContextReferences(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("narrative.contextReferences must be an array");
        }
        ArrayList<NarrativeContextReference> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("narrative.contextReferences entries must be objects");
            }
            Map<String, Object> reference = (Map<String, Object>) raw;
            result.add(new NarrativeContextReference(
                    string(reference.get("id"), "narrative.contextReference.id"),
                    enumValue(NarrativeContextRole.class,
                            stringOrDefault(reference.get("role"), NarrativeContextRole.STYLE.name()),
                            "narrative.contextReference.role"),
                    string(reference.get("assetId"), "narrative.contextReference.assetId"),
                    stringOrDefault(reference.get("displayName"), ""),
                    booleanOrDefault(reference.get("enabled"), true),
                    doubleOrDefault(reference.get("strength"), 1.0),
                    stringOrDefault(reference.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<NarrativeParagraphTake> readNarrativeParagraphTakes(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("narrative.paragraphTakes must be an array");
        }
        ArrayList<NarrativeParagraphTake> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("narrative.paragraphTakes entries must be objects");
            }
            Map<String, Object> take = (Map<String, Object>) raw;
            result.add(new NarrativeParagraphTake(
                    string(take.get("blockId"), "narrative.paragraphTake.blockId"),
                    booleanOrDefault(take.get("enabled"), true),
                    stringOrDefault(take.get("keyframeAssetId"), ""),
                    enumValue(NarrativeKeyframeSource.class,
                            stringOrDefault(take.get("keyframeSource"), NarrativeKeyframeSource.NONE.name()),
                            "narrative.paragraphTake.keyframeSource"),
                    readNarrativeClips(take.get("clips")),
                    stringOrDefault(take.get("prompt"), ""),
                    stringOrDefault(take.get("negativePrompt"), ""),
                    longOrDefault(take.get("seed"), 0L),
                    stringOrDefault(take.get("sourceFingerprint"), ""),
                    booleanOrDefault(take.get("stale"), true),
                    stringOrDefault(take.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<NarrativeGeneratedClip> readNarrativeClips(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("narrative.paragraphTake.clips must be an array");
        }
        ArrayList<NarrativeGeneratedClip> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("narrative.paragraphTake.clips entries must be objects");
            }
            Map<String, Object> clip = (Map<String, Object>) raw;
            result.add(new NarrativeGeneratedClip(
                    string(clip.get("id"), "narrative.clip.id"),
                    string(clip.get("assetId"), "narrative.clip.assetId"),
                    intOrDefault(clip.get("order"), result.size()),
                    doubleOrDefault(clip.get("durationSeconds"), 0.0),
                    stringOrDefault(clip.get("lastFrameAssetId"), ""),
                    stringOrDefault(clip.get("modelId"), ""),
                    stringOrDefault(clip.get("workflowId"), ""),
                    longOrDefault(clip.get("seed"), 0L),
                    stringOrDefault(clip.get("sourceFingerprint"), ""),
                    readStringMap(optionalObject(clip.get("metadata")))
            ));
        }
        return List.copyOf(result);
    }

    static Map<String, String> readStringMap(Map<String, Object> values) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return Map.copyOf(result);
    }

    static void writeNarrative(StringBuilder out, NarrativeProjectLayer narrative) {
        NarrativeVideoConfiguration settings = narrative.videoConfiguration();
        indent(out, 1).append("\"narrative\": {\n");
        indent(out, 2).append("\"videoConfiguration\": {\n");
        field(out, 3, "width", Integer.toString(settings.width())); out.append(",\n");
        field(out, 3, "height", Integer.toString(settings.height())); out.append(",\n");
        field(out, 3, "framesPerSecond", Integer.toString(settings.framesPerSecond())); out.append(",\n");
        field(out, 3, "maxClipDurationSeconds", Double.toString(settings.maxClipDurationSeconds())); out.append(",\n");
        field(out, 3, "imageProfile", quote(settings.imageProfile())); out.append(",\n");
        field(out, 3, "videoProfile", quote(settings.videoProfile())); out.append(",\n");
        field(out, 3, "memoryMode", quote(settings.memoryMode())); out.append(",\n");
        field(out, 3, "customWorkflowPath", quote(settings.customWorkflowPath())); out.append("\n");
        indent(out, 2).append("},\n");
        field(out, 2, "documentFingerprint", quote(narrative.documentFingerprint())); out.append(",\n");
        field(out, 2, "normalizedDocumentText", quote(narrative.normalizedDocumentText())); out.append(",\n");
        writeNarrativeContextReferences(out, narrative.contextReferences(), 2); out.append(",\n");
        writeNarrativeParagraphTakes(out, narrative.paragraphTakes(), 2); out.append("\n");
        indent(out, 1).append("}");
    }

    static void writeNarrativeContextReferences(StringBuilder out,
                                                        List<NarrativeContextReference> references,
                                                        int level) {
        indent(out, level).append("\"contextReferences\": [");
        if (!references.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < references.size(); i++) {
            NarrativeContextReference reference = references.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(reference.id())); out.append(",\n");
            field(out, level + 2, "role", quote(reference.role().name())); out.append(",\n");
            field(out, level + 2, "assetId", quote(reference.assetId())); out.append(",\n");
            field(out, level + 2, "displayName", quote(reference.displayName())); out.append(",\n");
            field(out, level + 2, "enabled", Boolean.toString(reference.enabled())); out.append(",\n");
            field(out, level + 2, "strength", Double.toString(reference.strength())); out.append(",\n");
            field(out, level + 2, "notes", quote(reference.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < references.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeNarrativeParagraphTakes(StringBuilder out,
                                                     List<NarrativeParagraphTake> takes,
                                                     int level) {
        indent(out, level).append("\"paragraphTakes\": [");
        if (!takes.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < takes.size(); i++) {
            NarrativeParagraphTake take = takes.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "blockId", quote(take.blockId())); out.append(",\n");
            field(out, level + 2, "enabled", Boolean.toString(take.enabled())); out.append(",\n");
            field(out, level + 2, "keyframeAssetId", quote(take.keyframeAssetId())); out.append(",\n");
            field(out, level + 2, "keyframeSource", quote(take.keyframeSource().name())); out.append(",\n");
            writeNarrativeClips(out, take.clips(), level + 2); out.append(",\n");
            field(out, level + 2, "prompt", quote(take.prompt())); out.append(",\n");
            field(out, level + 2, "negativePrompt", quote(take.negativePrompt())); out.append(",\n");
            field(out, level + 2, "seed", Long.toString(take.seed())); out.append(",\n");
            field(out, level + 2, "sourceFingerprint", quote(take.sourceFingerprint())); out.append(",\n");
            field(out, level + 2, "stale", Boolean.toString(take.stale())); out.append(",\n");
            field(out, level + 2, "notes", quote(take.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < takes.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeNarrativeClips(StringBuilder out,
                                            List<NarrativeGeneratedClip> clips,
                                            int level) {
        indent(out, level).append("\"clips\": [");
        if (!clips.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < clips.size(); i++) {
            NarrativeGeneratedClip clip = clips.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(clip.id())); out.append(",\n");
            field(out, level + 2, "assetId", quote(clip.assetId())); out.append(",\n");
            field(out, level + 2, "order", Integer.toString(clip.order())); out.append(",\n");
            field(out, level + 2, "durationSeconds", Double.toString(clip.durationSeconds())); out.append(",\n");
            field(out, level + 2, "lastFrameAssetId", quote(clip.lastFrameAssetId())); out.append(",\n");
            field(out, level + 2, "modelId", quote(clip.modelId())); out.append(",\n");
            field(out, level + 2, "workflowId", quote(clip.workflowId())); out.append(",\n");
            field(out, level + 2, "seed", Long.toString(clip.seed())); out.append(",\n");
            field(out, level + 2, "sourceFingerprint", quote(clip.sourceFingerprint())); out.append(",\n");
            writeStringMap(out, "metadata", clip.metadata(), level + 2); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < clips.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    static void writeStringMap(StringBuilder out, String name, Map<String, String> values, int level) {
        indent(out, level).append(quote(name)).append(": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            field(out, level + 1, entry.getKey(), quote(entry.getValue()));
            if (index++ < values.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("}");
    }

    @Override public String sectionName() { return "narrative"; }
}

