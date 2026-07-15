package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** Option shown when a user assigns a shared voice profile to a target. */
public record VoiceAssignmentOption(
        String voiceId,
        String displayName,
        VoiceReferenceAvailability availability,
        boolean selectable,
        boolean current,
        boolean reservedByOtherTarget,
        String status,
        String detail
) {
    public VoiceAssignmentOption {
        voiceId = normalize(voiceId);
        displayName = normalize(displayName).isBlank() ? voiceId : normalize(displayName);
        availability = availability == null ? VoiceReferenceAvailability.MISSING_PROFILE : availability;
        selectable = selectable && availability.usable() && !reservedByOtherTarget;
        status = normalize(status).isBlank() ? availability.name() : normalize(status);
        detail = normalize(detail).isBlank() ? "Sin detalle de voz." : normalize(detail);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    @Override
    public String toString() {
        String suffix = current ? " (actual)" : "";
        String state = selectable ? "" : " - " + status;
        return displayName + suffix + state;
    }
}
