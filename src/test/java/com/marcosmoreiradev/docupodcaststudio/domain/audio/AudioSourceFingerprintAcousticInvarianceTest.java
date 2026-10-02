package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionRevisionRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioSourceFingerprintAcousticInvarianceTest {

    @Test
    void visualRegionRevisionAndDerivedVisualMetadataDoNotInvalidateAudio() {
        AudioSourceFingerprint before = AudioSourceFingerprint.pdf(
                List.of(new PdfRegionRevisionRef("R1", 1, 1)),
                "Texto hablado", "VOC|NEUTRAL|sample", "tts-preprocessing-v1",
                "bbox=10,20,300,80;zoom=100;highlight=0.30");
        AudioSourceFingerprint after = AudioSourceFingerprint.pdf(
                List.of(new PdfRegionRevisionRef("R1", 1, 99)),
                "Texto hablado", "VOC|NEUTRAL|sample", "tts-preprocessing-v1",
                "bbox=20,30,290,70;lineBboxes=x;zoom=150");

        assertTrue(before.reusableFor(after));
        assertTrue(after.reusableFor(before));
    }

    @Test
    void spokenTextVoiceAndAcousticPreprocessingRemainInvalidating() {
        AudioSourceFingerprint base = AudioSourceFingerprint.generic(
                "Texto hablado", "VOC-A|NEUTRAL", "tts-preprocessing-v1");

        assertFalse(base.reusableFor(AudioSourceFingerprint.generic(
                "Texto cambiado", "VOC-A|NEUTRAL", "tts-preprocessing-v1")));
        assertFalse(base.reusableFor(AudioSourceFingerprint.generic(
                "Texto hablado", "VOC-B|NEUTRAL", "tts-preprocessing-v1")));
        assertFalse(base.reusableFor(AudioSourceFingerprint.generic(
                "Texto hablado", "VOC-A|NEUTRAL", "tts-preprocessing-v2")));
    }

    @Test
    void nullAndEmptyAcousticFieldsHaveTheSameCanonicalIdentity() {
        AudioSourceFingerprint nulls = AudioSourceFingerprint.generic(
                "Texto", null, null);
        AudioSourceFingerprint empties = AudioSourceFingerprint.generic(
                "Texto", "", "");

        org.junit.jupiter.api.Assertions.assertEquals(
                empties.voiceConfigurationSha256(), nulls.voiceConfigurationSha256());
        org.junit.jupiter.api.Assertions.assertEquals(
                empties.preprocessingSha256(), nulls.preprocessingSha256());
    }
}
