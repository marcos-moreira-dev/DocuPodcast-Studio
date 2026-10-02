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

final class StudyProjectJsonCodec implements ProjectJsonSectionCodec {
    static StudyProjectLayer readStudy(Map<String, Object> study) throws IOException {
        if (study.isEmpty()) {
            return StudyProjectLayer.empty();
        }
        List<Map<String, Object>> items = castList(study.get("technicalProblems"), "study.technicalProblems");
        ArrayList<TechnicalProblem> result = new ArrayList<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                String id = string(item.get("id"), "study.technicalProblem.id");
                result.add(new TechnicalProblem(
                        id,
                        stringOrDefault(item.get("title"), id),
                        readStudySources(item.get("sources")),
                        stringOrDefault(item.get("problemText"), ""),
                        stringOrDefault(item.get("solutionText"), ""),
                        stringOrDefault(item.get("solutionImageAssetId"), ""),
                        item.get("createdAt") == null ? Instant.now() : instant(item.get("createdAt"), "study.technicalProblem.createdAt"),
                        item.get("updatedAt") == null ? Instant.now() : instant(item.get("updatedAt"), "study.technicalProblem.updatedAt"),
                        stringOrDefault(item.get("notes"), "")
                ));
            }
        }
        return new StudyProjectLayer(result,
                readDocumentaryVideoConfiguration(optionalObject(study.get("documentaryVideoConfiguration"))));
    }

    static DocumentStudyVideoConfiguration readDocumentaryVideoConfiguration(Map<String, Object> map)
            throws IOException {
        if (map.isEmpty()) return DocumentStudyVideoConfiguration.empty();
        ArrayList<DocumentParagraphVisualAssignment> paragraphs = new ArrayList<>();
        List<Map<String, Object>> paragraphItems = castList(map.get("paragraphVisuals"),
                "study.documentaryVideoConfiguration.paragraphVisuals");
        if (paragraphItems != null) {
            for (Map<String, Object> item : paragraphItems) {
                paragraphs.add(new DocumentParagraphVisualAssignment(
                        string(item.get("blockId"), "study.documentaryVideoConfiguration.paragraph.blockId"),
                        stringOrDefault(item.get("sourceTextFingerprint"), ""),
                        stringOrDefault(item.get("importedImageAssetId"), ""),
                        stringOrDefault(item.get("drawnImageAssetId"), ""),
                        stringOrDefault(item.get("drawnStateRelativePath"), ""),
                        enumValue(DocumentVisualSource.class,
                                stringOrDefault(item.get("activeSource"), DocumentVisualSource.NONE.name()),
                                "study.documentaryVideoConfiguration.paragraph.activeSource"),
                        stringOrDefault(item.get("mascotAssetId"), ""),
                        enumValue(DocumentMascotPosition.class,
                                stringOrDefault(item.get("mascotPosition"), DocumentMascotPosition.BOTTOM_RIGHT.name()),
                                "study.documentaryVideoConfiguration.paragraph.mascotPosition"),
                        intOrDefault(item.get("mascotSizePercent"), 15),
                        stringOrDefault(item.get("subtitle"), ""),
                        booleanOrDefault(item.get("illustrationOnly"), false)));
            }
        }
        ArrayList<DocumentTableSlideConfiguration> tables = new ArrayList<>();
        List<Map<String, Object>> tableItems = castList(map.get("tableSlides"),
                "study.documentaryVideoConfiguration.tableSlides");
        if (tableItems != null) {
            for (Map<String, Object> item : tableItems) {
                tables.add(new DocumentTableSlideConfiguration(
                        string(item.get("blockId"), "study.documentaryVideoConfiguration.table.blockId"),
                        doubleOrDefault(item.get("durationSeconds"), 6.0)));
            }
        }
        ArrayList<DocumentStudyMusicTrack> tracks = new ArrayList<>();
        List<Map<String, Object>> musicItems = castList(map.get("musicTracks"),
                "study.documentaryVideoConfiguration.musicTracks");
        if (musicItems != null) {
            for (Map<String, Object> item : musicItems) {
                tracks.add(new DocumentStudyMusicTrack(
                        string(item.get("id"), "study.documentaryVideoConfiguration.music.id"),
                        string(item.get("assetId"), "study.documentaryVideoConfiguration.music.assetId"),
                        doubleOrDefault(item.get("durationSeconds"), 0.0),
                        doubleOrDefault(item.get("volume"), 0.20)));
            }
        }
        ArrayList<DocumentStudyClosingSlide> closingSlides = new ArrayList<>();
        List<Map<String, Object>> closingItems = castList(map.get("closingSlides"),
                "study.documentaryVideoConfiguration.closingSlides");
        if (closingItems != null) {
            for (Map<String, Object> item : closingItems) {
                closingSlides.add(new DocumentStudyClosingSlide(
                        string(item.get("id"), "study.documentaryVideoConfiguration.closing.id"),
                        stringOrDefault(item.get("title"), ""),
                        doubleOrDefault(item.get("durationSeconds"), 6.0),
                        stringOrDefault(item.get("imageAssetId"), "")));
            }
        }
        ArrayList<DocumentVideoSlideConfiguration> contentSlides = new ArrayList<>();
        List<Map<String, Object>> contentItems = castList(map.get("contentSlides"),
                "study.documentaryVideoConfiguration.contentSlides");
        if (contentItems != null) {
            for (Map<String, Object> item : contentItems) {
                String contentId = string(item.get("contentId"),
                        "study.documentaryVideoConfiguration.content.contentId");
                DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                        contentId,
                        stringOrDefault(item.get("sourceTextFingerprint"), ""),
                        stringOrDefault(item.get("importedImageAssetId"), ""),
                        stringOrDefault(item.get("drawnImageAssetId"), ""),
                        stringOrDefault(item.get("drawnStateRelativePath"), ""),
                        enumValue(DocumentVisualSource.class,
                                stringOrDefault(item.get("activeSource"), DocumentVisualSource.NONE.name()),
                                "study.documentaryVideoConfiguration.content.activeSource"),
                        stringOrDefault(item.get("mascotAssetId"), ""),
                        enumValue(DocumentMascotPosition.class,
                                stringOrDefault(item.get("mascotPosition"), DocumentMascotPosition.BOTTOM_RIGHT.name()),
                                "study.documentaryVideoConfiguration.content.mascotPosition"),
                        intOrDefault(item.get("mascotSizePercent"), 15),
                        stringOrDefault(item.get("subtitle"), ""),
                        booleanOrDefault(item.get("illustrationOnly"), false));
                contentSlides.add(new DocumentVideoSlideConfiguration(
                        contentId,
                        stringOrDefault(item.get("sourceFingerprint"), ""),
                        visual,
                        doubleOrDefault(item.get("durationSeconds"), 0.0),
                        booleanOrDefault(item.get("enabled"), true),
                        stringOrDefault(item.get("sourceVisualAssetId"),
                                stringOrDefault(item.get("sourceRoiAssetId"), "")),
                        stringOrDefault(item.get("sourceVisualFingerprint"), "")));
            }
        }
        return new DocumentStudyVideoConfiguration(
                stringOrDefault(map.get("videoTitle"), ""),
                doubleOrDefault(map.get("defaultSecondarySemanticDurationSeconds"),
                        doubleOrDefault(map.get("defaultTableDurationSeconds"), 6.0)),
                paragraphs, tables, tracks,
                stringListOrDefault(map.get("disabledBlockIds"), List.of()),
                closingSlides, contentSlides,
                enumValue(SecondarySlideInclusionMode.class,
                        stringOrDefault(map.get("secondarySlideInclusionMode"),
                                SecondarySlideInclusionMode.FOLLOW_READING_POLICY.name()),
                        "study.documentaryVideoConfiguration.secondarySlideInclusionMode"),
                Boolean.TRUE.equals(map.get("aiIllustrationsEnabled")),
                new com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentAiIllustrationAppearance(
                        Boolean.TRUE.equals(map.get("aiIllustrationBackground")),
                        doubleOrDefault(map.get("aiIllustrationOpacity"), 0.35),
                        (String) map.get("aiIllustrationFit")));
    }

    static List<StudySourceReference> readStudySources(Object value) throws IOException {
        List<Map<String, Object>> items = castList(value, "study.technicalProblem.sources");
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        ArrayList<StudySourceReference> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            result.add(new StudySourceReference(
                    string(item.get("blockId"), "study.source.blockId"),
                    intOrDefault(item.get("startOffset"), 0),
                    intOrDefault(item.get("endOffset"), 0),
                    stringOrDefault(item.get("selectedText"), ""),
                    stringOrDefault(item.get("sourceCropAssetId"), ""),
                    stringOrDefault(item.get("sourcePage"), ""),
                    stringOrDefault(item.get("bbox"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")

    static void writeDocumentaryVideoConfiguration(StringBuilder out,
                                                             DocumentStudyVideoConfiguration configuration,
                                                             int level) {
        DocumentStudyVideoConfiguration safe = configuration == null
                ? DocumentStudyVideoConfiguration.empty() : configuration;
        indent(out, level).append("\"documentaryVideoConfiguration\": {\n");
        field(out, level + 1, "videoTitle", quote(safe.videoTitle())); out.append(",\n");
        field(out, level + 1, "defaultTableDurationSeconds",
                Double.toString(safe.defaultTableDurationSeconds())); out.append(",\n");
        field(out, level + 1, "defaultSecondarySemanticDurationSeconds",
                Double.toString(safe.defaultSecondarySemanticDurationSeconds())); out.append(",\n");
        field(out, level + 1, "aiIllustrationBackground", Boolean.toString(safe.aiIllustrationAppearance().background())); out.append(",\n");
        field(out, level + 1, "aiIllustrationOpacity", Double.toString(safe.aiIllustrationAppearance().opacity())); out.append(",\n");
        field(out, level + 1, "aiIllustrationFit", quote(safe.aiIllustrationAppearance().fit())); out.append(",\n");
        field(out, level + 1, "aiIllustrationsEnabled", Boolean.toString(safe.aiIllustrationsEnabled())); out.append(",\n");
        field(out, level + 1, "secondarySlideInclusionMode",
                quote(safe.secondarySlideInclusionMode().name())); out.append(",\n");
        field(out, level + 1, "disabledBlockIds", stringArray(safe.disabledBlockIds())); out.append(",\n");
        indent(out, level + 1).append("\"paragraphVisuals\": [");
        if (!safe.paragraphVisuals().isEmpty()) out.append("\n");
        for (int i = 0; i < safe.paragraphVisuals().size(); i++) {
            DocumentParagraphVisualAssignment item = safe.paragraphVisuals().get(i);
            indent(out, level + 2).append("{\n");
            field(out, level + 3, "blockId", quote(item.blockId())); out.append(",\n");
            field(out, level + 3, "sourceTextFingerprint", quote(item.sourceTextFingerprint())); out.append(",\n");
            field(out, level + 3, "importedImageAssetId", quote(item.importedImageAssetId())); out.append(",\n");
            field(out, level + 3, "drawnImageAssetId", quote(item.drawnImageAssetId())); out.append(",\n");
            field(out, level + 3, "drawnStateRelativePath", quote(item.drawnStateRelativePath())); out.append(",\n");
            field(out, level + 3, "activeSource", quote(item.activeSource().name())); out.append(",\n");
            field(out, level + 3, "mascotAssetId", quote(item.mascotAssetId())); out.append(",\n");
            field(out, level + 3, "mascotPosition", quote(item.mascotPosition().name())); out.append(",\n");
            field(out, level + 3, "mascotSizePercent", Integer.toString(item.mascotSizePercent())); out.append(",\n");
            field(out, level + 3, "subtitle", quote(item.subtitle())); out.append(",\n");
            field(out, level + 3, "illustrationOnly", Boolean.toString(item.illustrationOnly())); out.append("\n");
            indent(out, level + 2).append("}");
            if (i < safe.paragraphVisuals().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level + 1).append("],\n");
        indent(out, level + 1).append("\"tableSlides\": [");
        if (!safe.tableSlides().isEmpty()) out.append("\n");
        for (int i = 0; i < safe.tableSlides().size(); i++) {
            DocumentTableSlideConfiguration item = safe.tableSlides().get(i);
            indent(out, level + 2).append("{\n");
            field(out, level + 3, "blockId", quote(item.blockId())); out.append(",\n");
            field(out, level + 3, "durationSeconds", Double.toString(item.durationSeconds())); out.append("\n");
            indent(out, level + 2).append("}");
            if (i < safe.tableSlides().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level + 1).append("],\n");
        indent(out, level + 1).append("\"closingSlides\": [");
        if (!safe.closingSlides().isEmpty()) out.append("\n");
        for (int i = 0; i < safe.closingSlides().size(); i++) {
            DocumentStudyClosingSlide item = safe.closingSlides().get(i);
            indent(out, level + 2).append("{\n");
            field(out, level + 3, "id", quote(item.id())); out.append(",\n");
            field(out, level + 3, "title", quote(item.title())); out.append(",\n");
            field(out, level + 3, "durationSeconds", Double.toString(item.durationSeconds())); out.append(",\n");
            field(out, level + 3, "imageAssetId", quote(item.imageAssetId())); out.append("\n");
            indent(out, level + 2).append("}");
            if (i < safe.closingSlides().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level + 1).append("],\n");
        indent(out, level + 1).append("\"contentSlides\": [");
        if (!safe.contentSlides().isEmpty()) out.append("\n");
        for (int i = 0; i < safe.contentSlides().size(); i++) {
            DocumentVideoSlideConfiguration item = safe.contentSlides().get(i);
            DocumentParagraphVisualAssignment visual = item.visual();
            indent(out, level + 2).append("{\n");
            field(out, level + 3, "contentId", quote(item.contentId())); out.append(",\n");
            field(out, level + 3, "sourceFingerprint", quote(item.sourceFingerprint())); out.append(",\n");
            field(out, level + 3, "durationSeconds", Double.toString(item.durationSeconds())); out.append(",\n");
            field(out, level + 3, "enabled", Boolean.toString(item.enabled())); out.append(",\n");
            field(out, level + 3, "sourceVisualAssetId", quote(item.sourceVisualAssetId())); out.append(",\n");
            field(out, level + 3, "sourceVisualFingerprint", quote(item.sourceVisualFingerprint())); out.append(",\n");
            field(out, level + 3, "sourceTextFingerprint", quote(visual.sourceTextFingerprint())); out.append(",\n");
            field(out, level + 3, "importedImageAssetId", quote(visual.importedImageAssetId())); out.append(",\n");
            field(out, level + 3, "drawnImageAssetId", quote(visual.drawnImageAssetId())); out.append(",\n");
            field(out, level + 3, "drawnStateRelativePath", quote(visual.drawnStateRelativePath())); out.append(",\n");
            field(out, level + 3, "activeSource", quote(visual.activeSource().name())); out.append(",\n");
            field(out, level + 3, "mascotAssetId", quote(visual.mascotAssetId())); out.append(",\n");
            field(out, level + 3, "mascotPosition", quote(visual.mascotPosition().name())); out.append(",\n");
            field(out, level + 3, "mascotSizePercent", Integer.toString(visual.mascotSizePercent())); out.append(",\n");
            field(out, level + 3, "subtitle", quote(visual.subtitle())); out.append(",\n");
            field(out, level + 3, "illustrationOnly", Boolean.toString(visual.illustrationOnly())); out.append("\n");
            indent(out, level + 2).append("}");
            if (i < safe.contentSlides().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level + 1).append("],\n");
        indent(out, level + 1).append("\"musicTracks\": [");
        if (!safe.musicTracks().isEmpty()) out.append("\n");
        for (int i = 0; i < safe.musicTracks().size(); i++) {
            DocumentStudyMusicTrack item = safe.musicTracks().get(i);
            indent(out, level + 2).append("{\n");
            field(out, level + 3, "id", quote(item.id())); out.append(",\n");
            field(out, level + 3, "assetId", quote(item.assetId())); out.append(",\n");
            field(out, level + 3, "durationSeconds", Double.toString(item.durationSeconds())); out.append(",\n");
            field(out, level + 3, "volume", Double.toString(item.volume())); out.append("\n");
            indent(out, level + 2).append("}");
            if (i < safe.musicTracks().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, level + 1).append("]\n");
        indent(out, level).append("}");
    }

    static void writeStudy(StringBuilder out, StudyProjectLayer study) {
        StudyProjectLayer safeStudy = study == null ? StudyProjectLayer.empty() : study;
        indent(out, 1).append("\"study\": {\n");
        indent(out, 2).append("\"technicalProblems\": [");
        if (!safeStudy.technicalProblems().isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeStudy.technicalProblems().size(); i++) {
            TechnicalProblem problem = safeStudy.technicalProblems().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(problem.id())); out.append(",\n");
            field(out, 4, "title", quote(problem.title())); out.append(",\n");
            writeStudySources(out, problem.sources(), 4);
            out.append(",\n");
            field(out, 4, "problemText", quote(problem.problemText())); out.append(",\n");
            field(out, 4, "solutionText", quote(problem.solutionText())); out.append(",\n");
            field(out, 4, "solutionImageAssetId", quote(problem.solutionImageAssetId())); out.append(",\n");
            field(out, 4, "createdAt", quote(problem.createdAt().toString())); out.append(",\n");
            field(out, 4, "updatedAt", quote(problem.updatedAt().toString())); out.append(",\n");
            field(out, 4, "notes", quote(problem.notes())); out.append("\n");
            indent(out, 3).append("}");
            if (i < safeStudy.technicalProblems().size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, 2).append("],\n");
        writeDocumentaryVideoConfiguration(out, safeStudy.documentaryVideoConfiguration(), 2);
        out.append("\n");
        indent(out, 1).append("}");
    }

    static void writeStudySources(StringBuilder out, List<StudySourceReference> sources, int level) {
        indent(out, level).append("\"sources\": [");
        if (sources != null && !sources.isEmpty()) {
            out.append("\n");
        }
        List<StudySourceReference> safeSources = sources == null ? List.of() : sources;
        for (int i = 0; i < safeSources.size(); i++) {
            StudySourceReference source = safeSources.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "blockId", quote(source.blockId())); out.append(",\n");
            field(out, level + 2, "startOffset", Integer.toString(source.startOffset())); out.append(",\n");
            field(out, level + 2, "endOffset", Integer.toString(source.endOffset())); out.append(",\n");
            field(out, level + 2, "selectedText", quote(source.selectedText())); out.append(",\n");
            field(out, level + 2, "sourceCropAssetId", quote(source.sourceCropAssetId())); out.append(",\n");
            field(out, level + 2, "sourcePage", quote(source.sourcePage())); out.append(",\n");
            field(out, level + 2, "bbox", quote(source.bbox())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeSources.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    @Override public String sectionName() { return "study"; }
}
