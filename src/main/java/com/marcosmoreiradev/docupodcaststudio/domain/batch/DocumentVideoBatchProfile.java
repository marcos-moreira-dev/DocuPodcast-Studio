package com.marcosmoreiradev.docupodcaststudio.domain.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;

/** Shared configuration inherited by every documentary child project in the batch. */
public record DocumentVideoBatchProfile(
        DocumentTextVideoOptions video,
        double imageSlideSeconds,
        boolean interpretImages,
        BatchBranding branding,
        BatchOutputKind outputKind,
        com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat audioFormat,
        String voiceEngineId,
        String aiEngineId,
        java.util.Map<String, DocumentBackgroundOverride> documentBackgrounds
) {
    public DocumentVideoBatchProfile {
        documentBackgrounds = documentBackgrounds == null ? java.util.Map.of() : java.util.Map.copyOf(documentBackgrounds);
        video = video == null ? DocumentTextVideoOptions.defaults() : video;
        imageSlideSeconds = Math.max(1.0, Math.min(60.0, imageSlideSeconds));
        branding = branding == null ? BatchBranding.none() : branding;
        outputKind = outputKind == null ? BatchOutputKind.VIDEO : outputKind;
        audioFormat = audioFormat == null ? com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat.MP3 : audioFormat;
        voiceEngineId = cleanEngineId(voiceEngineId);
        aiEngineId = cleanEngineId(aiEngineId);
        if (outputKind == BatchOutputKind.AUDIO) branding = BatchBranding.none();
    }

    public DocumentVideoBatchProfile(DocumentTextVideoOptions video, double seconds, boolean interpret, BatchBranding branding,
            BatchOutputKind output, com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat format,
            String voice, String ai) {
        this(video, seconds, interpret, branding, output, format, voice, ai, java.util.Map.of());
    }

    public DocumentTextVideoOptions effectiveVideo(String relativePath) {
        var custom = documentBackgrounds.get(relativePath.replace('\\', '/'));
        return custom == null || custom.imagePath().isBlank() ? video : video.withBackgroundImage(custom.imagePath(), custom.visibility());
    }

    public DocumentVideoBatchProfile(DocumentTextVideoOptions video, double imageSlideSeconds,
                                     boolean interpretImages, BatchBranding branding,
                                     BatchOutputKind outputKind,
                                     com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat audioFormat) {
        this(video, imageSlideSeconds, interpretImages, branding, outputKind, audioFormat, "", "");
    }

    public DocumentVideoBatchProfile(DocumentTextVideoOptions video, double imageSlideSeconds,
                                    boolean interpretImages, BatchBranding branding) {
        this(video, imageSlideSeconds, interpretImages, branding, BatchOutputKind.VIDEO, null, "", "");
    }

    public boolean audioOnly() { return outputKind == BatchOutputKind.AUDIO; }
    public String outputDirectory() { return audioOnly() ? "audios-exportados" : "videos-renderizados"; }
    public String outputExtension() { return audioOnly() ? audioFormat.extension() : ".mp4"; }
    public String outputLabel() { return audioOnly() ? "Solo audio · " + audioFormat.displayName() : "Video con audio · MP4"; }

    public static DocumentVideoBatchProfile defaults() {
        return new DocumentVideoBatchProfile(DocumentTextVideoOptions.defaults(), 6.0, false,
                BatchBranding.none());
    }

    private static String cleanEngineId(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
