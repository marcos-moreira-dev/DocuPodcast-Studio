package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.CharacterProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Writes the stable .docupodcast.json payload. */
public final class DocuPodcastProjectJsonWriter {
    public String write(DocuPodcastProject project) {
        Objects.requireNonNull(project, "project");
        StringBuilder out = new StringBuilder(8192);
        out.append("{\n");
        field(out, 1, "formatVersion", Integer.toString(DocuPodcastProjectFormat.CURRENT_FORMAT_VERSION));
        out.append(",\n");
        writeProject(out, project.metadata());
        out.append(",\n");
        writeReadingProfile(out, project.readingProfile());
        out.append(",\n");
        writeVoiceLibrary(out, project.voiceLibrary());
        out.append(",\n");
        writeAssets(out, project);
        out.append(",\n");
        writeNarrativeLayers(out, project.narrativeLayerAssignments());
        out.append(",\n");
        writeTheatre(out, project.theatre());
        out.append(",\n");
        writeStudy(out, project.study());
        out.append(",\n");
        writeView(out, project.viewState());
        out.append("\n}\n");
        return out.toString();
    }

    private static void writeProject(StringBuilder out, ProjectMetadata metadata) {
        indent(out, 1).append("\"project\": {\n");
        field(out, 2, "id", quote(metadata.id())); out.append(",\n");
        field(out, 2, "title", quote(metadata.title())); out.append(",\n");
        field(out, 2, "description", quote(metadata.description())); out.append(",\n");
        field(out, 2, "language", quote(metadata.language())); out.append(",\n");
        field(out, 2, "kind", quote(metadata.kind().name())); out.append(",\n");
        field(out, 2, "mode", quote(metadata.mode().name())); out.append(",\n");
        field(out, 2, "status", quote(metadata.status().name())); out.append(",\n");
        field(out, 2, "createdAt", quote(metadata.createdAt().toString())); out.append(",\n");
        field(out, 2, "updatedAt", quote(metadata.updatedAt().toString())); out.append("\n");
        indent(out, 1).append("}");
    }

    private static void writeReadingProfile(StringBuilder out, ReadingProfile profile) {
        indent(out, 1).append("\"readingProfile\": {\n");
        field(out, 2, "id", quote(profile.id())); out.append(",\n");
        field(out, 2, "name", quote(profile.name())); out.append(",\n");
        field(out, 2, "description", quote(profile.description())); out.append(",\n");
        field(out, 2, "imagePolicy", quote(profile.imagePolicy().name())); out.append(",\n");
        field(out, 2, "tablePolicy", quote(profile.tablePolicy().name())); out.append(",\n");
        indent(out, 2).append("\"headingRules\": {\n");
        stringArrayField(out, 3, "titleStyleKeywords", profile.headingRules().titleStyleKeywords()); out.append(",\n");
        stringArrayField(out, 3, "headingStyleKeywords", profile.headingRules().headingStyleKeywords()); out.append(",\n");
        stringArrayField(out, 3, "subheadingStyleKeywords", profile.headingRules().subheadingStyleKeywords()); out.append(",\n");
        field(out, 3, "maxShortBoldWords", Integer.toString(profile.headingRules().maxShortBoldWords())); out.append(",\n");
        field(out, 3, "treatShortBoldParagraphAsSubheading", Boolean.toString(profile.headingRules().treatShortBoldParagraphAsSubheading())); out.append("\n");
        indent(out, 2).append("}\n");
        indent(out, 1).append("}");
    }


