package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Product-level choices for the document listening flow.
 *
 * <p>The values are stored in project view state so they travel with the
 * project without coupling the project schema to a particular local model
 * runtime.</p>
 */
public record DocumentListeningPreferences(
        boolean reviewTechnicalElements,
        PendingTechnicalElementPolicy pendingPolicy,
        String selectedLocalModel,
        TechnicalElementFailurePolicy failurePolicy,
        SecondarySemanticReadingPolicy secondarySemanticPolicy
) {
    public static final String REVIEW_KEY =
            "documentListening.reviewTechnicalElements";
    public static final String PENDING_POLICY_KEY =
            "documentListening.pendingPolicy";
    public static final String MODEL_KEY =
            "documentListening.selectedLocalModel";
    public static final String FAILURE_POLICY_KEY =
            "documentListening.failurePolicy";
    public static final String SECONDARY_SEMANTIC_POLICY_KEY =
            "documentListening.secondarySemanticPolicy";
    public static final String QUALITY_MODEL =
            "qwen3-vl:4b-instruct-q8_0";

    public DocumentListeningPreferences {
        pendingPolicy = Objects.requireNonNullElse(
                pendingPolicy,
                PendingTechnicalElementPolicy.AUTOMATIC_WHEN_ENABLED);
        selectedLocalModel = QUALITY_MODEL;
        failurePolicy = Objects.requireNonNullElse(
                failurePolicy,
                TechnicalElementFailurePolicy.CONTINUE_EXCLUDING);
        secondarySemanticPolicy = Objects.requireNonNullElse(
                secondarySemanticPolicy,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    public DocumentListeningPreferences(
            boolean reviewTechnicalElements,
            PendingTechnicalElementPolicy pendingPolicy,
            String selectedLocalModel,
            TechnicalElementFailurePolicy failurePolicy) {
        this(reviewTechnicalElements, pendingPolicy, selectedLocalModel,
                failurePolicy,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    public static DocumentListeningPreferences defaults() {
        return new DocumentListeningPreferences(
                false,
                PendingTechnicalElementPolicy.AUTOMATIC_WHEN_ENABLED,
                QUALITY_MODEL,
                TechnicalElementFailurePolicy.CONTINUE_EXCLUDING,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    public static DocumentListeningPreferences fromViewState(
            Map<String, String> viewState) {
        Map<String, String> state = viewState == null ? Map.of() : viewState;
        DocumentListeningPreferences defaults = defaults();
        return new DocumentListeningPreferences(
                Boolean.parseBoolean(state.getOrDefault(
                        REVIEW_KEY,
                        Boolean.toString(defaults.reviewTechnicalElements()))),
                enumValue(PendingTechnicalElementPolicy.class,
                        state.get(PENDING_POLICY_KEY),
                        defaults.pendingPolicy()),
                state.getOrDefault(MODEL_KEY, defaults.selectedLocalModel()),
                enumValue(TechnicalElementFailurePolicy.class,
                        state.get(FAILURE_POLICY_KEY),
                        defaults.failurePolicy()),
                enumValue(SecondarySemanticReadingPolicy.class,
                        state.get(SECONDARY_SEMANTIC_POLICY_KEY),
                        defaults.secondarySemanticPolicy()));
    }

    public static DocumentListeningPreferences fromViewState(
            Map<String, String> viewState,
            ImageNarrationPolicy imagePolicy,
            TableNarrationPolicy tablePolicy) {
        Map<String, String> state = viewState == null ? Map.of() : viewState;
        DocumentListeningPreferences restored = fromViewState(state);
        if (state.containsKey(SECONDARY_SEMANTIC_POLICY_KEY)) return restored;
        return restored.withSecondarySemanticPolicy(
                SecondarySemanticReadingPolicy.fromLegacy(
                        imagePolicy, tablePolicy));
    }

    public Map<String, String> applyTo(Map<String, String> viewState) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(
                viewState == null ? Map.of() : viewState);
        updated.put(REVIEW_KEY, Boolean.toString(reviewTechnicalElements));
        updated.put(PENDING_POLICY_KEY, pendingPolicy.name());
        updated.put(MODEL_KEY, selectedLocalModel);
        updated.put(FAILURE_POLICY_KEY, failurePolicy.name());
        updated.put(SECONDARY_SEMANTIC_POLICY_KEY,
                secondarySemanticPolicy.name());
        return Map.copyOf(updated);
    }

    public DocumentListeningPreferences withReviewTechnicalElements(
            boolean enabled) {
        return new DocumentListeningPreferences(
                enabled, pendingPolicy, selectedLocalModel, failurePolicy,
                secondarySemanticPolicy);
    }

    public DocumentListeningPreferences withSelectedLocalModel(String model) {
        return new DocumentListeningPreferences(
                reviewTechnicalElements, pendingPolicy, QUALITY_MODEL,
                failurePolicy, secondarySemanticPolicy);
    }

    public DocumentListeningPreferences withSecondarySemanticPolicy(
            SecondarySemanticReadingPolicy policy) {
        return new DocumentListeningPreferences(
                reviewTechnicalElements, pendingPolicy, QUALITY_MODEL,
                failurePolicy, policy);
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, value.strip());
        } catch (IllegalArgumentException invalid) {
            return fallback;
        }
    }

    public enum PendingTechnicalElementPolicy {
        /**
         * The product-level checkbox is the complete decision: enabled means
         * analyze automatically; disabled means exclude pending technical
         * elements from this listening flow.
         */
        AUTOMATIC_WHEN_ENABLED
    }

    public enum TechnicalElementFailurePolicy {
        CONTINUE_EXCLUDING
    }
}
