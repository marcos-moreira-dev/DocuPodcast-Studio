package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorConfidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.HeadingDetectionRules;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.CharacterProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads .docupodcast.json payloads. */
public final class DocuPodcastProjectJsonReader {
    @SuppressWarnings("unchecked")
    public DocuPodcastProject read(String json) throws IOException {
        Object parsed = SimpleJsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> rawRoot)) {
            throw new IOException("DocuPodcast project JSON must be an object");
        }
        Map<String, Object> root = (Map<String, Object>) rawRoot;
        int version = intValue(root.getOrDefault("formatVersion", DocuPodcastProjectFormat.LEGACY_FORMAT_VERSION));
        if (version > DocuPodcastProjectFormat.CURRENT_FORMAT_VERSION) {
            throw new IOException("Unsupported future DocuPodcast formatVersion: " + version);
        }
        Map<String, Object> projectMap = object(root.get("project"), "project");
        boolean explicitMode = projectMap.containsKey("mode");
        ProjectMetadata metadata = readMetadata(projectMap);
        ReadingProfile readingProfile = readReadingProfile(optionalObject(root.get("readingProfile")));
        VoiceLibrary voiceLibrary = readVoiceLibrary(optionalObject(root.get("voiceLibrary")));
        ProjectAssetCatalog assets = readAssets(object(root.getOrDefault("assets", Map.of("items", List.of())), "assets"));
        List<NarrativeLayerAssignment> narrativeLayers = readNarrativeLayers(optionalObject(root.get("narrativeLayers")));
        TheatreProjectLayer theatre = readTheatre(optionalObject(root.get("theatre")));
        StudyProjectLayer study = readStudy(optionalObject(root.get("study")));
        Map<String, String> view = readView(optionalObject(root.get("view")));
        if (!explicitMode) {
            metadata = metadata.withMode(new ProjectModePolicy().inferLegacy(metadata.kind(), theatre));
        }
        return new DocuPodcastProject(metadata, assets, readingProfile, voiceLibrary, narrativeLayers, theatre, study, view);
    }

    private static ProjectMetadata readMetadata(Map<String, Object> project) throws IOException {
        ProjectMode mode = project.containsKey("mode")
                ? enumValue(ProjectMode.class, string(project.get("mode"), "project.mode"), "project.mode")
                : null;
        return new ProjectMetadata(
                string(project.get("id"), "project.id"),
                string(project.get("title"), "project.title"),
                stringOrDefault(project.get("description"), ""),
                stringOrDefault(project.get("language"), "es"),
                enumValue(ProjectKind.class, stringOrDefault(project.get("kind"), ProjectKind.EMPTY.name()), "project.kind"),
                mode,
                enumValue(ProjectStatus.class, stringOrDefault(project.get("status"), ProjectStatus.DRAFT.name()), "project.status"),
                instant(project.get("createdAt"), "project.createdAt"),
                instant(project.get("updatedAt"), "project.updatedAt")
        );
    }

    private static ReadingProfile readReadingProfile(Map<String, Object> map) throws IOException {
        if (map.isEmpty()) {
            return ReadingProfile.academicDefaults();
        }
        HeadingDetectionRules defaults = HeadingDetectionRules.academicDefaults();
        Map<String, Object> rules = optionalObject(map.get("headingRules"));
        HeadingDetectionRules headingRules = new HeadingDetectionRules(
                stringListOrDefault(rules.get("titleStyleKeywords"), defaults.titleStyleKeywords()),
                stringListOrDefault(rules.get("headingStyleKeywords"), defaults.headingStyleKeywords()),
                stringListOrDefault(rules.get("subheadingStyleKeywords"), defaults.subheadingStyleKeywords()),
                intOrDefault(rules.get("maxShortBoldWords"), defaults.maxShortBoldWords()),
                booleanOrDefault(rules.get("treatShortBoldParagraphAsSubheading"), defaults.treatShortBoldParagraphAsSubheading())
        );
        return new ReadingProfile(
                stringOrDefault(map.get("id"), ReadingProfile.academicDefaults().id()),
                stringOrDefault(map.get("name"), ReadingProfile.academicDefaults().name()),
                stringOrDefault(map.get("description"), ReadingProfile.academicDefaults().description()),
                headingRules,
                enumValue(ImageNarrationPolicy.class, stringOrDefault(map.get("imagePolicy"), ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT.name()), "readingProfile.imagePolicy"),
                enumValue(TableNarrationPolicy.class, stringOrDefault(map.get("tablePolicy"), TableNarrationPolicy.ANNOUNCE_SUMMARY.name()), "readingProfile.tablePolicy")
        );
    }

    @SuppressWarnings("unchecked")

    private static VoiceLibrary readVoiceLibrary(Map<String, Object> map) throws IOException {
        if (map.isEmpty()) {
            return VoiceLibrary.defaults();
        }
        String id = stringOrDefault(map.get("id"), VoiceLibrary.defaults().id());
        String notes = stringOrDefault(map.get("notes"), "");
        Instant updatedAt = map.get("updatedAt") == null ? Instant.now() : instant(map.get("updatedAt"), "voiceLibrary.updatedAt");
        List<VoiceProfile> voices = readVoiceProfiles(map.get("voices"));
        List<CharacterProfile> characters = readCharacters(map.get("characters"));
        List<PerformanceStyle> styles = readStyles(map.get("styles"));
        List<VoiceReferenceSampleSet> referenceSampleSets = readReferenceSampleSets(map.get("referenceSampleSets"));
        VoiceLibrary parsed = new VoiceLibrary(id, voices.isEmpty() ? VoiceLibrary.defaults().voices() : voices,
                characters.isEmpty() ? VoiceLibrary.defaults().characters() : characters,
                styles.isEmpty() ? VoiceLibrary.defaults().styles() : styles,
                referenceSampleSets, updatedAt, notes);
        return withOfficialAdvancedPresets(parsed);
    }

    private static VoiceLibrary withOfficialAdvancedPresets(VoiceLibrary library) {
        VoiceLibrary updated = library;
        if (updated.voiceById("VOC-OWN-PLACEHOLDER").isPresent()) {
            updated = updated.withoutVoice("VOC-OWN-PLACEHOLDER");
        }
        for (VoiceProfile preset : OfficialAdvancedVoicePresetCatalog.profiles()) {
            if (updated.voiceById(preset.id()).isEmpty()) {
                updated = updated.withVoice(preset);
            }
        }
        for (VoiceReferenceSampleSet sampleSet : OfficialAdvancedVoicePresetCatalog.sampleSets()) {
            if (updated.referenceSampleSetByVoiceId(sampleSet.voiceProfileId()).isEmpty()) {
                updated = updated.withReferenceSampleSet(sampleSet);
            }
        }
        return updated;
    }

    @SuppressWarnings("unchecked")
    private static List<VoiceProfile> readVoiceProfiles(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.voices must be an array");
        }
        ArrayList<VoiceProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.voices entries must be objects");
            }
            Map<String, Object> voice = (Map<String, Object>) raw;
            result.add(new VoiceProfile(
                    string(voice.get("id"), "voice.id"),
                    string(voice.get("displayName"), "voice.displayName"),
                    enumValue(VoiceProfileType.class, stringOrDefault(voice.get("type"), VoiceProfileType.UNKNOWN.name()), "voice.type"),
                    enumValue(VoiceEngineType.class, stringOrDefault(voice.get("engineType"), VoiceEngineType.UNKNOWN.name()), "voice.engineType"),
                    stringOrDefault(voice.get("language"), "es"),
                    stringOrDefault(voice.get("sampleAssetId"), ""),
                    stringOrDefault(voice.get("modelAssetId"), ""),
                    enumValue(VoiceQualityPreset.class, stringOrDefault(voice.get("qualityPreset"), VoiceQualityPreset.BALANCED.name()), "voice.qualityPreset"),
                    booleanOrDefault(voice.get("supportsStyleTransfer"), false),
                    stringOrDefault(voice.get("consentNote"), ""),
                    Map.of()
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<VoiceReferenceSampleSet> readReferenceSampleSets(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.referenceSampleSets must be an array");
        }
        ArrayList<VoiceReferenceSampleSet> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.referenceSampleSets entries must be objects");
            }
            Map<String, Object> set = (Map<String, Object>) raw;
            String voiceProfileId = string(set.get("voiceProfileId"), "referenceSampleSet.voiceProfileId");
            result.add(new VoiceReferenceSampleSet(voiceProfileId, readReferenceSamples(set.get("samples"), voiceProfileId)));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<VoiceReferenceSample> readReferenceSamples(Object value, String fallbackVoiceProfileId) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("referenceSampleSet.samples must be an array");
        }
        ArrayList<VoiceReferenceSample> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("referenceSampleSet.samples entries must be objects");
            }
            Map<String, Object> sample = (Map<String, Object>) raw;
            result.add(new VoiceReferenceSample(
                    string(sample.get("id"), "referenceSample.id"),
                    stringOrDefault(sample.get("voiceProfileId"), fallbackVoiceProfileId),
                    enumValue(VoiceReferenceTone.class, stringOrDefault(sample.get("tone"), VoiceReferenceTone.NEUTRAL.name()), "referenceSample.tone"),
                    string(sample.get("fileUri"), "referenceSample.fileUri"),
                    enumValue(VoiceSampleOrigin.class, stringOrDefault(sample.get("origin"), VoiceSampleOrigin.IMPORTED_FILE.name()), "referenceSample.origin"),
                    enumValue(VoiceFileOwnership.class, stringOrDefault(sample.get("ownership"), VoiceFileOwnership.EXTERNAL_REFERENCE.name()), "referenceSample.ownership"),
                    longOrDefault(sample.get("durationMillis"), 0),
                    sample.get("createdAt") == null ? Instant.now() : instant(sample.get("createdAt"), "referenceSample.createdAt"),
                    stringOrDefault(sample.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<CharacterProfile> readCharacters(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.characters must be an array");
        }
        ArrayList<CharacterProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.characters entries must be objects");
            }
            Map<String, Object> character = (Map<String, Object>) raw;
            result.add(new CharacterProfile(
                    string(character.get("id"), "character.id"),
                    string(character.get("displayName"), "character.displayName"),
                    stringOrDefault(character.get("defaultVoiceProfileId"), "VOC-NARRATOR"),
                    stringOrDefault(character.get("defaultPerformanceStyleId"), "STY-NEUTRAL"),
                    stringOrDefault(character.get("description"), ""),
                    Map.of()
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<PerformanceStyle> readStyles(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.styles must be an array");
        }
        ArrayList<PerformanceStyle> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.styles entries must be objects");
            }
            Map<String, Object> style = (Map<String, Object>) raw;
            result.add(new PerformanceStyle(
                    string(style.get("id"), "style.id"),
                    string(style.get("displayName"), "style.displayName"),
                    stringOrDefault(style.get("description"), ""),
                    booleanOrDefault(style.get("requiresEngineSupport"), false),
                    Map.of()
            ));
        }
        return List.copyOf(result);
    }

    private static ProjectAssetCatalog readAssets(Map<String, Object> assets) throws IOException {
        Object rawItems = assets.getOrDefault("items", List.of());
        if (!(rawItems instanceof List<?> list)) {
            throw new IOException("assets.items must be an array");
        }
        List<ProjectAssetReference> references = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawAsset)) {
                throw new IOException("assets.items entries must be objects");
            }
            Map<String, Object> asset = (Map<String, Object>) rawAsset;
            references.add(new ProjectAssetReference(
                    string(asset.get("id"), "asset.id"),
                    enumValue(ProjectAssetKind.class, string(asset.get("kind"), "asset.kind"), "asset.kind"),
                    string(asset.get("displayName"), "asset.displayName"),
                    string(asset.get("relativePath"), "asset.relativePath"),
                    stringOrDefault(asset.get("mimeType"), ""),
                    stringOrDefault(asset.get("purpose"), ""),
                    stringOrDefault(asset.get("checksum"), ""),
                    stringOrDefault(asset.get("notes"), "")
            ));
        }
        return new ProjectAssetCatalog(references);
    }


    @SuppressWarnings("unchecked")
    private static List<NarrativeLayerAssignment> readNarrativeLayers(Map<String, Object> narrativeLayers) throws IOException {
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


    private static TextAnchor readTextAnchor(Map<String, Object> anchor, String assignmentId, DocumentTextRange fallbackRange) throws IOException {
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

    private static TheatreProjectLayer readTheatre(Map<String, Object> theatre) throws IOException {
        if (theatre.isEmpty()) {
            return TheatreProjectLayer.empty();
        }
        return new TheatreProjectLayer(
                readIntervenciones(theatre.get("intervenciones")),
                readTheatreCharacters(theatre.get("characters")),
                readVoiceRoleAliases(theatre.get("voiceRoleAliases")),
                readCharacterImages(theatre.get("characterImages")),
                readIntervencionesVisuales(theatre.get("intervencionesVisuales")),
                readIntermediateFrames(theatre.get("intermediateFrames")),
                readTheatreActs(theatre.get("acts")),
                readScenes(theatre.get("scenes")),
                readSpatialPositions(theatre.get("positions")),
                readTheatreActions(theatre.get("actions")),
                readTextActionPlacements(theatre.get("textActionPlacements")),
                readObjectImages(theatre.get("objectImages")),
                readTheatreObjects(theatre.get("objects")),
                readTheatreAudioTracks(theatre.get("audioTracks")),
                readCameraReferences(theatre.get("cameraReferences")),
                readCameraCues(theatre.get("cameraCues")),
                readStageBackdrops(theatre.get("stageBackdrops")),
                readStageBackdropAssignments(theatre.get("stageBackdropAssignments")),
                readChoralVoiceAssignments(theatre.get("choralVoiceAssignments"))
        );
    }

    private static StudyProjectLayer readStudy(Map<String, Object> study) throws IOException {
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

    private static DocumentStudyVideoConfiguration readDocumentaryVideoConfiguration(Map<String, Object> map)
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
                                "study.documentaryVideoConfiguration.paragraph.mascotPosition")));
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
        return new DocumentStudyVideoConfiguration(
                stringOrDefault(map.get("videoTitle"), ""),
                doubleOrDefault(map.get("defaultTableDurationSeconds"), 6.0),
                paragraphs, tables, tracks,
                stringListOrDefault(map.get("disabledBlockIds"), List.of()),
                closingSlides);
    }

    private static List<StudySourceReference> readStudySources(Object value) throws IOException {
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
    private static List<TheatreProjectLayer.Intervencion> readIntervenciones(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intervenciones must be an array");
        }
        ArrayList<TheatreProjectLayer.Intervencion> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intervenciones entries must be objects");
            }
            Map<String, Object> alias = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.Intervencion(
                    string(alias.get("id"), "theatre.intervencion.id"),
                    string(alias.get("blockId"), "theatre.intervencion.blockId"),
                    intOrDefault(alias.get("sequenceIndex"), result.size() + 1)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.CharacterProfile> readTheatreCharacters(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.characters must be an array");
        }
        ArrayList<TheatreProjectLayer.CharacterProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.characters entries must be objects");
            }
            Map<String, Object> character = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CharacterProfile(
                    string(character.get("id"), "theatre.character.id"),
                    string(character.get("displayName"), "theatre.character.displayName"),
                    stringListOrDefault(character.get("aliases"), List.of()),
                    stringOrDefault(character.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.VoiceRoleAlias> readVoiceRoleAliases(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.voiceRoleAliases must be an array");
        }
        ArrayList<TheatreProjectLayer.VoiceRoleAlias> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.voiceRoleAliases entries must be objects");
            }
            Map<String, Object> alias = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.VoiceRoleAlias(
                    string(alias.get("id"), "theatre.voiceRoleAlias.id"),
                    string(alias.get("displayName"), "theatre.voiceRoleAlias.displayName"),
                    string(alias.get("voiceProfileId"), "theatre.voiceRoleAlias.voiceProfileId"),
                    stringOrDefault(alias.get("characterId"), ""),
                    stringOrDefault(alias.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.CharacterImage> readCharacterImages(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.characterImages must be an array");
        }
        ArrayList<TheatreProjectLayer.CharacterImage> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.characterImages entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            String id = stringOrDefault(image.get("id"), "");
            if (id.isBlank()) {
                id = "CHARIMG-" + String.format(java.util.Locale.ROOT, "%03d", result.size() + 1);
            }
            result.add(new TheatreProjectLayer.CharacterImage(
                    id,
                    string(image.get("characterId"), "theatre.characterImage.characterId"),
                    stringOrDefault(image.get("sceneId"), ""),
                    string(image.get("view"), "theatre.characterImage.view"),
                    string(image.get("assetId"), "theatre.characterImage.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.IntervencionVisual> readIntervencionesVisuales(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intervencionesVisuales must be an array");
        }
        ArrayList<TheatreProjectLayer.IntervencionVisual> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intervencionesVisuales entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.IntervencionVisual(
                    string(image.get("intervencionId"), "theatre.intervencionVisual.intervencionId"),
                    string(image.get("assetId"), "theatre.intervencionVisual.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.IntermediateFrame> readIntermediateFrames(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.intermediateFrames must be an array");
        }
        ArrayList<TheatreProjectLayer.IntermediateFrame> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.intermediateFrames entries must be objects");
            }
            Map<String, Object> frame = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.IntermediateFrame(
                    string(frame.get("fromIntervencionId"), "theatre.intermediateFrame.fromIntervencionId"),
                    string(frame.get("toIntervencionId"), "theatre.intermediateFrame.toIntervencionId"),
                    string(frame.get("assetId"), "theatre.intermediateFrame.assetId"),
                    stringOrDefault(frame.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.CameraReference> readCameraReferences(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.cameraReferences must be an array");
        }
        ArrayList<TheatreProjectLayer.CameraReference> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.cameraReferences entries must be objects");
            }
            Map<String, Object> camera = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CameraReference(
                    string(camera.get("id"), "theatre.cameraReference.id"),
                    string(camera.get("displayName"), "theatre.cameraReference.displayName"),
                    string(camera.get("assetId"), "theatre.cameraReference.assetId"),
                    stringOrDefault(camera.get("distance"), ""),
                    stringOrDefault(camera.get("orientation"), ""),
                    stringOrDefault(camera.get("height"), ""),
                    booleanOrDefault(camera.get("defaultCamera"), false),
                    stringOrDefault(camera.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.CameraCue> readCameraCues(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.cameraCues must be an array");
        }
        ArrayList<TheatreProjectLayer.CameraCue> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.cameraCues entries must be objects");
            }
            Map<String, Object> cue = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.CameraCue(
                    string(cue.get("intervencionId"), "theatre.cameraCue.intervencionId"),
                    string(cue.get("cameraId"), "theatre.cameraCue.cameraId"),
                    stringOrDefault(cue.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.StageBackdrop> readStageBackdrops(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.stageBackdrops must be an array");
        }
        ArrayList<TheatreProjectLayer.StageBackdrop> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.stageBackdrops entries must be objects");
            }
            Map<String, Object> backdrop = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.StageBackdrop(
                    string(backdrop.get("id"), "theatre.stageBackdrop.id"),
                    string(backdrop.get("displayName"), "theatre.stageBackdrop.displayName"),
                    string(backdrop.get("assetId"), "theatre.stageBackdrop.assetId"),
                    stringOrDefault(backdrop.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.StageBackdropAssignment> readStageBackdropAssignments(Object value)
            throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.stageBackdropAssignments must be an array");
        }
        ArrayList<TheatreProjectLayer.StageBackdropAssignment> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.stageBackdropAssignments entries must be objects");
            }
            Map<String, Object> assignment = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.StageBackdropAssignment(
                    string(assignment.get("scope"), "theatre.stageBackdropAssignment.scope"),
                    string(assignment.get("scopeId"), "theatre.stageBackdropAssignment.scopeId"),
                    string(assignment.get("backdropId"), "theatre.stageBackdropAssignment.backdropId"),
                    stringOrDefault(assignment.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.ChoralVoiceAssignment> readChoralVoiceAssignments(Object value)
            throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.choralVoiceAssignments must be an array");
        }
        ArrayList<TheatreProjectLayer.ChoralVoiceAssignment> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.choralVoiceAssignments entries must be objects");
            }
            Map<String, Object> assignment = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.ChoralVoiceAssignment(
                    string(assignment.get("intervencionId"), "theatre.choralVoiceAssignment.intervencionId"),
                    stringListOrDefault(assignment.get("participantCharacterIds"), List.of()),
                    stringOrDefault(assignment.get("mixedAudioAssetId"), ""),
                    stringOrDefault(assignment.get("sourceFingerprint"), ""),
                    stringOrDefault(assignment.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.TheatreAudioTrack> readTheatreAudioTracks(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.audioTracks must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAudioTrack> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.audioTracks entries must be objects");
            }
            Map<String, Object> track = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAudioTrack(
                    string(track.get("id"), "theatre.audioTrack.id"),
                    string(track.get("assetId"), "theatre.audioTrack.assetId"),
                    stringOrDefault(track.get("startIntervencionId"), ""),
                    stringOrDefault(track.get("startSegmentId"), ""),
                    doubleOrDefault(track.get("sourceStartSeconds"), 0.0),
                    doubleOrDefault(track.get("sourceEndSeconds"), 0.0),
                    enumValue(TheatreProjectLayer.AudioTrackEndMode.class,
                            stringOrDefault(track.get("endMode"), TheatreProjectLayer.AudioTrackEndMode.FILE_END.name()),
                            "theatre.audioTrack.endMode"),
                    doubleOrDefault(track.get("volume"), 0.30),
                    doubleOrDefault(track.get("sourceDurationSeconds"), 0.0),
                    booleanOrDefault(track.get("gentleFade"), false)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.TheatreAct> readTheatreActs(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.acts must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAct> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.acts entries must be objects");
            }
            Map<String, Object> act = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAct(
                    string(act.get("id"), "theatre.act.id"),
                    string(act.get("displayName"), "theatre.act.displayName"),
                    stringOrDefault(act.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.Scene> readScenes(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.scenes must be an array");
        }
        ArrayList<TheatreProjectLayer.Scene> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.scenes entries must be objects");
            }
            Map<String, Object> scene = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.Scene(
                    string(scene.get("id"), "theatre.scene.id"),
                    string(scene.get("displayName"), "theatre.scene.displayName"),
                    stringOrDefault(scene.get("notes"), ""),
                    stringOrDefault(scene.get("actId"), ""),
                    stringOrDefault(scene.get("spatialMapAssetId"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.SpatialPosition> readSpatialPositions(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.positions must be an array");
        }
        ArrayList<TheatreProjectLayer.SpatialPosition> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.positions entries must be objects");
            }
            Map<String, Object> position = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.SpatialPosition(
                    string(position.get("sceneId"), "theatre.position.sceneId"),
                    string(position.get("alias"), "theatre.position.alias"),
                    stringOrDefault(position.get("characterId"), ""),
                    doubleOrDefault(position.get("x"), 0),
                    doubleOrDefault(position.get("y"), 0),
                    stringOrDefault(position.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.TheatreAction> readTheatreActions(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.actions must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreAction> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.actions entries must be objects");
            }
            Map<String, Object> action = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreAction(
                    string(action.get("sceneId"), "theatre.action.sceneId"),
                    string(action.get("fromAlias"), "theatre.action.fromAlias"),
                    string(action.get("toAlias"), "theatre.action.toAlias"),
                    stringOrDefault(action.get("characterId"), ""),
                    stringOrDefault(action.get("description"), ""),
                    booleanOrDefault(action.get("showArrow"), true)
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.TextActionPlacement> readTextActionPlacements(Object value) throws IOException {
        List<Map<String, Object>> items = castList(value, "theatre.textActionPlacements");
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        ArrayList<TheatreProjectLayer.TextActionPlacement> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, String> characterLocations = new LinkedHashMap<>();
            Object locs = item.get("characterLocations");
            if (locs instanceof Map<?, ?> rawLocs) {
                for (var entry : rawLocs.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        characterLocations.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }
            result.add(new TheatreProjectLayer.TextActionPlacement(
                    string(item.get("intervencionId"), "theatre.textActionPlacement.intervencionId"),
                    string(item.get("sceneId"), "theatre.textActionPlacement.sceneId"),
                    stringOrDefault(item.get("characterId"), ""),
                    stringOrDefault(item.get("origin"), ""),
                    stringOrDefault(item.get("destination"), ""),
                    stringOrDefault(item.get("interactionTarget"), ""),
                    characterLocations));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.TheatreObject> readTheatreObjects(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.objects must be an array");
        }
        ArrayList<TheatreProjectLayer.TheatreObject> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.objects entries must be objects");
            }
            Map<String, Object> object = (Map<String, Object>) raw;
            result.add(new TheatreProjectLayer.TheatreObject(
                    string(object.get("id"), "theatre.object.id"),
                    string(object.get("displayName"), "theatre.object.displayName"),
                    stringOrDefault(object.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<TheatreProjectLayer.ObjectImage> readObjectImages(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("theatre.objectImages must be an array");
        }
        ArrayList<TheatreProjectLayer.ObjectImage> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("theatre.objectImages entries must be objects");
            }
            Map<String, Object> image = (Map<String, Object>) raw;
            String id = stringOrDefault(image.get("id"), "");
            if (id.isBlank()) {
                id = "OBJIMG-" + String.format(java.util.Locale.ROOT, "%03d", result.size() + 1);
            }
            result.add(new TheatreProjectLayer.ObjectImage(
                    id,
                    string(image.get("objectId"), "theatre.objectImage.objectId"),
                    stringOrDefault(image.get("sceneId"), ""),
                    string(image.get("view"), "theatre.objectImage.view"),
                    string(image.get("assetId"), "theatre.objectImage.assetId"),
                    stringOrDefault(image.get("notes"), "")
            ));
        }
        return List.copyOf(result);
    }

    private static Map<String, String> readView(Map<String, Object> view) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : view.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String name) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException(name + " must be an object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) throws IOException {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("Expected object");
        }
        return (Map<String, Object>) map;
    }

    private static String string(Object value, String field) throws IOException {
        if (!(value instanceof String string)) {
            throw new IOException(field + " must be a string");
        }
        return string;
    }

    private static String stringOrDefault(Object value, String defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String string)) {
            throw new IOException("Expected string value");
        }
        return string;
    }

    private static Instant instant(Object value, String field) throws IOException {
        try {
            return Instant.parse(string(value, field));
        } catch (RuntimeException ex) {
            throw new IOException(field + " must be an ISO-8601 instant", ex);
        }
    }

    private static int intValue(Object value) throws IOException {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("formatVersion must be a number");
    }

    private static int intOrDefault(Object value, int defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static long longOrDefault(Object value, long defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static double doubleOrDefault(Object value, double defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IOException("Expected numeric value");
    }

    private static boolean booleanOrDefault(Object value, boolean defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IOException("Expected boolean value");
    }

    private static List<String> stringListOrDefault(Object value, List<String> defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("Expected array of strings");
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) {
                throw new IOException("Expected array of strings");
            }
            result.add(text);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value, String name) throws IOException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException(name + " must be an array");
        }
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException(name + " entries must be objects");
            }
            result.add((Map<String, Object>) raw);
        }
        return result;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String field) throws IOException {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IOException(field + " has unsupported value: " + value, ex);
        }
    }
}
