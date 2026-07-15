package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;

/** Result of exporting the auditable simple-video package. */
public record SimpleVideoPackageExportResult(
        Path rootDirectory,
        Path planMarkdownFile,
        Path framesCsvFile,
        Path ffmpegConcatFile,
        Path renderScriptFile,
        Path renderManifestFile,
        Path renderCommandsFile,
        Path renderStateFile,
        String renderModeLabel,
        boolean renderableAsMp4,
        String outputFileName,
        int frameCount,
        double totalDurationSeconds,
        long framesMissingImage,
        long framesMissingAudio
) {
    public SimpleVideoPackageExportResult {
        renderModeLabel = renderModeLabel == null || renderModeLabel.isBlank()
                ? "PACKAGE_NEEDS_REVIEW"
                : renderModeLabel.strip();
        outputFileName = outputFileName == null || outputFileName.isBlank()
                ? "video-simple.mp4"
                : outputFileName.strip();
    }

    public String honestStatusLabel() {
        if (renderableAsMp4) {
            return "paquete listo para render MP4";
        }
        if ("PACKAGE_READY_FFMPEG_REQUIRED".equals(renderModeLabel)) {
            return "paquete renderizable pendiente de FFmpeg";
        }
        return "paquete auditable que requiere revisión";
    }
}
