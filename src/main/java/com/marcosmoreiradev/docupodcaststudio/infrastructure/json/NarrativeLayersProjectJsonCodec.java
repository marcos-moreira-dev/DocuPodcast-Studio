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

final class NarrativeLayersProjectJsonCodec implements ProjectJsonSectionCodec {
    static List<NarrativeLayerAssignment> readNarrativeLayers(Map<String, Object> narrativeLayers) throws IOException {
        if (narrativeLayers.isEmpty()) {
            return List.of();
        }
        Object rawAssignments = narrativeLayers.getOrDefault("assignments", List.of());
        if (!(rawAssignments instanceof List<?> list)) {
            throw new IOException("narrativeLayers.assignments must be an array");
        }
        ArrayList<NarrativeLayerAssignment> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawAssignment)) {
                throw new IOException("narrativeLayers.assignments entries must be objects");
            }
            Map<String, Object> assignment = (Map<String, Object>) rawAssignment;
            ScriptTextRange scriptRange = new ScriptTextRange(
                    string(assignment.get("segmentId"), "narrativeLayer.segmentId"),
                    intValue(assignment.get("startOffset")),
                    intValue(assignment.get("endOffset"))
            );
            DocumentTextRange documentRange = null;
            if (assignment.get("sourceBlockId") != null) {
                documentRange = new DocumentTextRange(
                        string(assignment.get("sourceBlockId"), "narrativeLayer.sourceBlockId"),
                        intValue(assignment.get("sourceStartOffset")),
                        intValue(assignment.get("sourceEndOffset"))
                );
            }
            String assignmentId = string(assignment.get("id"), "narrativeLayer.id");
            TextAnchor textAnchor = readTextAnchor(optionalObject(assignment.get("textAnchor")), assignmentId, documentRange);
            result.add(new NarrativeLayerAssignment(
                    assignmentId,
                    enumValue(NarrativeLayerKind.class, string(assignment.get("kind"), "narrativeLayer.kind"), "narrativeLayer.kind"),
                    scriptRange,
                    documentRange,
                    textAnchor,
                    stringOrDefault(assignment.get("targetId"), ""),
                    stringOrDefault(assignment.get("displayName"), ""),
                    stringOrDefault(assignment.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }


    static TextAnchor readTextAnchor(Map<String, Object> anchor, String assignmentId, DocumentTextRange fallbackRange) throws IOException {
        if (anchor.isEmpty()) {
            return TextAnchor.legacy(assignmentId, fallbackRange);
        }
        DocumentTextRange range = new DocumentTextRange(
                string(anchor.get("sourceBlockId"), "textAnchor.sourceBlockId"),
                intValue(anchor.get("sourceStartOffset")),
                intValue(anchor.get("sourceEndOffset"))
        );
        return new TextAnchor(
                stringOrDefault(anchor.get("id"), "ANCH-" + assignmentId),
                range,
                stringOrDefault(anchor.get("selectedText"), ""),
                stringOrDefault(anchor.get("selectedTextHash"), ""),
                stringOrDefault(anchor.get("contextBefore"), ""),
                stringOrDefault(anchor.get("contextAfter"), ""),
                stringOrDefault(anchor.get("sourceSnapshotHash"), ""),
                enumValue(TextAnchorConfidence.class, stringOrDefault(anchor.get("confidence"), TextAnchorConfidence.LOW.name()), "textAnchor.confidence"),
                enumValue(TextAnchorStatus.class, stringOrDefault(anchor.get("status"), TextAnchorStatus.NEEDS_REVIEW.name()), "textAnchor.status")
        );
    }

    static void writeNarrativeLayers(StringBuilder out, List<NarrativeLayerAssignment> assignments) {
        indent(out, 1).append("\"narrativeLayers\": {\n");
        indent(out, 2).append("\"assignments\": [");
        if (!assignments.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < assignments.size(); i++) {
            NarrativeLayerAssignment assignment = assignments.get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(assignment.id())); out.append(",\n");
            field(out, 4, "kind", quote(assignment.kind().name())); out.append(",\n");
            field(out, 4, "segmentId", quote(assignment.textRange().segmentId())); out.append(",\n");
            field(out, 4, "startOffset", Integer.toString(assignment.textRange().startOffset())); out.append(",\n");
            field(out, 4, "endOffset", Integer.toString(assignment.textRange().endOffset())); out.append(",\n");
            if (assignment.documentRange() != null) {
                field(out, 4, "sourceBlockId", quote(assignment.documentRange().blockId())); out.append(",\n");
                field(out, 4, "sourceStartOffset", Integer.toString(assignment.documentRange().startOffset())); out.append(",\n");
                field(out, 4, "sourceEndOffset", Integer.toString(assignment.documentRange().endOffset())); out.append(",\n");
            }
            if (assignment.textAnchor() != null) {
                writeTextAnchor(out, assignment.textAnchor());
                out.append(",\n");
            }
            field(out, 4, "targetId", quote(assignment.targetId())); out.append(",\n");
            field(out, 4, "displayName", quote(assignment.displayName())); out.append(",\n");
            field(out, 4, "notes", quote(assignment.notes())); out.append("\n");
            indent(out, 3).append("}");
            if (i < assignments.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }


    static void writeTextAnchor(StringBuilder out, TextAnchor anchor) {
        indent(out, 4).append("\"textAnchor\": {\n");
        field(out, 5, "id", quote(anchor.id())); out.append(",\n");
        field(out, 5, "sourceBlockId", quote(anchor.range().blockId())); out.append(",\n");
        field(out, 5, "sourceStartOffset", Integer.toString(anchor.range().startOffset())); out.append(",\n");
        field(out, 5, "sourceEndOffset", Integer.toString(anchor.range().endOffset())); out.append(",\n");
        field(out, 5, "selectedText", quote(anchor.selectedText())); out.append(",\n");
        field(out, 5, "selectedTextHash", quote(anchor.selectedTextHash())); out.append(",\n");
        field(out, 5, "contextBefore", quote(anchor.contextBefore())); out.append(",\n");
        field(out, 5, "contextAfter", quote(anchor.contextAfter())); out.append(",\n");
        field(out, 5, "sourceSnapshotHash", quote(anchor.sourceSnapshotHash())); out.append(",\n");
        field(out, 5, "confidence", quote(anchor.confidence().name())); out.append(",\n");
        field(out, 5, "status", quote(anchor.status().name())); out.append("\n");
        indent(out, 4).append("}");
    }

    @Override public String sectionName() { return "narrativeLayers"; }
}

