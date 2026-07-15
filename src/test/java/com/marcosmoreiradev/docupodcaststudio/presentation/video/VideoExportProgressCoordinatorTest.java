package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoExportProgressCoordinatorTest {
    @Test
    void translatesLinkageErrorsIntoRestartInstructions() {
        Throwable result = VideoExportProgressCoordinator.actionableFailure(
                new RuntimeException("wrapper", new NoClassDefFoundError("old/Class")));

        assertTrue(result.getMessage().contains("inicia una instancia nueva"));
        assertTrue(result.getMessage().contains("no se han modificado"));
    }

    @Test
    void preservesNormalExportFailures() {
        IOException failure = new IOException("ffmpeg failed");

        assertSame(failure, VideoExportProgressCoordinator.actionableFailure(failure));
    }
}
