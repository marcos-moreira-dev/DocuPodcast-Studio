package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectVisualProcessingSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;

/** Stable v1 facade; detailed section knowledge lives in dedicated codecs. */
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
        ProjectMetadata metadata = MetadataProjectJsonCodec.readMetadata(projectMap);
        ReadingProfile readingProfile = ReadingProfileProjectJsonCodec.readReadingProfile(optionalObject(root.get("readingProfile")));
        VoiceLibrary voiceLibrary = VoiceLibraryProjectJsonCodec.readVoiceLibrary(optionalObject(root.get("voiceLibrary")));
        ProjectAssetCatalog assets = AssetsProjectJsonCodec.readAssets(
                object(root.getOrDefault("assets", Map.of("items", List.of())), "assets"));
        List<NarrativeLayerAssignment> narrativeLayers =
                NarrativeLayersProjectJsonCodec.readNarrativeLayers(optionalObject(root.get("narrativeLayers")));
        NarrativeProjectLayer narrative = NarrativeProjectJsonCodec.readNarrative(optionalObject(root.get("narrative")));
        TheatreProjectLayer theatre = TheatreProjectJsonCodec.readTheatre(optionalObject(root.get("theatre")));
        StudyProjectLayer study = StudyProjectJsonCodec.readStudy(optionalObject(root.get("study")));
        LegacyDocumentSourceVisualAssetMigration.Result sourceVisualMigration =
                new LegacyDocumentSourceVisualAssetMigration().migrate(assets, study);
        assets = sourceVisualMigration.assets();
        study = sourceVisualMigration.study();
        ProjectVisualProcessingSettings visualProcessing =
                VisualProcessingProjectJsonCodec.read(optionalObject(root.get("visualProcessing")));
        Map<String, String> view = ViewStateProjectJsonCodec.readView(optionalObject(root.get("view")));
        if (!explicitMode) metadata = metadata.withMode(new ProjectModePolicy().inferLegacy(metadata.kind(), theatre));
        return new DocuPodcastProject(metadata, assets, readingProfile, voiceLibrary, narrativeLayers,
                narrative, theatre, study, visualProcessing, view);
    }
}
