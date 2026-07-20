package com.marcosmoreiradev.docupodcaststudio.application;

import com.marcosmoreiradev.docupodcaststudio.application.services.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

import java.util.Objects;

/**
 * Narrow dependency bundles exposed to desktop workspaces. This is the
 * migration boundary away from using {@link ApplicationServices} as a global
 * locator throughout presentation code.
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

    public static WorkspaceApplicationServices from(ApplicationServices services) {
        return from(services, MediaEnginePlatform.empty());
    }

    public static WorkspaceApplicationServices from(ApplicationServices services, MediaEnginePlatform mediaEngines) {
        Objects.requireNonNull(services, "services");
        return new WorkspaceApplicationServices(
                new ProjectWorkspace(services.project(), services.assets(), services.document(),
                        services.readingProfile(), services.script(), services.grammar(), services.fragment(),
                        services.documentStudy(), services.theatre()),
                new PlaybackWorkspace(services.audio(), services.recording(), services.playback()),
                new GenerationWorkspace(services.render(), services.storyboard(), services.media(), services.visual()),
                new ExportWorkspace(services.export(), services.process()),
                new AdministrationWorkspace(services.voice(), services.settings(), services.examples(),
                        services.guide(), services.resources(), mediaEngines));
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
