package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioSpinnerStyleTest {
    @Test
    void sharedSpinnerSkinOwnsPurpleSteppersAndWhiteArrows() throws Exception {
        String css;
        try (var input = getClass().getResourceAsStream(
                "/css/components/form-controls.css")) {
            css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(css.contains(".ui-form-spinner .increment-arrow-button"));
        assertTrue(css.contains(".ui-form-spinner .decrement-arrow-button"));
        assertTrue(css.contains("-fx-background-color: -docu-accent;"));
        assertTrue(css.contains(".ui-form-spinner .increment-arrow,"));
        assertTrue(css.contains("-fx-background-color: -docu-text-on-dark;"));
        assertTrue(css.contains(".ui-form-spinner .increment-arrow-button:hover"));
        assertTrue(css.contains(".ui-form-spinner .increment-arrow-button:pressed"));
        assertTrue(css.contains(".ui-form-spinner:disabled .increment-arrow-button"));
    }

    @Test
    void projectLoadingIndicatorRemovesTheNumericSpinnerChrome() throws Exception {
        String css;
        try (var input = getClass().getResourceAsStream(
                "/css/components/form-controls.css")) {
            css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(css.contains(".project-loading-indicator:indeterminate > .spinner"));
        assertTrue(css.contains(".project-loading-label"));
        assertTrue(css.contains("-fx-font-size: 18px"));
        assertTrue(css.contains("-fx-font-weight: 700"));
    }

    @Test
    void sharedInlineProgressIndicatorHasNoContainerChrome() throws Exception {
        String css;
        try (var input = getClass().getResourceAsStream(
                "/css/components/form-controls.css")) {
            css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(css.contains(".ui-progress-indicator:indeterminate > .spinner"));
        assertTrue(css.contains("-fx-border-width: 0"));
        assertTrue(css.contains("-fx-effect: null"));
    }
}
