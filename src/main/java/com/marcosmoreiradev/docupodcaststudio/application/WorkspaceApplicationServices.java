package com.marcosmoreiradev.docupodcaststudio.application;

import com.marcosmoreiradev.docupodcaststudio.application.services.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

import java.util.Objects;

/**
 * Narrow dependency bundles exposed to desktop workspaces. Each bundle belongs
 * to a concrete area of the shell; no global service catalog is exposed.
 */
public record WorkspaceApplicationServices(
        ProjectWorkspace project,
        PlaybackWorkspace playback,
        GenerationWorkspace generation,
        ExportWorkspace exports,
        AdministrationWorkspace administration) {

    public WorkspaceApplicationServices {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(playback, "playback");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(exports, "exports");
        Objects.requireNonNull(administration, "administration");
    }

    public record ProjectWorkspace(
            ProjectApplicationServices project,
            AssetApplicationServices assets,
            DocumentApplicationServices document,
            ReadingProfileApplicationServices readingProfile,
            ScriptApplicationServices script,
            GrammarApplicationServices grammar,
            FragmentApplicationServices fragment,
            DocumentStudyApplicationServices documentStudy,
            TheatreApplicationServices theatre) { }

    public record PlaybackWorkspace(
            AudioApplicationServices audio,
            RecordingApplicationServices recording,
            PlaybackApplicationServices playback) { }

    public record GenerationWorkspace(
            RenderApplicationServices render,
            StoryboardApplicationServices storyboard,
            MediaApplicationServices media,
            VisualProductionApplicationServices visual) { }

    public record ExportWorkspace(
            ExportApplicationServices export,
            ProcessApplicationServices process) { }

    public record AdministrationWorkspace(
            VoiceApplicationServices voice,
            SettingsApplicationServices settings,
            ExampleApplicationServices examples,
            GuideApplicationServices guide,
            AiResourceApplicationServices resources,
            MediaEnginePlatform mediaEngines) {
        public AdministrationWorkspace {
            mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
        }
    }
}
