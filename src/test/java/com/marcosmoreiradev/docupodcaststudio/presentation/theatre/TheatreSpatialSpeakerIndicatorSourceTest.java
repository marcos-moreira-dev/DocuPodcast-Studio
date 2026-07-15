package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreSpatialSpeakerIndicatorSourceTest {
    @Test
    void sidebarFullscreenAndVideoMarkSpeakerNamesWithoutChangingGrammarData() throws Exception {
        String sidebar = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSpatialActionMapPanel.java"));
        String fullscreen = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFullscreenMapView.java"));
        String video = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreSpatialVideoPlanUseCase.java"));

        assertSpeakerIndicator(sidebar);
        assertSpeakerIndicator(fullscreen);
        assertSpeakerIndicator(video);

        assertTrue(sidebar.contains("new MarkerParticipant(speaker, locationFor(placement, speaker, placement.origin()),"));
        assertTrue(fullscreen.contains("TheatreSpatialRoleIcon.forSpeaker(speakerName), true"));
        assertTrue(video.contains("TheatreSpatialRoleIcon.forSpeaker(speakerName), true"));
    }

    @Test
    void selfLoopsAreConstrainedToMapBounds() throws Exception {
        String sidebar = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSpatialActionMapPanel.java"));
        String fullscreen = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFullscreenMapView.java"));
        String video = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreSpatialVideoPlanUseCase.java"));

        assertTrue(sidebar.contains("SPATIAL_SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44"));
        assertTrue(sidebar.contains("SPATIAL_SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10"));
        assertTrue(sidebar.contains("double x = clamp(origin.x() + SPATIAL_MARKER_SIZE * SPATIAL_SELF_LOOP_RIGHT_OFFSET_FACTOR"));
        assertTrue(sidebar.contains("markerPointForSelfLoop(stagePoint(location), group.selfLoop())"));
        assertTrue(sidebar.contains("SPATIAL_IMAGE_FIT_WIDTH - size * 1.14 - 4"));
        assertTrue(fullscreen.contains("SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44"));
        assertTrue(fullscreen.contains("SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10"));
        assertTrue(fullscreen.contains("drawActionArrow(gc, origin, stagePoint(destination, overlayBounds), overlayBounds)"));
        assertTrue(fullscreen.contains("mapBounds.x() + mapBounds.width() - size * 1.14 - 4"));
        assertTrue(fullscreen.contains("markerPointForSelfLoop(stagePoint(entry.getKey(), mapBounds), entry.getValue().selfLoop, mapBounds)"));
        assertTrue(video.contains("SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44"));
        assertTrue(video.contains("SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10"));
        assertTrue(video.contains("drawSelfLoop(g, ox, oy, iconSize, spec, mapX, mapY, mapW, mapH)"));
        assertTrue(video.contains("mapX + mapW - size * 1.14 - 4"));
        assertTrue(video.contains("markerXForSelfLoop(x, mapX, mapW, iconSize)"));
    }

    @Test
    void selfLoopArrowheadsUseFilledTangentHeadsInsteadOfLooseStrokes() throws Exception {
        String sidebar = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSpatialActionMapPanel.java"));
        String fullscreen = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFullscreenMapView.java"));
        String video = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreSpatialVideoPlanUseCase.java"));

        assertTrue(sidebar.contains("fillArrowHead(gc, ex, ey, tangentAngle, headLength)"));
        assertTrue(fullscreen.contains("fillArrowHead(gc, ex, ey, tangentAngle, headLength)"));
        assertTrue(video.contains("fillArrowHead(g, ex, ey, tangentAngle, headLength)"));
        assertTrue(video.contains("Path2D.Double loop"));
        assertTrue(video.contains("Path2D.Double head"));
    }

    private static void assertSpeakerIndicator(String source) {
        assertTrue(source.contains("boolean speaking"));
        assertTrue(source.contains("group.speakers"));
        assertTrue(source.contains("containsIgnoreCase(speakers"));
        assertTrue(source.contains("fillOval"));
    }
}
