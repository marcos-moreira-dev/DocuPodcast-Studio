package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class QwenEmotionProsodyProcessorTest {
    @TempDir Path temporary;

    @Test void emotionalProfilesAreDistinctAndBounded() {
        var neutral = QwenEmotionProsodyProcessor.profileFor("NEUTRAL");
        var happy = QwenEmotionProsodyProcessor.profileFor("HAPPY");
        var sad = QwenEmotionProsodyProcessor.profileFor("SAD");
        var angry = QwenEmotionProsodyProcessor.profileFor("ANGRY");
        var dramatic = QwenEmotionProsodyProcessor.profileFor("DRAMATIC");

        assertTrue(neutral.neutral());
        assertEquals("bright", happy.family());
        assertTrue(happy.rate() > neutral.rate());
        assertEquals("subdued", sad.family());
        assertTrue(sad.rate() < neutral.rate());
        assertEquals("forceful", angry.family());
        assertTrue(angry.gain() > happy.gain());
        assertEquals("dramatic", dramatic.family());
        assertTrue(dramatic.contrast() > happy.contrast());
        assertEquals("hesitant", QwenEmotionProsodyProcessor.profileFor("CONFUSED").family());
        assertEquals("wry", QwenEmotionProsodyProcessor.profileFor("SARCASTIC").family());
        assertEquals("restrained", QwenEmotionProsodyProcessor.profileFor("BORED").family());
    }

    @Test void writesCanonicalWavAndChangesDurationAccordingToStyle() throws Exception {
        Path input = temporary.resolve("input.wav");
        writeTone(input, 1.0);
        Path happy = temporary.resolve("happy.wav");
        Path sad = temporary.resolve("sad.wav");
        QwenEmotionProsodyProcessor processor = new QwenEmotionProsodyProcessor();

        processor.process(input, happy, "feliz");
        processor.process(input, sad, "triste");

        try (AudioInputStream happyAudio = AudioSystem.getAudioInputStream(happy.toFile());
             AudioInputStream sadAudio = AudioSystem.getAudioInputStream(sad.toFile())) {
            assertEquals(24_000.0f, happyAudio.getFormat().getSampleRate());
            assertEquals(16, happyAudio.getFormat().getSampleSizeInBits());
            assertEquals(1, happyAudio.getFormat().getChannels());
            assertTrue(happyAudio.getFrameLength() < 24_000);
            assertTrue(sadAudio.getFrameLength() > 24_000);
        }
    }

    private static void writeTone(Path target, double seconds) throws Exception {
        float sampleRate = 24_000.0f;
        int frames = (int) Math.round(sampleRate * seconds);
        byte[] pcm = new byte[frames * 2];
        for (int index = 0; index < frames; index++) {
            short sample = (short) Math.round(Math.sin(2 * Math.PI * 220 * index / sampleRate) * 8_000);
            pcm[index * 2] = (byte) (sample & 0xff);
            pcm[index * 2 + 1] = (byte) ((sample >>> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(pcm), format, frames)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, target.toFile());
        }
    }
}
