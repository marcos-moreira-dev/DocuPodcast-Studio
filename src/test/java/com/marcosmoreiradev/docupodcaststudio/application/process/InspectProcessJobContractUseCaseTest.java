package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectProcessJobContractUseCaseTest {
    @Test
    void reportsAudioAsUnifiedAndOtherLongRunningProcessesAsPending() {
        ProcessJobContractReport report = new InspectProcessJobContractUseCase().inspect();

        assertTrue(report.trackedKinds().contains(ProcessJobKind.TTS_AUDIO));
        assertTrue(report.trackedKinds().contains(ProcessJobKind.MEDIA_PREPARATION));
        assertTrue(report.trackedKinds().contains(ProcessJobKind.ENGINE_SETUP));
        assertTrue(report.trackedKinds().contains(ProcessJobKind.VIDEO_RENDER));
        assertTrue(report.audioGenerationIsUnified());
        assertFalse(report.fullyUnified());
    }
}
