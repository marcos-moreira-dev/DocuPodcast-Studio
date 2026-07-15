package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Lists long-running jobs through the common T94 process contract. */
public final class ListProcessJobsUseCase {
    private final AudioJobRepository audioJobRepository;
    private final AudioJobProcessMapper audioJobProcessMapper;
    private final VideoRenderJobRepository videoRenderJobRepository;
    private final VideoRenderJobProcessMapper videoRenderJobProcessMapper;

    public ListProcessJobsUseCase(AudioJobRepository audioJobRepository) {
        this(audioJobRepository, new AudioJobProcessMapper(), null, new VideoRenderJobProcessMapper());
    }

    public ListProcessJobsUseCase(AudioJobRepository audioJobRepository, AudioJobProcessMapper audioJobProcessMapper) {
        this(audioJobRepository, audioJobProcessMapper, null, new VideoRenderJobProcessMapper());
    }

    public ListProcessJobsUseCase(AudioJobRepository audioJobRepository,
                                  AudioJobProcessMapper audioJobProcessMapper,
                                  VideoRenderJobRepository videoRenderJobRepository,
                                  VideoRenderJobProcessMapper videoRenderJobProcessMapper) {
        this.audioJobRepository = Objects.requireNonNull(audioJobRepository, "audioJobRepository");
        this.audioJobProcessMapper = Objects.requireNonNull(audioJobProcessMapper, "audioJobProcessMapper");
        this.videoRenderJobRepository = videoRenderJobRepository;
        this.videoRenderJobProcessMapper = Objects.requireNonNull(videoRenderJobProcessMapper, "videoRenderJobProcessMapper");
    }

    public List<ProcessJobSnapshot> listPersisted(Path projectDirectory) throws IOException {
        List<AudioJobSnapshot> audioJobs = audioJobRepository.list(projectDirectory);
        java.util.ArrayList<ProcessJobSnapshot> snapshots = new java.util.ArrayList<>(audioJobProcessMapper.mapAll(audioJobs));
        if (videoRenderJobRepository != null) {
            List<VideoRenderJobSnapshot> videoJobs = videoRenderJobRepository.list(projectDirectory);
            snapshots.addAll(videoRenderJobProcessMapper.mapAll(videoJobs));
        }
        return snapshots.stream()
                .sorted(Comparator.comparing(ProcessJobSnapshot::updatedAt).reversed())
                .toList();
    }

    public List<ProcessJobSnapshot> fromAudioJobs(List<AudioJobSnapshot> audioJobs) {
        return audioJobProcessMapper.mapAll(audioJobs).stream()
                .sorted(Comparator.comparing(ProcessJobSnapshot::updatedAt).reversed())
                .toList();
    }
}
