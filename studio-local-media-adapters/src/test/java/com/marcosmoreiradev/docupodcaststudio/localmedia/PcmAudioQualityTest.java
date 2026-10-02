package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PcmAudioQualityTest {
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="docupodcast.quality.sampleRoot", matches=".+")
    void reportedFragmentIsSuspectButNeighborsAreAccepted() throws Exception {
        var root=java.nio.file.Path.of(System.getProperty("docupodcast.quality.sampleRoot"));
        assertEquals(PcmAudioQuality.Verdict.SUSPECT, PcmAudioQuality.inspect(root.resolve("SEG-745-U001.wav"),"Ya no pedimos permiso").verdict());
        assertEquals(PcmAudioQuality.Verdict.ACCEPT, PcmAudioQuality.inspect(root.resolve("SEG-744-U001.wav"),"o no se llama libertad.").verdict());
        assertEquals(PcmAudioQuality.Verdict.ACCEPT, PcmAudioQuality.inspect(root.resolve("SEG-746-U001.wav"),"para decidir y mandar;").verdict());
    }
    private short[] tone(int count, int start, int length, int amplitude) {
        short[] samples = new short[count];
        for (int i = start; i < Math.min(count, start + length); i++)
            samples[i] = (short) (amplitude * Math.sin(i * .0573));
        return samples;
    }
    @Test void silenceAndClickAreInvalid() {
        short[] click = new short[24000]; click[12000] = 32000;
        assertEquals(PcmAudioQuality.Verdict.INVALID, PcmAudioQuality.inspect(click,24000,"hola").verdict());
        assertEquals(PcmAudioQuality.Verdict.INVALID, PcmAudioQuality.inspect(new short[24000],24000,"hola").verdict());
    }
    @Test void weakBriefSignalForLongTextIsSuspect() {
        assertEquals(PcmAudioQuality.Verdict.SUSPECT,
                PcmAudioQuality.inspect(tone(96000,6000,16000,3000),24000,"Ya no pedimos permiso").verdict());
    }
    @Test void trailingSilenceAloneIsNotRejectedAndSamplesRemainUnchanged() {
        short[] input = tone(240000,1000,36000,10000), original = input.clone();
        assertEquals(PcmAudioQuality.Verdict.ACCEPT, PcmAudioQuality.inspect(input,24000,"Ya no pedimos permiso").verdict());
        assertArrayEquals(original,input);
    }
    @Test void softVoiceAndShortTextAreNotRejected() {
        assertEquals(PcmAudioQuality.Verdict.ACCEPT,
                PcmAudioQuality.inspect(tone(72000,0,70000,200),24000,"Ya no pedimos permiso").verdict());
        assertEquals(PcmAudioQuality.Verdict.ACCEPT,
                PcmAudioQuality.inspect(tone(24000,0,5000,3000),24000,"sí").verdict());
    }
    @Test void repeatedToneIsOnlyAWarning() {
        short[] samples = new short[24000];
        for (int i=0;i<samples.length;i++) samples[i]=(short)((i%48<24)?10000:-10000);
        var result=PcmAudioQuality.inspect(samples,24000,"aaaa");
        assertEquals(PcmAudioQuality.Verdict.ACCEPT,result.verdict());
        assertTrue(result.repetitionWarning());
    }
}
