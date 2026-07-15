package com.marcosmoreiradev.docupodcaststudio.application;

import com.marcosmoreiradev.docupodcaststudio.application.services.AssetApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.AudioApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.DocumentApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.DocumentStudyApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ExportApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ExampleApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.FragmentApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.GrammarApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.GuideApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.AiResourceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.PlaybackApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ProcessApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ProjectApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ReadingProfileApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.RecordingApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.RenderApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ScriptApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.MediaApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.TheatreApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.VoiceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.VisualProductionApplicationServices;

/** Root application-service facade grouped by families. */
public record ApplicationServices(
        ProjectApplicationServices project,
        AssetApplicationServices assets,
        DocumentApplicationServices document,
        ReadingProfileApplicationServices readingProfile,
        ScriptApplicationServices script,
        AudioApplicationServices audio,
        RecordingApplicationServices recording,
        PlaybackApplicationServices playback,
        RenderApplicationServices render,
        VoiceApplicationServices voice,
        StoryboardApplicationServices storyboard,
        ExportApplicationServices export,
        ExampleApplicationServices examples,
        GuideApplicationServices guide,
        AiResourceApplicationServices resources,
        MediaApplicationServices media,
        ProcessApplicationServices process,
        SettingsApplicationServices settings,
        GrammarApplicationServices grammar,
        FragmentApplicationServices fragment,
        DocumentStudyApplicationServices documentStudy,
        TheatreApplicationServices theatre,
        VisualProductionApplicationServices visual
) {
    public ApplicationServices {
        visual = visual == null ? VisualProductionApplicationServices.defaults() : visual;
    }

    public ApplicationServices(
            ProjectApplicationServices project,
            AssetApplicationServices assets,
            DocumentApplicationServices document,
            ReadingProfileApplicationServices readingProfile,
            ScriptApplicationServices script,
            AudioApplicationServices audio,
            RecordingApplicationServices recording,
            PlaybackApplicationServices playback,
            RenderApplicationServices render,
            VoiceApplicationServices voice,
            StoryboardApplicationServices storyboard,
            ExportApplicationServices export,
            ExampleApplicationServices examples,
            GuideApplicationServices guide,
            AiResourceApplicationServices resources,
            MediaApplicationServices media,
            ProcessApplicationServices process,
            SettingsApplicationServices settings,
            GrammarApplicationServices grammar,
            FragmentApplicationServices fragment,
            DocumentStudyApplicationServices documentStudy,
            TheatreApplicationServices theatre) {
        this(project, assets, document, readingProfile, script, audio, recording, playback, render, voice, storyboard,
                export, examples, guide, resources, media, process, settings, grammar, fragment, documentStudy, theatre,
                VisualProductionApplicationServices.defaults());
    }
}
