package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the bundled FFmpeg tools before falling back to a user configured executable. */
public final class EmbeddedFfmpegLocator {
    public static final String EMBEDDED_FFMPEG_RELATIVE_PATH = "tools/ffmpeg/bin/ffmpeg.exe";
    public static final String EMBEDDED_FFPROBE_RELATIVE_PATH = "tools/ffmpeg/bin/ffprobe.exe";

    public FfmpegToolDiscovery locate(Path applicationRoot, Path configuredFfmpeg) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path embeddedFfmpeg = paths.ffmpegExecutable();
        Path embeddedFfprobe = paths.ffprobeExecutable();
        if (Files.isRegularFile(embeddedFfmpeg)) {
            return new FfmpegToolDiscovery(
                    embeddedFfmpeg,
                    Files.isRegularFile(embeddedFfprobe) ? embeddedFfprobe : null,
                    true,
                    true,
                    "FFmpeg embebido encontrado en tools/ffmpeg."
            );
        }
        if (configuredFfmpeg != null && Files.isRegularFile(configuredFfmpeg)) {
            return new FfmpegToolDiscovery(
                    configuredFfmpeg.toAbsolutePath().normalize(),
                    null,
                    false,
                    true,
                    "FFmpeg externo configurado por el usuario."
            );
        }
        return new FfmpegToolDiscovery(
                embeddedFfmpeg,
                embeddedFfprobe,
                true,
                false,
                "No se encontró FFmpeg embebido; se exportará paquete renderizable y se podrá configurar FFmpeg externo."
        );
    }
}
