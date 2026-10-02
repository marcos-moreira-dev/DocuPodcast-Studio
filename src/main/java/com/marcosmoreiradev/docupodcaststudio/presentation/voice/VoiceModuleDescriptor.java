package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

/** Display metadata for the sober module navigation in Voices. */
public record VoiceModuleDescriptor(
        VoiceModuleId id,
        String group,
        String title,
        String description
) implements com.marcosmoreiradev.docupodcaststudio.presentation.components.admin.AdminModuleSpec<VoiceModuleId> {
    public VoiceModuleDescriptor {
        id = id == null ? VoiceModuleId.HOME : id;
        group = clean(group, "OPERACIÓN");
        title = clean(title, id.title());
        description = clean(description, id.description());
    }

    public static VoiceModuleDescriptor of(VoiceModuleId id, String group) {
        return new VoiceModuleDescriptor(id, group, id.title(), id.description());
    }

    private static String clean(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
