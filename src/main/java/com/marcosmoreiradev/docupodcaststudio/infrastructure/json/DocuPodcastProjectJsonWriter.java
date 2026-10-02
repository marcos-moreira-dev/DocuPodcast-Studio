package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Objects;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.field;

/** Stable v1 facade; field order is deliberately unchanged. */
public final class DocuPodcastProjectJsonWriter {
    public String write(DocuPodcastProject project) {
        Objects.requireNonNull(project, "project");
        StringBuilder out = new StringBuilder(8192);
        out.append("{\n");
        field(out, 1, "formatVersion", Integer.toString(DocuPodcastProjectFormat.CURRENT_FORMAT_VERSION));
        out.append(",\n");
        MetadataProjectJsonCodec.writeProject(out, project.metadata());
        out.append(",\n");
        ReadingProfileProjectJsonCodec.writeReadingProfile(out, project.readingProfile());
        out.append(",\n");
        VoiceLibraryProjectJsonCodec.writeVoiceLibrary(out, project.voiceLibrary());
        out.append(",\n");
        AssetsProjectJsonCodec.writeAssets(out, project);
        out.append(",\n");
        NarrativeLayersProjectJsonCodec.writeNarrativeLayers(out, project.narrativeLayerAssignments());
        out.append(",\n");
        NarrativeProjectJsonCodec.writeNarrative(out, project.narrative());
        out.append(",\n");
        TheatreProjectJsonCodec.writeTheatre(out, project.theatre());
        out.append(",\n");
        StudyProjectJsonCodec.writeStudy(out, project.study());
        out.append(",\n");
        if (project.visualProcessing().hasOverrides()) {
            VisualProcessingProjectJsonCodec.write(out, project.visualProcessing());
            out.append(",\n");
        }
        ViewStateProjectJsonCodec.writeView(out, project.viewState());
        out.append("\n}\n");
        return out.toString();
    }
}
