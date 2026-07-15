package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreAudioTrackTimelineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.RemoveTheatreAudioTrackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.RenderTheatreChoralVoiceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFragmentLinkPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.UpsertTheatreAudioTrackUseCase;

/** Theatre production projections, readiness and fragment-link policies. */
public record TheatreApplicationServices(
        BuildTheatreProductionProjectionUseCase buildTheatreProductionProjection,
        TheatreFragmentLinkPolicy fragmentLinkPolicy,
        BuildTheatreAudioTrackTimelineUseCase buildAudioTrackTimeline,
        UpsertTheatreAudioTrackUseCase upsertAudioTrack,
        RemoveTheatreAudioTrackUseCase removeAudioTrack,
        RenderTheatreChoralVoiceUseCase renderChoralVoice
) {
    public TheatreApplicationServices(BuildTheatreProductionProjectionUseCase projection,
                                      TheatreFragmentLinkPolicy fragmentLinkPolicy) {
        this(projection, fragmentLinkPolicy, defaults());
    }

    private TheatreApplicationServices(BuildTheatreProductionProjectionUseCase projection,
                                       TheatreFragmentLinkPolicy fragmentLinkPolicy,
                                       BuildTheatreAudioTrackTimelineUseCase timeline) {
        this(projection, fragmentLinkPolicy, timeline,
                new UpsertTheatreAudioTrackUseCase(timeline), new RemoveTheatreAudioTrackUseCase(), null);
    }

    private static BuildTheatreAudioTrackTimelineUseCase defaults() {
        return new BuildTheatreAudioTrackTimelineUseCase();
    }
}
