package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.List;

/** Resolved placement of one theatre background track on the narration timeline. */
public record TheatreAudioTrackTimelineEntry(
        TheatreProjectLayer.TheatreAudioTrack track,
        String assetRelativePath,
        String assetDisplayName,
        String startSegmentId,
        double timelineStartSeconds,
        double timelineEndSeconds,
        List<String> affectedSegmentIds,
        List<String> affectedInterventionIds,
        boolean valid,
        String message
) {
    public TheatreAudioTrackTimelineEntry(TheatreProjectLayer.TheatreAudioTrack track,
                                          String assetRelativePath,
                                          String assetDisplayName,
                                          double timelineStartSeconds,
                                          double timelineEndSeconds,
                                          List<String> affectedSegmentIds,
                                          List<String> affectedInterventionIds,
                                          boolean valid,
                                          String message) {
        this(track, assetRelativePath, assetDisplayName,
                track == null ? "" : track.startSegmentId(), timelineStartSeconds, timelineEndSeconds,
                affectedSegmentIds, affectedInterventionIds, valid, message);
    }

    public TheatreAudioTrackTimelineEntry {
        assetRelativePath = assetRelativePath == null ? "" : assetRelativePath.strip();
        assetDisplayName = assetDisplayName == null ? "" : assetDisplayName.strip();
        startSegmentId = startSegmentId == null ? "" : startSegmentId.strip();
        timelineStartSeconds = Math.max(0.0, timelineStartSeconds);
        timelineEndSeconds = Math.max(timelineStartSeconds, timelineEndSeconds);
        affectedSegmentIds = affectedSegmentIds == null ? List.of() : List.copyOf(affectedSegmentIds);
        affectedInterventionIds = affectedInterventionIds == null ? List.of() : List.copyOf(affectedInterventionIds);
        message = message == null ? "" : message.strip();
    }

    public boolean contains(double positionSeconds) {
        return valid && positionSeconds >= timelineStartSeconds && positionSeconds < timelineEndSeconds;
    }

    public boolean overlaps(TheatreAudioTrackTimelineEntry other) {
        return other != null && valid && other.valid
                && timelineStartSeconds < other.timelineEndSeconds
                && other.timelineStartSeconds < timelineEndSeconds;
    }
}
