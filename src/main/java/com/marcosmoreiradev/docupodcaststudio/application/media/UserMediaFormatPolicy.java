package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Classifies media files accepted by the user-facing audio assignment flow.
 *
 * <p>The product intentionally keeps one simple action: "Asignar audio". WAV can be copied directly; MP3/M4A/FLAC/OGG are normalized through
 * FFmpeg when available, while video files are accepted only as sources for audio
 * extraction. The UI should not split this into redundant buttons such as voice
 * versus sound effect; the user names and chooses the file according to intent.</p>
 */
public final class UserMediaFormatPolicy {
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "flac", "ogg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "mkv", "webm");

    public UserMediaAssetKind classify(Path file) {
        String extension = extension(file);
        if (AUDIO_EXTENSIONS.contains(extension)) {
            return UserMediaAssetKind.AUDIO_FILE;
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return UserMediaAssetKind.VIDEO_FOR_AUDIO;
        }
        return UserMediaAssetKind.UNSUPPORTED;
    }

    public boolean supported(Path file) {
        return classify(file) != UserMediaAssetKind.UNSUPPORTED;
    }

    public boolean audioRequiresNormalization(Path file) {
        return classify(file) == UserMediaAssetKind.AUDIO_FILE && !"wav".equals(extension(file));
    }

    public String extension(Path file) {
        String name = file == null || file.getFileName() == null ? "" : file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String mimeType(Path file) {
        return mimeType(extension(file));
    }

    public String mimeType(String extension) {
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "flac" -> "audio/flac";
            case "ogg" -> "audio/ogg";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }

    public String supportedFormatSummary() {
        return "Audio: MP3/WAV/M4A/FLAC/OGG; video para extraer audio: MP4/MOV/MKV/WEBM";
    }
}
