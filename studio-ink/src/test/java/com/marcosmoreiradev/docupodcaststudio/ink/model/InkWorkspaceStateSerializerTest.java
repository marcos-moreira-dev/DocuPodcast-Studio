package com.marcosmoreiradev.docupodcaststudio.ink.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkWorkspaceStateSerializerTest {
    @Test
    void writesGenericV3SidecarWithStrokesImagesAndMetadata() {
        InkWorkspaceState state = InkWorkspaceState.create(
                1200,
                900,
                "#ffffffff",
                List.of(InkStroke.draw("#ff0000ff", 4,
                        List.of(new InkPoint(10, 20, 100, 0.7), new InkPoint(22, 28, 120, 0.8)))),
                List.of(new InkPlacedImage(
                        "IMG-1",
                        "ASSET-1",
                        "study/source.png",
                        "inline-current",
                        "inline-original",
                        40,
                        80,
                        320,
                        180,
                        40,
                        80,
                        400,
                        new InkImageCrop(true, 5, 6, 100, 80))),
                Map.of("consumer", "test"));

        String json = InkWorkspaceStateSerializer.toJson(state);

        assertTrue(json.contains("\"version\":3"));
        assertTrue(json.contains("\"format\":\"ink-workspace-state\""));
        assertTrue(json.contains("\"inkStrokes\""));
        assertTrue(json.contains("\"pressure\":0.700"));
        assertTrue(json.contains("\"images\""));
        assertTrue(json.contains("\"cropActive\":true"));
        assertTrue(json.contains("\"consumer\":\"test\""));
    }

    @Test
    void contentBoundsIncludeStrokesAndPlacedImages() {
        InkWorkspaceState state = InkWorkspaceState.create(
                200,
                200,
                "#ffffffff",
                List.of(InkStroke.draw("#000000ff", 3,
                        List.of(InkPoint.of(12, 15, 1), InkPoint.of(80, 90, 2)))),
                List.of(new InkPlacedImage("", "", "", "", "", 100, 120, 250, 90, 100, 120, 250,
                        InkImageCrop.none())),
                Map.of());

        InkWorkspaceBounds bounds = state.contentBounds().expandedBy(100);

        assertTrue(bounds.maxX() - bounds.minX() >= 350);
        assertTrue(bounds.maxY() - bounds.minY() >= 210);
    }

    @Test
    void readsGenericV3SidecarWithStrokesAndTitleMetadata() throws Exception {
        InkWorkspaceState state = InkWorkspaceState.create(
                1280,
                720,
                "#ffffffff",
                List.of(InkStroke.draw("#000000ff", 6,
                        List.of(InkPoint.of(12, 16, 1, 0.5), InkPoint.of(30, 40, 2, 1.0)))),
                List.of(new InkPlacedImage("IMG-RESTORE", "ASSET-SOURCE", "media/source.png",
                        "current-data", "original-data", 20, 30, 400, 210,
                        18, 28, 440, new InkImageCrop(true, 1, 2, 800, 420))),
                Map.of(
                        "showFragmentTitle", "true",
                        "fragmentTitleText", "Linea uno\nLinea dos"));

        InkWorkspaceState parsed = InkWorkspaceStateSerializer.fromJson(InkWorkspaceStateSerializer.toJson(state));

        assertEquals(1280, parsed.logicalWidth());
        assertEquals(720, parsed.logicalHeight());
        assertEquals("true", parsed.metadata().get("showFragmentTitle"));
        assertEquals("Linea uno\nLinea dos", parsed.metadata().get("fragmentTitleText"));
        assertEquals(1, parsed.strokes().size());
        assertEquals(2, parsed.strokes().get(0).points().size());
        assertEquals(0.5, parsed.strokes().get(0).points().get(0).pressure());
        assertEquals(1, parsed.images().size());
        assertEquals("IMG-RESTORE", parsed.images().get(0).id());
        assertEquals("ASSET-SOURCE", parsed.images().get(0).sourceAssetId());
        assertEquals("current-data", parsed.images().get(0).inlineImageData());
        assertEquals(400, parsed.images().get(0).fitWidth());
        assertTrue(parsed.images().get(0).crop().active());
    }

    @Test
    void normalizesLegacyCanvasCommandsWithoutASecondConsumerParser() throws Exception {
        String legacy = """
                {
                  "width": 640,
                  "height": 480,
                  "strokes": [
                    {"type":"DRAW","x1":10,"y1":20,"x2":50,"y2":60,
                     "controlX":30,"controlY":45,"quadratic":true,
                     "color":"#112233ff","width":4}
                  ]
                }
                """;

        InkWorkspaceState parsed = InkWorkspaceStateSerializer.fromJson(legacy);

        assertEquals(1, parsed.strokes().size());
        assertEquals(3, parsed.strokes().getFirst().points().size());
        assertEquals(30, parsed.strokes().getFirst().points().get(1).x());
        assertEquals("#112233ff", parsed.strokes().getFirst().color());
    }
}
