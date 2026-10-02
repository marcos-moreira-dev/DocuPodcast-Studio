package com.marcosmoreiradev.docupodcaststudio.application.video;

/** User-facing progress state for the blocking video render view. */
public record VideoRenderProgress(
        VideoRenderStage stage,
        int completedFrames,
        int totalFrames,
        String currentStep,
        boolean mainWorkspaceBlocked,
        boolean cancellable
) {
    public VideoRenderProgress {
        stage = stage == null ? VideoRenderStage.IDLE : stage;
        completedFrames = Math.max(0, completedFrames);
        totalFrames = Math.max(completedFrames, Math.max(0, totalFrames));
        currentStep = currentStep == null ? "" : currentStep.strip();
    }

    public double ratio() {
        if (totalFrames == 0) {
            return stage == VideoRenderStage.COMPLETED ? 1.0 : 0.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) completedFrames / (double) totalFrames));
    }

    /** Final assembly/mixing/verification have no honest frame denominator. */
    public boolean determinateProgress() {
        return switch (stage) {
            case MIXING_TRACKS, VERIFYING_OUTPUT, IDLE -> false;
            default -> totalFrames > 0;
        };
    }

    public static VideoRenderProgress idle() {
        return new VideoRenderProgress(VideoRenderStage.IDLE, 0, 0, "", false, false);
    }

    public static VideoRenderProgress preparing(int totalFrames) {
        return new VideoRenderProgress(VideoRenderStage.PREPARING, 0, totalFrames,
                "Preparando unidades de audio e imagen.", true, true);
    }

    public static VideoRenderProgress composingMaps(int completedFrames, int totalFrames, String detail) {
        return new VideoRenderProgress(VideoRenderStage.BUILDING_FRAMES, completedFrames, totalFrames,
                detail == null || detail.isBlank() ? "Componiendo mapas teatrales." : detail, true, true);
    }

    public static VideoRenderProgress rendering(int completedFrames, int totalFrames, String currentStep) {
        return new VideoRenderProgress(VideoRenderStage.RENDERING_WITH_FFMPEG, completedFrames, totalFrames,
                currentStep == null || currentStep.isBlank() ? "Renderizando video final..." : currentStep, true, true);
    }

    public static VideoRenderProgress assembling(int totalFrames, String detail) {
        return assembling(0, totalFrames, detail);
    }

    public static VideoRenderProgress assembling(int completedFrames, int totalFrames,
                                                   String detail) {
        return new VideoRenderProgress(VideoRenderStage.ASSEMBLING_FINAL,
                completedFrames, totalFrames,
                detail == null || detail.isBlank()
                        ? "Uniendo y comprimiendo la línea de tiempo completa. Esta fase puede tardar en videos largos."
                        : detail,
                true, true);
    }

    public static VideoRenderProgress mixing(int totalFrames, String detail) {
        return new VideoRenderProgress(VideoRenderStage.MIXING_TRACKS, totalFrames, totalFrames,
                detail == null || detail.isBlank() ? "Mezclando pistas multimedia." : detail, true, true);
    }

    public static VideoRenderProgress verifying(int totalFrames, String targetFile) {
        String suffix = targetFile == null || targetFile.isBlank() ? "" : " " + targetFile;
        return new VideoRenderProgress(VideoRenderStage.VERIFYING_OUTPUT, totalFrames, totalFrames,
                "Verificando MP4 final..." + suffix, true, true);
    }

    public static VideoRenderProgress completed(int totalFrames, String targetFile) {
        String suffix = targetFile == null || targetFile.isBlank() ? "" : " " + targetFile;
        return new VideoRenderProgress(VideoRenderStage.COMPLETED, totalFrames, totalFrames,
                "Video final exportado." + suffix, false, false);
    }

    public static VideoRenderProgress cancelled(int completedFrames, int totalFrames) {
        return new VideoRenderProgress(VideoRenderStage.CANCELLED, completedFrames, totalFrames,
                "Exportación de video cancelada por el usuario.", false, false);
    }

    public static VideoRenderProgress failed(int completedFrames, int totalFrames, String detail) {
        String message = detail == null || detail.isBlank() ? "No se pudo exportar el video final." : detail;
        return new VideoRenderProgress(VideoRenderStage.FAILED, completedFrames, totalFrames, message, false, false);
    }

}