    private static void writeVoiceLibrary(StringBuilder out, VoiceLibrary library) {
        indent(out, 1).append("\"voiceLibrary\": {\n");
        field(out, 2, "id", quote(library.id())); out.append(",\n");
        field(out, 2, "updatedAt", quote(library.updatedAt().toString())); out.append(",\n");
        field(out, 2, "notes", quote(library.notes())); out.append(",\n");
        indent(out, 2).append("\"voices\": [");
        if (!library.voices().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.voices().size(); i++) {
            VoiceProfile voice = library.voices().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(voice.id())); out.append(",\n");
            field(out, 4, "displayName", quote(voice.displayName())); out.append(",\n");
            field(out, 4, "type", quote(voice.type().name())); out.append(",\n");
            field(out, 4, "engineType", quote(voice.engineType().name())); out.append(",\n");
            field(out, 4, "language", quote(voice.language())); out.append(",\n");
            field(out, 4, "sampleAssetId", quote(voice.sampleAssetId())); out.append(",\n");
            field(out, 4, "modelAssetId", quote(voice.modelAssetId())); out.append(",\n");
            field(out, 4, "qualityPreset", quote(voice.qualityPreset().name())); out.append(",\n");
            field(out, 4, "supportsStyleTransfer", Boolean.toString(voice.supportsStyleTransfer())); out.append(",\n");
            field(out, 4, "consentNote", quote(voice.consentNote())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.voices().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("],\n");
        writeReferenceSampleSets(out, library.referenceSampleSets(), 2);
        out.append(",\n");
        indent(out, 2).append("\"characters\": [");
        if (!library.characters().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.characters().size(); i++) {
            CharacterProfile character = library.characters().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(character.id())); out.append(",\n");
            field(out, 4, "displayName", quote(character.displayName())); out.append(",\n");
            field(out, 4, "defaultVoiceProfileId", quote(character.defaultVoiceProfileId())); out.append(",\n");
            field(out, 4, "defaultPerformanceStyleId", quote(character.defaultPerformanceStyleId())); out.append(",\n");
            field(out, 4, "description", quote(character.description())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.characters().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("],\n");
        indent(out, 2).append("\"styles\": [");
        if (!library.styles().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.styles().size(); i++) {
            PerformanceStyle style = library.styles().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(style.id())); out.append(",\n");
            field(out, 4, "displayName", quote(style.displayName())); out.append(",\n");
            field(out, 4, "description", quote(style.description())); out.append(",\n");
            field(out, 4, "requiresEngineSupport", Boolean.toString(style.requiresEngineSupport())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.styles().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }

    private static void writeDocumentaryVideoConfiguration(StringBuilder out,
                                                             DocumentStudyVideoConfiguration configuration,
                                                             int level) {
        DocumentStudyVideoConfiguration safe = configuration == null
                ? DocumentStudyVideoConfiguration.empty() : configuration;
        indent(out, level).append("\"documentaryVideoConfiguration\": {\n");
        field(out, level + 1, "videoTitle", quote(safe.videoTitle())); out.append(",\n");
        field(out, level + 1, "defaultTableDurationSeconds",
                Double.toString(safe.defaultTableDurationSeconds())); out.append(",\n");
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
            field(out, level + 3, "mascotPosition", quote(item.mascotPosition().name())); out.append("\n");
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

    private static void writeReferenceSampleSets(StringBuilder out, List<VoiceReferenceSampleSet> sampleSets, int level) {
        indent(out, level).append("\"referenceSampleSets\": [");
        if (!sampleSets.isEmpty()) { out.append("\n"); }
        for (int i = 0; i < sampleSets.size(); i++) {
            VoiceReferenceSampleSet sampleSet = sampleSets.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "voiceProfileId", quote(sampleSet.voiceProfileId())); out.append(",\n");
            indent(out, level + 2).append("\"samples\": [");
            if (!sampleSet.samples().isEmpty()) { out.append("\n"); }
            for (int j = 0; j < sampleSet.samples().size(); j++) {
                VoiceReferenceSample sample = sampleSet.samples().get(j);
                indent(out, level + 3).append("{\n");
                field(out, level + 4, "id", quote(sample.id())); out.append(",\n");
                field(out, level + 4, "voiceProfileId", quote(sample.voiceProfileId())); out.append(",\n");
                field(out, level + 4, "tone", quote(sample.tone().name())); out.append(",\n");
                field(out, level + 4, "fileUri", quote(sample.fileUri())); out.append(",\n");
                field(out, level + 4, "origin", quote(sample.origin().name())); out.append(",\n");
                field(out, level + 4, "ownership", quote(sample.ownership().name())); out.append(",\n");
                field(out, level + 4, "durationMillis", Long.toString(sample.durationMillis())); out.append(",\n");
                field(out, level + 4, "createdAt", quote(sample.createdAt().toString())); out.append(",\n");
                field(out, level + 4, "notes", quote(sample.notes())); out.append("\n");
                indent(out, level + 3).append("}");
                if (j < sampleSet.samples().size() - 1) { out.append(","); }
                out.append("\n");
            }
            indent(out, level + 2).append("]\n");
            indent(out, level + 1).append("}");
            if (i < sampleSets.size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeAssets(StringBuilder out, DocuPodcastProject project) {
        indent(out, 1).append("\"assets\": {\n");
        indent(out, 2).append("\"items\": [");
        if (!project.assets().references().isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < project.assets().references().size(); i++) {
            ProjectAssetReference asset = project.assets().references().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(asset.id())); out.append(",\n");
            field(out, 4, "kind", quote(asset.kind().name())); out.append(",\n");
            field(out, 4, "displayName", quote(asset.displayName())); out.append(",\n");
            field(out, 4, "relativePath", quote(asset.relativePath())); out.append(",\n");
            field(out, 4, "mimeType", quote(asset.mimeType())); out.append(",\n");
            field(out, 4, "purpose", quote(asset.purpose())); out.append(",\n");
            field(out, 4, "checksum", quote(asset.checksum())); out.append(",\n");
            field(out, 4, "notes", quote(asset.notes())); out.append("\n");
            indent(out, 3).append("}");
            if (i < project.assets().references().size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }


    private static void writeNarrativeLayers(StringBuilder out, List<NarrativeLayerAssignment> assignments) {
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


    private static void writeTextAnchor(StringBuilder out, TextAnchor anchor) {
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

    private static void writeTheatre(StringBuilder out, TheatreProjectLayer theatre) {
        indent(out, 1).append("\"theatre\": {\n");
        writeIntervenciones(out, theatre.intervenciones(), 2);
        out.append(",\n");
        writeTheatreCharacters(out, theatre.characters(), 2);
        out.append(",\n");
        writeVoiceRoleAliases(out, theatre.voiceRoleAliases(), 2);
        out.append(",\n");
        writeCharacterImages(out, theatre.characterImages(), 2);
        out.append(",\n");
        writeIntervencionesVisuales(out, theatre.intervencionesVisuales(), 2);
        out.append(",\n");
        writeIntermediateFrames(out, theatre.intermediateFrames(), 2);
        out.append(",\n");
        writeCameraReferences(out, theatre.cameraReferences(), 2);
        out.append(",\n");
        writeCameraCues(out, theatre.cameraCues(), 2);
        out.append(",\n");
        writeStageBackdrops(out, theatre.stageBackdrops(), 2);
        out.append(",\n");
        writeStageBackdropAssignments(out, theatre.stageBackdropAssignments(), 2);
        out.append(",\n");
        writeChoralVoiceAssignments(out, theatre.choralVoiceAssignments(), 2);
        out.append(",\n");
        writeTheatreActs(out, theatre.acts(), 2);
        out.append(",\n");
        writeScenes(out, theatre.scenes(), 2);
        out.append(",\n");
        writeSpatialPositions(out, theatre.positions(), 2);
        out.append(",\n");
        writeTheatreActions(out, theatre.actions(), 2);
        out.append(",\n");
        writeTextActionPlacements(out, theatre.textActionPlacements(), 2);
        out.append(",\n");
        writeObjectImages(out, theatre.objectImages(), 2);
        out.append(",\n");
        writeTheatreObjects(out, theatre.objects(), 2);
        out.append(",\n");
        writeTheatreAudioTracks(out, theatre.audioTracks(), 2);
        out.append("\n");
        indent(out, 1).append("}");
    }

    private static void writeTheatreAudioTracks(StringBuilder out,
                                                List<TheatreProjectLayer.TheatreAudioTrack> tracks,
                                                int level) {
        indent(out, level).append("\"audioTracks\": [");
        if (tracks != null && !tracks.isEmpty()) {
            out.append("\n");
        }
        List<TheatreProjectLayer.TheatreAudioTrack> safeTracks = tracks == null ? List.of() : tracks;
        for (int i = 0; i < safeTracks.size(); i++) {
            TheatreProjectLayer.TheatreAudioTrack track = safeTracks.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(track.id())); out.append(",\n");
            field(out, level + 2, "assetId", quote(track.assetId())); out.append(",\n");
            field(out, level + 2, "startIntervencionId", quote(track.startIntervencionId())); out.append(",\n");
            field(out, level + 2, "startSegmentId", quote(track.startSegmentId())); out.append(",\n");
            field(out, level + 2, "sourceStartSeconds", Double.toString(track.sourceStartSeconds())); out.append(",\n");
            field(out, level + 2, "sourceEndSeconds", Double.toString(track.sourceEndSeconds())); out.append(",\n");
            field(out, level + 2, "endMode", quote(track.endMode().name())); out.append(",\n");
            field(out, level + 2, "volume", Double.toString(track.volume())); out.append(",\n");
            field(out, level + 2, "sourceDurationSeconds", Double.toString(track.sourceDurationSeconds())); out.append(",\n");
            field(out, level + 2, "gentleFade", Boolean.toString(track.gentleFade())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeTracks.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeStudy(StringBuilder out, StudyProjectLayer study) {
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

    private static void writeStudySources(StringBuilder out, List<StudySourceReference> sources, int level) {
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

    private static void writeIntervenciones(StringBuilder out, List<TheatreProjectLayer.Intervencion> intervenciones, int level) {
        indent(out, level).append("\"intervenciones\": [");
        if (!intervenciones.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < intervenciones.size(); i++) {
            TheatreProjectLayer.Intervencion intervencion = intervenciones.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(intervencion.id())); out.append(",\n");
            field(out, level + 2, "blockId", quote(intervencion.blockId())); out.append(",\n");
            field(out, level + 2, "sequenceIndex", Integer.toString(intervencion.sequenceIndex())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < intervenciones.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeTheatreCharacters(StringBuilder out, List<TheatreProjectLayer.CharacterProfile> characters, int level) {
        indent(out, level).append("\"characters\": [");
        if (!characters.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < characters.size(); i++) {
            TheatreProjectLayer.CharacterProfile character = characters.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(character.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(character.displayName())); out.append(",\n");
            stringArrayField(out, level + 2, "aliases", character.aliases()); out.append(",\n");
            field(out, level + 2, "notes", quote(character.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < characters.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeVoiceRoleAliases(StringBuilder out, List<TheatreProjectLayer.VoiceRoleAlias> aliases, int level) {
        indent(out, level).append("\"voiceRoleAliases\": [");
        if (!aliases.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < aliases.size(); i++) {
            TheatreProjectLayer.VoiceRoleAlias alias = aliases.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(alias.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(alias.displayName())); out.append(",\n");
            field(out, level + 2, "voiceProfileId", quote(alias.voiceProfileId())); out.append(",\n");
            field(out, level + 2, "characterId", quote(alias.characterId())); out.append(",\n");
            field(out, level + 2, "notes", quote(alias.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < aliases.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeCharacterImages(StringBuilder out, List<TheatreProjectLayer.CharacterImage> images, int level) {
        indent(out, level).append("\"characterImages\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.CharacterImage image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(image.id())); out.append(",\n");
            field(out, level + 2, "characterId", quote(image.characterId())); out.append(",\n");
            field(out, level + 2, "sceneId", quote(image.sceneId())); out.append(",\n");
            field(out, level + 2, "view", quote(image.view())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeIntervencionesVisuales(StringBuilder out, List<TheatreProjectLayer.IntervencionVisual> images, int level) {
        indent(out, level).append("\"intervencionesVisuales\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.IntervencionVisual image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(image.intervencionId())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeIntermediateFrames(StringBuilder out, List<TheatreProjectLayer.IntermediateFrame> frames, int level) {
        List<TheatreProjectLayer.IntermediateFrame> safeFrames = frames == null ? List.of() : frames;
        indent(out, level).append("\"intermediateFrames\": [");
        if (!safeFrames.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeFrames.size(); i++) {
            TheatreProjectLayer.IntermediateFrame frame = safeFrames.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "fromIntervencionId", quote(frame.fromIntervencionId())); out.append(",\n");
            field(out, level + 2, "toIntervencionId", quote(frame.toIntervencionId())); out.append(",\n");
            field(out, level + 2, "assetId", quote(frame.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(frame.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeFrames.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeCameraReferences(StringBuilder out,
                                              List<TheatreProjectLayer.CameraReference> references,
                                              int level) {
        List<TheatreProjectLayer.CameraReference> safeReferences = references == null ? List.of() : references;
        indent(out, level).append("\"cameraReferences\": [");
        if (!safeReferences.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeReferences.size(); i++) {
            TheatreProjectLayer.CameraReference reference = safeReferences.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(reference.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(reference.displayName())); out.append(",\n");
            field(out, level + 2, "assetId", quote(reference.assetId())); out.append(",\n");
            field(out, level + 2, "distance", quote(reference.distance())); out.append(",\n");
            field(out, level + 2, "orientation", quote(reference.orientation())); out.append(",\n");
            field(out, level + 2, "height", quote(reference.height())); out.append(",\n");
            field(out, level + 2, "defaultCamera", Boolean.toString(reference.defaultCamera())); out.append(",\n");
            field(out, level + 2, "notes", quote(reference.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeReferences.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeCameraCues(StringBuilder out, List<TheatreProjectLayer.CameraCue> cues, int level) {
        List<TheatreProjectLayer.CameraCue> safeCues = cues == null ? List.of() : cues;
        indent(out, level).append("\"cameraCues\": [");
        if (!safeCues.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeCues.size(); i++) {
            TheatreProjectLayer.CameraCue cue = safeCues.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(cue.intervencionId())); out.append(",\n");
            field(out, level + 2, "cameraId", quote(cue.cameraId())); out.append(",\n");
            field(out, level + 2, "notes", quote(cue.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeCues.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeStageBackdrops(StringBuilder out,
                                            List<TheatreProjectLayer.StageBackdrop> backdrops,
                                            int level) {
        List<TheatreProjectLayer.StageBackdrop> safeBackdrops = backdrops == null ? List.of() : backdrops;
        indent(out, level).append("\"stageBackdrops\": [");
        if (!safeBackdrops.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeBackdrops.size(); i++) {
            TheatreProjectLayer.StageBackdrop backdrop = safeBackdrops.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(backdrop.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(backdrop.displayName())); out.append(",\n");
            field(out, level + 2, "assetId", quote(backdrop.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(backdrop.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeBackdrops.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeStageBackdropAssignments(StringBuilder out,
                                                      List<TheatreProjectLayer.StageBackdropAssignment> assignments,
                                                      int level) {
        List<TheatreProjectLayer.StageBackdropAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        indent(out, level).append("\"stageBackdropAssignments\": [");
        if (!safeAssignments.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeAssignments.size(); i++) {
            TheatreProjectLayer.StageBackdropAssignment assignment = safeAssignments.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "scope", quote(assignment.scope())); out.append(",\n");
            field(out, level + 2, "scopeId", quote(assignment.scopeId())); out.append(",\n");
            field(out, level + 2, "backdropId", quote(assignment.backdropId())); out.append(",\n");
            field(out, level + 2, "notes", quote(assignment.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeAssignments.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeChoralVoiceAssignments(StringBuilder out,
                                                    List<TheatreProjectLayer.ChoralVoiceAssignment> assignments,
                                                    int level) {
        List<TheatreProjectLayer.ChoralVoiceAssignment> safeAssignments =
                assignments == null ? List.of() : assignments;
        indent(out, level).append("\"choralVoiceAssignments\": [");
        if (!safeAssignments.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < safeAssignments.size(); i++) {
            TheatreProjectLayer.ChoralVoiceAssignment assignment = safeAssignments.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(assignment.intervencionId())); out.append(",\n");
            field(out, level + 2, "participantCharacterIds", stringArray(assignment.participantCharacterIds())); out.append(",\n");
            field(out, level + 2, "mixedAudioAssetId", quote(assignment.mixedAudioAssetId())); out.append(",\n");
            field(out, level + 2, "sourceFingerprint", quote(assignment.sourceFingerprint())); out.append(",\n");
            field(out, level + 2, "notes", quote(assignment.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < safeAssignments.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeTheatreActs(StringBuilder out, List<TheatreProjectLayer.TheatreAct> acts, int level) {
        indent(out, level).append("\"acts\": [");
        if (!acts.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < acts.size(); i++) {
            TheatreProjectLayer.TheatreAct act = acts.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(act.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(act.displayName())); out.append(",\n");
            field(out, level + 2, "notes", quote(act.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < acts.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeScenes(StringBuilder out, List<TheatreProjectLayer.Scene> scenes, int level) {
        indent(out, level).append("\"scenes\": [");
        if (!scenes.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < scenes.size(); i++) {
            TheatreProjectLayer.Scene scene = scenes.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(scene.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(scene.displayName())); out.append(",\n");
            field(out, level + 2, "actId", quote(scene.actId())); out.append(",\n");
            field(out, level + 2, "spatialMapAssetId", quote(scene.spatialMapAssetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(scene.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < scenes.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeSpatialPositions(StringBuilder out, List<TheatreProjectLayer.SpatialPosition> positions, int level) {
        indent(out, level).append("\"positions\": [");
        if (!positions.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < positions.size(); i++) {
            TheatreProjectLayer.SpatialPosition position = positions.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "sceneId", quote(position.sceneId())); out.append(",\n");
            field(out, level + 2, "alias", quote(position.alias())); out.append(",\n");
            field(out, level + 2, "characterId", quote(position.characterId())); out.append(",\n");
            field(out, level + 2, "x", Double.toString(position.x())); out.append(",\n");
            field(out, level + 2, "y", Double.toString(position.y())); out.append(",\n");
            field(out, level + 2, "notes", quote(position.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < positions.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeTheatreActions(StringBuilder out, List<TheatreProjectLayer.TheatreAction> actions, int level) {
        indent(out, level).append("\"actions\": [");
        if (!actions.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < actions.size(); i++) {
            TheatreProjectLayer.TheatreAction action = actions.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "sceneId", quote(action.sceneId())); out.append(",\n");
            field(out, level + 2, "fromAlias", quote(action.fromAlias())); out.append(",\n");
            field(out, level + 2, "toAlias", quote(action.toAlias())); out.append(",\n");
            field(out, level + 2, "characterId", quote(action.characterId())); out.append(",\n");
            field(out, level + 2, "description", quote(action.description())); out.append(",\n");
            field(out, level + 2, "showArrow", Boolean.toString(action.showArrow())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < actions.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeTextActionPlacements(StringBuilder out, List<TheatreProjectLayer.TextActionPlacement> placements, int level) {
        indent(out, level).append("\"textActionPlacements\": [\n");
        for (int i = 0; i < placements.size(); i++) {
            var p = placements.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "intervencionId", quote(p.intervencionId()));
            out.append(",\n");
            field(out, level + 2, "sceneId", quote(p.sceneId()));
            out.append(",\n");
            field(out, level + 2, "characterId", quote(p.characterId()));
            out.append(",\n");
            field(out, level + 2, "origin", quote(p.origin()));
            out.append(",\n");
            field(out, level + 2, "destination", quote(p.destination()));
            out.append(",\n");
            field(out, level + 2, "interactionTarget", quote(p.interactionTarget()));
            out.append(",\n");
            // write characterLocations map
            indent(out, level + 2).append("\"characterLocations\": {\n");
            var locs = p.characterLocations();
            int locIdx = 0;
            for (var entry : locs.entrySet()) {
                indent(out, level + 3).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
                if (locIdx < locs.size() - 1) {
                    out.append(",\n");
                }
                locIdx++;
            }
            out.append("\n");
            indent(out, level + 2).append("}\n");
            indent(out, level + 1).append("}");
            if (i < placements.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeTheatreObjects(StringBuilder out, List<TheatreProjectLayer.TheatreObject> objects, int level) {
        indent(out, level).append("\"objects\": [");
        if (!objects.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < objects.size(); i++) {
            TheatreProjectLayer.TheatreObject object = objects.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(object.id())); out.append(",\n");
            field(out, level + 2, "displayName", quote(object.displayName())); out.append(",\n");
            field(out, level + 2, "notes", quote(object.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < objects.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeObjectImages(StringBuilder out, List<TheatreProjectLayer.ObjectImage> images, int level) {
        indent(out, level).append("\"objectImages\": [");
        if (!images.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < images.size(); i++) {
            TheatreProjectLayer.ObjectImage image = images.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "id", quote(image.id())); out.append(",\n");
            field(out, level + 2, "objectId", quote(image.objectId())); out.append(",\n");
            field(out, level + 2, "sceneId", quote(image.sceneId())); out.append(",\n");
            field(out, level + 2, "view", quote(image.view())); out.append(",\n");
            field(out, level + 2, "assetId", quote(image.assetId())); out.append(",\n");
            field(out, level + 2, "notes", quote(image.notes())); out.append("\n");
            indent(out, level + 1).append("}");
            if (i < images.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    private static void writeView(StringBuilder out, Map<String, String> viewState) {
        indent(out, 1).append("\"view\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : viewState.entrySet()) {
            field(out, 2, entry.getKey(), quote(entry.getValue()));
            if (index < viewState.size() - 1) {
                out.append(",");
            }
            out.append("\n");
            index++;
        }
        indent(out, 1).append("}");
    }

    private static void stringArrayField(StringBuilder out, int level, String name, List<String> values) {
        indent(out, level).append(quote(name)).append(": [");
        for (int i = 0; i < values.size(); i++) {
            out.append(quote(values.get(i)));
            if (i < values.size() - 1) {
                out.append(", ");
            }
        }
        out.append("]");
    }

    private static String stringArray(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < safeValues.size(); i++) {
            out.append(quote(safeValues.get(i)));
            if (i < safeValues.size() - 1) {
                out.append(", ");
            }
        }
        return out.append("]").toString();
    }

    private static void field(StringBuilder out, int level, String name, String value) {
        indent(out, level).append(quote(name)).append(": ").append(value);
    }

    private static StringBuilder indent(StringBuilder out, int level) {
        return out.append("    ".repeat(level));
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(safe.length() + 16);
        escaped.append('"');
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
