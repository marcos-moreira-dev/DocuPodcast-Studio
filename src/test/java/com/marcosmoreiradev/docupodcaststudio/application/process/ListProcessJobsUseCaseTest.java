package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListProcessJobsUseCaseTest {
    @Test
    void listsPersistedAudioJobsThroughCommonProcessContract() throws Exception {
        AudioJobSnapshot older = job("JOB-OLD", Instant.parse("2026-01-01T00:00:01Z"));
        AudioJobSnapshot newer = job("JOB-NEW", Instant.parse("2026-01-01T00:00:05Z"));
        ListProcessJobsUseCase useCase = new ListProcessJobsUseCase(new FakeAudioJobRepository(List.of(older, newer)));

        var jobs = useCase.listPersisted(Path.of("project"));

        assertEquals(2, jobs.size());
        assertEquals("JOB-NEW", jobs.get(0).jobId());
        assertEquals(ProcessJobKind.TTS_AUDIO, jobs.get(0).kind());
    }

    private static AudioJobSnapshot job(String id, Instant updatedAt) {
        return new AudioJobSnapshot(
                id,
                "Documento",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1,
                "SEG-001",
                "Fragmento",
                0,
                "Listo",
                "jobs/" + id,
                "jobs/" + id + "/final/audio.wav",
                "jobs/" + id + "/audio-manifest.json",
                List.of(AudioSegmentSnapshot.pending("SEG-001", "Fragmento").completed("jobs/" + id + "/audio/SEG-001.wav", 1.0)),
                Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt
        );
    }

    private record FakeAudioJobRepository(List<AudioJobSnapshot> jobs) implements AudioJobRepository {
        @Override
        public void save(Path projectDirectory, AudioJobSnapshot snapshot) throws IOException {
        }

        @Override
        public Optional<AudioJobSnapshot> load(Path projectDirectory, String jobId) throws IOException {
            return jobs.stream().filter(job -> job.jobId().equals(jobId)).findFirst();
        }

        @Override
        public List<AudioJobSnapshot> list(Path projectDirectory) throws IOException {
            return jobs;
        }

        @Override
        public void deleteAll(Path projectDirectory) throws IOException {
        }
    }
}
