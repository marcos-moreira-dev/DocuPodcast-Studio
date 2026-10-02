package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentListeningPreferencesTest {
    @Test
    void defaultsDisableAutomaticReviewAndContinueAfterAnIndividualFailure() {
        DocumentListeningPreferences preferences =
                DocumentListeningPreferences.defaults();

        assertFalse(preferences.reviewTechnicalElements());
        assertEquals(
                DocumentListeningPreferences.PendingTechnicalElementPolicy
                        .AUTOMATIC_WHEN_ENABLED,
                preferences.pendingPolicy());
        assertEquals(DocumentListeningPreferences.QUALITY_MODEL,
                preferences.selectedLocalModel());
        assertEquals(
                DocumentListeningPreferences.TechnicalElementFailurePolicy
                        .CONTINUE_EXCLUDING,
                preferences.failurePolicy());
        assertEquals(SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF,
                preferences.secondarySemanticPolicy());
    }

    @Test
    void explicitlyEnabledReviewSurvivesProjectRoundTrip() {
        DocumentListeningPreferences expected =
                DocumentListeningPreferences.defaults()
                        .withReviewTechnicalElements(true);

        DocumentListeningPreferences restored =
                DocumentListeningPreferences.fromViewState(
                        expected.applyTo(Map.of()));

        assertTrue(restored.reviewTechnicalElements());
        assertEquals(expected.selectedLocalModel(), restored.selectedLocalModel());
        assertEquals(expected.failurePolicy(), restored.failurePolicy());
    }

    @Test
    void obsoletePromptPolicyFallsBackToAutomaticBehavior() {
        DocumentListeningPreferences restored =
                DocumentListeningPreferences.fromViewState(Map.of(
                        DocumentListeningPreferences.PENDING_POLICY_KEY,
                        "ASK_WHEN_PENDING"));

        assertEquals(
                DocumentListeningPreferences.PendingTechnicalElementPolicy
                        .AUTOMATIC_WHEN_ENABLED,
                restored.pendingPolicy());
    }

    @Test
    void roundTripsInsideProjectViewStateWithoutDiscardingOtherValues() {
        DocumentListeningPreferences expected =
                DocumentListeningPreferences.defaults()
                        .withReviewTechnicalElements(false)
                        .withSelectedLocalModel(
                                "qwen3-vl:4b-instruct-q4_K_M");

        Map<String, String> state = expected.applyTo(
                Map.of("document.zoom", "1.25"));
        DocumentListeningPreferences restored =
                DocumentListeningPreferences.fromViewState(state);

        assertFalse(restored.reviewTechnicalElements());
        assertEquals(expected.selectedLocalModel(),
                restored.selectedLocalModel());
        assertEquals("1.25", state.get("document.zoom"));
    }

    @Test
    void normalizesEveryPersistedModelToTheOnlySupportedQ8Model() {
        DocumentListeningPreferences restored =
                DocumentListeningPreferences.fromViewState(Map.of(
                        DocumentListeningPreferences.MODEL_KEY,
                        "qwen3-vl:4b-instruct-q4_K_M"));

        assertEquals(DocumentListeningPreferences.QUALITY_MODEL,
                restored.selectedLocalModel());
    }

    @Test
    void migratesLegacyTableAndImageChoicesWhenNewPolicyIsAbsent() {
        assertEquals(SecondarySemanticReadingPolicy.OMIT_ALL,
                DocumentListeningPreferences.fromViewState(Map.of(),
                        ImageNarrationPolicy.IGNORE_IMAGES,
                        TableNarrationPolicy.SKIP).secondarySemanticPolicy());
        assertEquals(SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS,
                DocumentListeningPreferences.fromViewState(Map.of(),
                        ImageNarrationPolicy.IGNORE_IMAGES,
                        TableNarrationPolicy.SUMMARIZE).secondarySemanticPolicy());
        assertEquals(SecondarySemanticReadingPolicy.IMAGES_AND_EXTRAS,
                DocumentListeningPreferences.fromViewState(Map.of(),
                        ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT,
                        TableNarrationPolicy.SKIP).secondarySemanticPolicy());
        assertEquals(SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF,
                DocumentListeningPreferences.fromViewState(Map.of(),
                        ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT,
                        TableNarrationPolicy.SUMMARIZE).secondarySemanticPolicy());
    }
}
