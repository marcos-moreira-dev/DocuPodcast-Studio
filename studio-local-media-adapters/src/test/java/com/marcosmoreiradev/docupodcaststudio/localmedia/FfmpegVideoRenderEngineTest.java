package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.VideoEncodingPreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoTimelineItem;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoTimelinePlan;
import com.marcosmoreiradev.docupodcaststudio.media.api.TimelineVisualSource;
import com.marcosmoreiradev.docupodcaststudio.media.api.TimelineVisualKind;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationArtifactStaging;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FfmpegVideoRenderEngineTest {
    @Test
    void preservesExactHardwareEncoderAtTheFfmpegAdapterBoundary() {
        assertEquals("h264_nvenc", FfmpegVideoRenderEngine.codec(
                VideoEncodingPreference.NVIDIA_NVENC));
        assertEquals("h264_qsv", FfmpegVideoRenderEngine.codec(
                VideoEncodingPreference.INTEL_QSV));
        assertEquals("h264_amf", FfmpegVideoRenderEngine.codec(
                VideoEncodingPreference.AMD_AMF));
        assertEquals("libx264", FfmpegVideoRenderEngine.codec(
                VideoEncodingPreference.CPU));
    }

    @Test
    void rejectsMultiSecondStreamDivergenceAndAcceptsOneFrameTolerance() {
        var visual = new TimelineVisualSource(TimelineVisualKind.STILL_IMAGE,
                Path.of("frame.png"), 0, 10, 0, 0, "frame", Map.of());
        var item = new VideoTimelineItem("F1", List.of(visual), Path.of("audio.wav"),
                10, Map.of());
        var plan = new VideoTimelinePlan(List.of(item), List.of(), 960, 540, 30,
                VideoEncodingPreference.CPU, Map.of());
        assertThrows(java.io.IOException.class, () -> FfmpegVideoRenderEngine
                .validateDurations(plan, new FfmpegVideoRenderEngine.MediaDurations(10, 12, 12)));
        assertDoesNotThrow(() -> FfmpegVideoRenderEngine.validateDurations(plan,
                new FfmpegVideoRenderEngine.MediaDurations(10.000, 10.030, 10.030)));
    }

    @Test
    void normalizesEveryIntermediateClipToOneConcatSafeAudioFormat() {
        String filter = FfmpegVideoRenderEngine.normalizedTimelineAudioFilter(3.25);

        assertTrue(filter.contains("aresample=44100"));
        assertTrue(filter.contains("sample_rates=44100"));
        assertTrue(filter.contains("channel_layouts=mono"));
        assertTrue(filter.contains("atrim=duration=3.250"));
        assertEquals("pcm_s16le", FfmpegVideoRenderEngine.intermediateAudioCodec(),
                "Los clips intermedios no deben introducir priming AAC por cada unidad");
        assertEquals("clip-0001.mkv", FfmpegVideoRenderEngine.intermediateClipFileName(1),
                "PCM intermedio necesita un contenedor que lo soporte sin recodificar por unidad");
    }

    @Test
    void translatesFfmpegPhysicalProgressIntoPercentageSpeedAndTiming() {
        ArrayList<Double> ratios = new ArrayList<>();
        ArrayList<String> messages = new ArrayList<>();
        ExecutionContext context = new ExecutionContext("ffmpeg-progress-test",
                CancellationToken.NONE,
                (stage, ratio, message) -> {
                    assertEquals("ASSEMBLING", stage);
                    ratios.add(ratio);
                    messages.add(message);
                }, ExecutionPolicy.unbounded(), ResourceLease.NONE,
                GenerationArtifactStaging.NONE);
        var reporter = new FfmpegVideoRenderEngine.FfmpegProgressReporter(context, 10.0);

        reporter.accept("frame=150");
        reporter.accept("fps=30.0");
        reporter.accept("out_time_us=5000000");
        reporter.accept("speed=2.0x");
        reporter.accept("progress=continue");

        assertEquals(0.5, ratios.getFirst(), 0.0001);
        assertTrue(messages.getFirst().contains("50 %"));
        assertTrue(messages.getFirst().contains("transcurrido"));
        assertTrue(messages.getFirst().contains("faltan aprox."));
        assertTrue(messages.getFirst().contains("velocidad 2.00x"));
        assertTrue(messages.getFirst().contains("frame 150"));
        assertTrue(messages.getFirst().contains("30.0 fps"));
    }
}
