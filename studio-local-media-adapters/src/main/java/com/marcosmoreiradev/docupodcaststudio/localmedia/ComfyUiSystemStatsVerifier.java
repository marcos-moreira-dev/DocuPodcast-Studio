package com.marcosmoreiradev.docupodcaststudio.localmedia;

/** Verifies the device reported by a running ComfyUI without duplicating host policy in the adapter. */
@FunctionalInterface
public interface ComfyUiSystemStatsVerifier {
    ComfyUiSystemStatsVerifier NONE = json -> "";

    /**
     * @return an empty string when the binding is valid, otherwise a human-readable rejection reason.
     */
    String verify(String systemStatsJson);
}
