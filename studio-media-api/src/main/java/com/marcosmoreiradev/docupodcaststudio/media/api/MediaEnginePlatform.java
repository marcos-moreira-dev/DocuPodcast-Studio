package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** Complete engine platform passed explicitly by the composition root. */
public record MediaEnginePlatform(
        EngineRegistry<VoiceSynthesisEngine> voiceEngines,
        EngineRegistry<ImageGenerationEngine> imageEngines,
        EngineRegistry<ImageSuperResolutionEngine> imageSuperResolutionEngines,
        EngineRegistry<ImageRefinementEngine> imageRefinementEngines,
        EngineRegistry<VideoGenerationEngine> videoGenerationEngines,
        EngineRegistry<VideoRenderEngine> videoRenderEngines,
        ContentAnalysisEngineRegistry contentAnalysisEngines,
        EngineAdministrationRegistry administration) implements AutoCloseable {
    public MediaEnginePlatform {
        voiceEngines = Objects.requireNonNullElseGet(voiceEngines,
                () -> new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS));
        imageEngines = Objects.requireNonNullElseGet(imageEngines,
                () -> new EngineRegistry<>(CapabilityId.IMAGE_GENERATION));
        imageSuperResolutionEngines = Objects.requireNonNullElseGet(imageSuperResolutionEngines,
                () -> new EngineRegistry<>(CapabilityId.IMAGE_SUPER_RESOLUTION));
        imageRefinementEngines = Objects.requireNonNullElseGet(imageRefinementEngines,
                () -> new EngineRegistry<>(CapabilityId.IMAGE_REFINEMENT));
        videoGenerationEngines = Objects.requireNonNullElseGet(videoGenerationEngines,
                () -> new EngineRegistry<>(CapabilityId.VIDEO_GENERATION));
        videoRenderEngines = Objects.requireNonNullElseGet(videoRenderEngines,
                () -> new EngineRegistry<>(CapabilityId.VIDEO_RENDERING));
        contentAnalysisEngines = Objects.requireNonNullElseGet(
                contentAnalysisEngines, ContentAnalysisEngineRegistry::new);
        administration = Objects.requireNonNullElseGet(administration, EngineAdministrationRegistry::new);
    }

    public MediaEnginePlatform(EngineRegistry<VoiceSynthesisEngine> voiceEngines,
                               EngineRegistry<ImageGenerationEngine> imageEngines,
                               EngineRegistry<ImageSuperResolutionEngine> imageSuperResolutionEngines,
                               EngineRegistry<ImageRefinementEngine> imageRefinementEngines,
                               EngineRegistry<VideoGenerationEngine> videoGenerationEngines,
                               EngineRegistry<VideoRenderEngine> videoRenderEngines,
                               EngineAdministrationRegistry administration) {
        this(voiceEngines, imageEngines, imageSuperResolutionEngines, imageRefinementEngines,
                videoGenerationEngines, videoRenderEngines, null, administration);
    }

    public MediaEnginePlatform(EngineRegistry<VoiceSynthesisEngine> voiceEngines,
                               EngineRegistry<ImageGenerationEngine> imageEngines,
                               EngineRegistry<ImageSuperResolutionEngine> imageSuperResolutionEngines,
                               EngineRegistry<VideoGenerationEngine> videoGenerationEngines,
                               EngineRegistry<VideoRenderEngine> videoRenderEngines,
                               EngineAdministrationRegistry administration) {
        this(voiceEngines, imageEngines, imageSuperResolutionEngines, null,
                videoGenerationEngines, videoRenderEngines, null, administration);
    }

    public MediaEnginePlatform(EngineRegistry<VoiceSynthesisEngine> voiceEngines,
                               EngineRegistry<ImageGenerationEngine> imageEngines,
                               EngineRegistry<VideoRenderEngine> videoRenderEngines) {
        this(voiceEngines, imageEngines, null, null, null, videoRenderEngines, null, null);
    }

    public MediaEnginePlatform(EngineRegistry<VoiceSynthesisEngine> voiceEngines,
                               EngineRegistry<ImageGenerationEngine> imageEngines,
                               EngineRegistry<VideoGenerationEngine> videoGenerationEngines,
                               EngineRegistry<VideoRenderEngine> videoRenderEngines,
                               EngineAdministrationRegistry administration) {
        this(voiceEngines, imageEngines, null, null,
                videoGenerationEngines, videoRenderEngines, null, administration);
    }

    public static MediaEnginePlatform empty() {
        return new MediaEnginePlatform(null, null, null, null, null, null, null, null);
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        java.util.Set<AutoCloseable> owned = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        collect(owned, voiceEngines.engines());
        collect(owned, imageEngines.engines());
        collect(owned, imageSuperResolutionEngines.engines());
        collect(owned, imageRefinementEngines.engines());
        collect(owned, videoGenerationEngines.engines());
        collect(owned, videoRenderEngines.engines());
        collect(owned, contentAnalysisEngines.engines());
        collect(owned, administration.entries());
        for (AutoCloseable closeable : owned) {
            try {
                closeable.close();
            } catch (Exception ex) {
                if (failure == null) failure = new IllegalStateException(
                        "Could not close owned media component "
                                + closeable.getClass().getSimpleName(), ex);
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    private static void collect(java.util.Set<AutoCloseable> destination,
                                java.util.List<?> components) {
        for (Object component : components) {
            if (component instanceof AutoCloseable closeable) destination.add(closeable);
        }
    }
}
