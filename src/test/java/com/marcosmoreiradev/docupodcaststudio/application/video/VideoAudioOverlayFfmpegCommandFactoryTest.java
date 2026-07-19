package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoAudioOverlayFfmpegCommandFactoryTest {
    @Test
    void buildsTrimDelayVolumeAndMixFilters() {
        VideoAudioOverlayPlan plan = new VideoAudioOverlayPlan(List.of(
                new VideoAudioOverlayPlan.Input("TRACK-1", Path.of("ambiente.wav"), 1.0, 6.0, 3.5, 0.30, 0.5)));

        List<String> command = new VideoAudioOverlayFfmpegCommandFactory().build(
                Path.of("ffmpeg"), Path.of("narracion.mp4"), Path.of("final.mp4"), plan, 9.0);
        String joined = String.join(" ", command);

        assertTrue(joined.contains("apad=pad_dur=9.000"));
        assertTrue(joined.contains("atrim=duration=9.000"));
        assertTrue(joined.contains("atrim=start=1.000:end=6.000"));
        assertTrue(joined.contains("adelay=delays=3500:all=1"));
        assertTrue(joined.contains("volume=0.300"));
        assertTrue(joined.contains("afade=t=in:st=0:d=0.500"));
        assertTrue(joined.contains("afade=t=out:st=4.500:d=0.500"));
        assertTrue(joined.contains("amix=inputs=2:duration=longest"));
        assertTrue(joined.contains("-t 9.000"));
        assertTrue(joined.contains("-ar 48000 -ac 2"));
        assertTrue(joined.contains("aresample=48000"));
        assertFalse(command.contains("-shortest"));
    }
}
