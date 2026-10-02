package com.marcosmoreiradev.docupodcaststudio.localmedia;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Deterministic acoustic screening, not speech recognition. No samples are modified. */
final class PcmAudioQuality {
    enum Verdict { ACCEPT, SUSPECT, INVALID }
    record Report(Verdict verdict, double durationSeconds, double activeSeconds,
                  double peakDb, boolean repetitionWarning, String reason) {}

    static Report inspect(Path wav, String text) throws IOException {
        var format = new AudioFormat(24000, 16, 1, true, false);
        try (var source = AudioSystem.getAudioInputStream(wav.toFile());
             var pcm = AudioSystem.getAudioInputStream(format, source)) {
            byte[] bytes = pcm.readAllBytes();
            short[] samples = new short[bytes.length / 2];
            for (int i = 0; i < samples.length; i++)
                samples[i] = (short) ((bytes[2 * i] & 255) | (bytes[2 * i + 1] << 8));
            return inspect(samples, 24000, text);
        } catch (javax.sound.sampled.UnsupportedAudioFileException | IllegalArgumentException ex) {
            throw new AudioQualityException("Archivo de voz ilegible o incompatible.");
        }
    }

    static Report inspect(short[] samples, int rate, String text) {
        int window = Math.max(1, rate / 50);
        int peak = 0, active = 0, sustained = 0, longest = 0, repeated = 0;
        for (short sample : samples) peak = Math.max(peak, Math.abs((int) sample));
        double activeThreshold = Math.max(32, Math.min(184, peak * .08));
        Map<Integer, Integer> windows = new HashMap<>();
        for (int offset = 0; offset < samples.length; offset += window) {
            int end = Math.min(samples.length, offset + window), hash = 1;
            double energy = 0;
            for (int i = offset; i < end; i++) {
                int value = samples[i];
                peak = Math.max(peak, Math.abs(value));
                energy += (double) value * value;
                hash = 31 * hash + value;
            }
            double rms = Math.sqrt(energy / (end - offset));
            sustained = rms >= 32 ? sustained + end - offset : 0;
            longest = Math.max(longest, sustained);
            if (rms >= activeThreshold) { // Relative floor protects soft voices.
                active += end - offset;
                repeated = Math.max(repeated, windows.merge(hash, 1, Integer::sum));
            }
        }
        double duration = samples.length / (double) rate, voice = active / (double) rate;
        double peakDb = peak == 0 ? -120 : 20 * Math.log10(peak / 32768.0);
        boolean repetition = repeated >= 20; // Exact repeated blocks: warning only, including tonal audio.
        long letters = text == null ? 0 : text.codePoints().filter(Character::isLetterOrDigit).count();
        if (longest < rate * .04)
            return new Report(Verdict.INVALID, duration, voice, peakDb, repetition, "Sin señal sostenida suficiente.");
        // Never reject for trailing silence alone, or for a quiet voice alone.
        boolean suspect = letters >= 12 && voice < letters * .055
                && voice < duration * .4 && peakDb < -12;
        return new Report(suspect ? Verdict.SUSPECT : Verdict.ACCEPT, duration, voice, peakDb, repetition,
                suspect ? "Señal muy débil y breve para el texto solicitado; requiere revisión."
                        : repetition ? "Advertencia experimental: bloques de señal repetidos; revisar si se oye ruido."
                        : "Sin anomalías acústicas concluyentes.");
    }
}
