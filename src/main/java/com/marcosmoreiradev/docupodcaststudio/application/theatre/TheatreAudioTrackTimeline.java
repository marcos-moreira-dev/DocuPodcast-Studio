package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Ordered theatre background-track projection used by UI, playback and export. */
public record TheatreAudioTrackTimeline(List<TheatreAudioTrackTimelineEntry> entries) {
    public TheatreAudioTrackTimeline {
        entries = entries == null ? List.of() : entries.stream()
                .sorted(Comparator.comparingDouble(TheatreAudioTrackTimelineEntry::timelineStartSeconds))
                .toList();
    }

    public Optional<TheatreAudioTrackTimelineEntry> activeAt(double positionSeconds) {
        return entries.stream().filter(entry -> entry.contains(positionSeconds)).findFirst();
    }

    public Optional<TheatreAudioTrackTimelineEntry> startingAt(String interventionId) {
        String target = normalize(interventionId);
        return entries.stream().filter(entry -> entry.track().startIntervencionId().equals(target)).findFirst();
    }

    public Optional<TheatreAudioTrackTimelineEntry> startingAtSegment(String segmentId) {
        String target = normalize(segmentId);
        return entries.stream().filter(entry -> entry.startSegmentId().equals(target)).findFirst();
    }

    public Optional<TheatreAudioTrackTimelineEntry> affectingSegment(String segmentId) {
        String target = normalize(segmentId);
        return entries.stream().filter(entry -> entry.affectedSegmentIds().contains(target)).findFirst();
    }

    public Optional<TheatreAudioTrackTimelineEntry> affectingIntervention(String interventionId) {
        String target = normalize(interventionId);
        return entries.stream().filter(entry -> entry.affectedInterventionIds().contains(target)).findFirst();
    }

    public List<TheatreAudioTrackTimelineEntry> conflictsWith(TheatreAudioTrackTimelineEntry candidate) {
        if (candidate == null) {
            return List.of();
        }
        return entries.stream()
                .filter(existing -> !existing.track().id().equals(candidate.track().id()))
                .filter(existing -> existing.overlaps(candidate))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
