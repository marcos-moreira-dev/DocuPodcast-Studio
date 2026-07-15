package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectExportFormatPolicyTest {
    private final ProjectExportFormatPolicy policy = new ProjectExportFormatPolicy();

    @Test
    void exposesFormatsOnlyWhenRequiredArtifactsExist() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto", List.of())
        ));
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Guion", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "SEG-001", "Intro", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json",
                List.of(new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-001.wav", 0.25, 1, "")), Instant.now(), Instant.now());

        assertTrue(policy.canExportPodcastWav(List.of(job)));
        assertTrue(policy.canExportProjectBundle(true));
        assertTrue(policy.canExportStoryboardPackage(script, StoryboardDocument.createForScript(script)));
        assertFalse(policy.canExportPodcastWav(List.of()));
        assertFalse(policy.canExportProjectBundle(false));
    }
}
