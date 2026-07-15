package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectTheatreAudioTracksJsonTest {
    @Test
    void roundTripsTheatreBackgroundTracksAndOldProjectsDefaultToEmpty() throws Exception {
        var track = new TheatreProjectLayer.TheatreAudioTrack("TRACK-001", "AUDIO-001", "INTERVENCION-1", "SEG-003",
                1.25, 9.5, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.30, 12.0, true);
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withTheatre(TheatreProjectLayer.empty().withAudioTracks(List.of(track)));

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"audioTracks\""));
        assertTrue(json.contains("\"startSegmentId\": \"SEG-003\""));
        assertTrue(json.contains("\"gentleFade\": true"));
        assertEquals(track, opened.theatre().audioTracks().getFirst());

        String withoutField = json.replaceFirst(",\\s*\"audioTracks\"\\s*:\\s*\\[[^]]*]", "");
        assertTrue(new DocuPodcastProjectJsonReader().read(withoutField).theatre().audioTracks().isEmpty());
    }
}
