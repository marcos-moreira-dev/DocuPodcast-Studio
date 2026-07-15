package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreTextualMapManualRecordingSourceTest {
    @Test
    void textualMapContextMenuOpensManualRecordingWorkflow() throws Exception {
        String canvas = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreTextSequenceCanvas.java"));
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreTextualMapPanel.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ManualInterventionAudioWorkflow.java"));

        assertTrue(canvas.contains("Grabar audio narraci\\u00f3n/efecto sonido"));
        assertTrue(panel.contains("showManualRecordingDialog"));
        assertTrue(panel.contains("Dejar de grabar"));
        assertTrue(panel.contains("Asignar audio a intervenci\\u00f3n"));
        assertTrue(panel.contains("AudioInputDeviceSelector"));
        assertTrue(panel.contains("stopManualInterventionRecordingDraft"));
        assertTrue(workflow.contains("manualAudioSegmentJob()"));
    }
}
