package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioWorkflowCoordinatorPortionTest {
    @Test
    void limitsThePortionFromTheSelectedFragment() {
        NarrationScriptDocument script = script(200);

        NarrationScriptDocument portion = AudioWorkflowCoordinator.scriptStartingAt(
                script, "SEG-051", 30);

        assertEquals(30, portion.segmentCount());
        assertEquals("SEG-051", portion.segments().getFirst().id());
        assertEquals("SEG-080", portion.segments().getLast().id());
    }

    @Test
    void keepsEverythingFromSelectionForTheLastScaleLevel() {
        NarrationScriptDocument script = script(200);

        NarrationScriptDocument portion = AudioWorkflowCoordinator.scriptStartingAt(
                script, "SEG-151", 0);

        assertEquals(50, portion.segmentCount());
        assertEquals("SEG-151", portion.segments().getFirst().id());
        assertEquals("SEG-200", portion.segments().getLast().id());
    }

    @Test
    void isolatesExactlyOneFragmentForSingleFragmentPlayback() {
        NarrationScriptDocument script = script(200);

        NarrationScriptDocument portion = AudioWorkflowCoordinator.scriptStartingAt(
                script, "SEG-051", 1);

        assertEquals(1, portion.segmentCount());
        assertEquals("SEG-051", portion.segments().getFirst().id());
    }

    private static NarrationScriptDocument script(int count) {
        List<NarrationSegment> segments = IntStream.rangeClosed(1, count)
                .mapToObj(index -> new NarrationSegment(
                        "SEG-" + String.format("%03d", index),
                        NarrationSegmentType.PARAGRAPH,
                        "Fragmento " + index,
                        "Texto narrable " + index + ".",
                        List.of("B" + index),
                        "CHR-NARRATOR",
                        "VOC-NARRATOR",
                        "STY-NEUTRAL",
                        Map.of()))
                .toList();
        return new NarrationScriptDocument(
                "SCRIPT-PORTION",
                "Prueba",
                "es",
                "documento.pdf",
                segments,
                Instant.EPOCH,
                Instant.EPOCH,
                "");
    }
}
