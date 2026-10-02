package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

/** Provider aliases used only while reading and writing the format-v1 settings surface. */
public final class LegacyEngineAliases {
    private LegacyEngineAliases() { }

    public static String imageEngine(String legacyMode) {
        return "managed-local".equalsIgnoreCase(legacyMode) ? "comfyui" : clean(legacyMode);
    }

    public static String videoGenerationEngine() { return "comfyui-video"; }

    public static String videoRenderEngine() { return "ffmpeg"; }

    private static String clean(String value) { return value == null ? "" : value.strip(); }
}
