package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrepareListeningSessionUseCaseTest {
    private final PrepareListeningSessionUseCase useCase = new PrepareListeningSessionUseCase();

    @Test
    void noDocumentReturnsExecutablePlanAndUserFacingState() {
        ListeningSessionReadiness readiness = useCase.prepare(new PrepareListeningSessionRequest(
                null,
                null,
                PlaybackManifest.empty(),
                false,
                false,
                AudioJobStatusDto.idle(),
                PlaybackBufferPolicy.defaultPolicy()));

        assertEquals(DocumentListenPhase.NO_DOCUMENT, readiness.plan().phase());
        assertEquals("1. Abre un Word/DOCX", readiness.state().title());
        assertTrue(readiness.state().styleClass().contains("document-listen-flow-waiting"));
    }

    @Test
    void stateIsDerivedFromPlanWithoutPresentationBooleans() {
        assertEquals("3. Guarda el proyecto para generar audio",
                ListeningSessionState.from(DocumentListenPlan.saveProjectRequired()).title());
        assertEquals("Preparando audio por fragmentos",
                ListeningSessionState.from(DocumentListenPlan.waitForAudioBuffer("buffer")).title());
        assertEquals("Listo para reproducir",
                ListeningSessionState.from(DocumentListenPlan.playExistingAudio()).title());
    }
}
