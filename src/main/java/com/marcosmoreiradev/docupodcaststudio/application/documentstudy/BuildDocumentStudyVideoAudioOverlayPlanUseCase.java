package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Builds a looping sequential documentary soundtrack without theatre dependencies. */
public final class BuildDocumentStudyVideoAudioOverlayPlanUseCase {
    public VideoAudioOverlayPlan build(DocuPodcastProject project, Path projectDirectory, SimpleVideoPlan videoPlan)
            throws IOException {
        if (project == null || videoPlan == null) return VideoAudioOverlayPlan.emptyPlan();
        List<DocumentStudyMusicTrack> tracks = project.study().documentaryVideoConfiguration().musicTracks();
        if (tracks.isEmpty() || videoPlan.totalDurationSeconds() <= 0.0) return VideoAudioOverlayPlan.emptyPlan();
        Path root = projectDirectory.toAbsolutePath().normalize();
        ArrayList<VideoAudioOverlayPlan.Input> inputs = new ArrayList<>();
        double cursor = 0.0;
        int occurrence = 1;
        while (cursor < videoPlan.totalDurationSeconds() - 0.001) {
            boolean advanced = false;
            for (DocumentStudyMusicTrack track : tracks) {
                ProjectAssetReference asset = project.assets().byId(track.assetId())
                        .orElseThrow(() -> new IOException("No existe la pista documental " + track.assetId() + "."));
                Path file = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                    throw new IOException("La pista documental no resuelve dentro del proyecto: " + track.assetId());
                }
                if (track.durationSeconds() <= 0.0) {
                    throw new IOException("La pista documental no tiene duracion medible: " + track.assetId());
                }
                double remaining = videoPlan.totalDurationSeconds() - cursor;
                double use = Math.min(track.durationSeconds(), remaining);
                inputs.add(new VideoAudioOverlayPlan.Input(
                        track.id() + "-" + occurrence++, file, 0.0, use, cursor, track.volume(),
                        Math.min(0.35, use * 0.10)));
                cursor += use;
                advanced = true;
                if (cursor >= videoPlan.totalDurationSeconds() - 0.001) break;
            }
            if (!advanced) break;
        }
        return new VideoAudioOverlayPlan(inputs);
    }
}
