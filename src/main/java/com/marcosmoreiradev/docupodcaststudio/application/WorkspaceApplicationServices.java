package com.marcosmoreiradev.docupodcaststudio.application;

import com.marcosmoreiradev.docupodcaststudio.application.services.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityAdministrationService;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeRoots;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

import java.nio.file.Path;
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
        AdministrationWorkspace administration,
        RuntimeWorkspace runtime) {

    public WorkspaceApplicationServices {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(playback, "playback");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(exports, "exports");
        Objects.requireNonNull(administration, "administration");
        runtime = runtime == null
                ? RuntimeWorkspace.configured()
                : runtime;
    }

    public WorkspaceApplicationServices(
            ProjectWorkspace project,
            PlaybackWorkspace playback,
            GenerationWorkspace generation,
            ExportWorkspace exports,
            AdministrationWorkspace administration) {
        this(project, playback, generation, exports, administration, RuntimeWorkspace.configured());
    }

    public record RuntimeWorkspace(Path installationRoot, Path runtimeRoot) {
        public RuntimeWorkspace {
            installationRoot = Objects.requireNonNull(installationRoot, "installation root")
                    .toAbsolutePath().normalize();
            runtimeRoot = Objects.requireNonNull(runtimeRoot, "runtime root")
                    .toAbsolutePath().normalize();
        }

        public RuntimeWorkspace(ApplicationRuntimeRoots roots) {
            this(roots.installationRoot(), roots.runtimeRoot());
        }

        public static RuntimeWorkspace configured() {
            return new RuntimeWorkspace(ApplicationRuntimeRoots.configured());
        }
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
            TheatreApplicationServices theatre,
            TheatrePackageApplicationServices theatrePackage) {

        public ProjectWorkspace(
                ProjectApplicationServices project,
                AssetApplicationServices assets,
                DocumentApplicationServices document,
                ReadingProfileApplicationServices readingProfile,
                ScriptApplicationServices script,
                GrammarApplicationServices grammar,
                FragmentApplicationServices fragment,
                DocumentStudyApplicationServices documentStudy,
                TheatreApplicationServices theatre) {
            this(project, assets, document, readingProfile, script, grammar, fragment,
                    documentStudy, theatre, null);
        }
    }

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
            MediaEnginePlatform mediaEngines,
            CapabilityAdministrationService capabilities) {
        public AdministrationWorkspace {
            mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
            capabilities = capabilities == null
                    ? new CapabilityAdministrationService(mediaEngines) : capabilities;
        }

        public AdministrationWorkspace(
                VoiceApplicationServices voice,
                SettingsApplicationServices settings,
                ExampleApplicationServices examples,
                GuideApplicationServices guide,
                AiResourceApplicationServices resources,
                MediaEnginePlatform mediaEngines) {
            this(voice, settings, examples, guide, resources, mediaEngines, null);
        }
    }
}
