package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RC smoke gate for the user-facing final MP4 flow.
 *
 * <p>This does not render video by itself. It converts export readiness and FFmpeg evidence
 * into one operational decision so the smoke script/UI can explain why the MP4 smoke can run
 * or why it must be skipped before starting a long render.</p>
 */
public final class InspectFinalVideoSmokeReadinessUseCase {
    public UserVisibleDecision inspect(ExportReadinessReport readiness, FfmpegRuntimeReport ffmpeg) {
        Objects.requireNonNull(readiness, "readiness");
        ExportReadinessItem mp4 = readiness.items().stream()
                .filter(item -> item.kind() == ExportableArtifactKind.FINAL_VIDEO_MP4)
                .findFirst()
                .orElse(null);
        ArrayList<String> blockers = new ArrayList<>();
        if (mp4 == null) {
            blockers.add("No existe readiness para Video MP4 final.");
        } else if (mp4.blocked()) {
            blockers.addAll(mp4.missingRequirements());
        }
        if (ffmpeg == null || !ffmpeg.readyForFinalVideo()) {
            blockers.add("Video local/FFmpeg no está confirmado para MP4 final.");
            if (ffmpeg != null) {
                blockers.addAll(ffmpeg.warnings());
            }
        }
        if (!blockers.isEmpty()) {
            return UserVisibleDecision.warning(
                    "Smoke de video final no listo",
                    "No se debe iniciar el smoke MP4 todavía. " + join(blockers));
        }
        return UserVisibleDecision.information(
                "Smoke de video final listo",
                "El proyecto tiene readiness MP4 y Video local/FFmpeg está confirmado. Puedes ejecutar el smoke de video final.");
    }

    private static String join(List<String> values) {
        return String.join(" ", values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList());
    }
}
