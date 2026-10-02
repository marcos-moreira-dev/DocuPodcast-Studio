package com.marcosmoreiradev.docupodcaststudio.localmedia;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Conservative, deterministic prosody direction for Qwen Base voice clones.
 *
 * <p>The Base checkpoint has no instruction channel. Its reference x-vector mainly
 * preserves speaker identity, so this adapter reinforces broad acoustic cues after
 * inference without presenting the result as model-level emotion control.</p>
 */
final class QwenEmotionProsodyProcessor {
    static final String POLICY_VERSION = "qwen-prosody-v2-leading-silence";
    static final float SAMPLE_RATE = 24_000.0f;
    private static final AudioFormat CANONICAL =
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE,
                    16, 1, 2, SAMPLE_RATE, false);

    void process(Path source, Path target, String styleId) throws IOException {
        Path input = source.toAbsolutePath().normalize();
        Path output = target.toAbsolutePath().normalize();
        if (!Files.isRegularFile(input) || Files.size(input) == 0) {
            throw new AudioQualityException("Qwen no produjo un WAV válido para dirigir la prosodia.");
        }
        Files.createDirectories(output.getParent());
        Profile profile = Profile.forStyle(styleId);
        try (AudioInputStream original = AudioSystem.getAudioInputStream(input.toFile());
             AudioInputStream canonical = AudioSystem.getAudioInputStream(CANONICAL, original)) {
            byte[] pcm = readAll(canonical);
            short[] samples = decode(pcm);
            short[] directed = profile.neutral() ? samples : direct(samples, profile);
            directed = QwenLeadingSilence.normalize(directed, (int) SAMPLE_RATE);
            write(output, directed);
        } catch (javax.sound.sampled.UnsupportedAudioFileException unsupported) {
            throw new AudioQualityException("Qwen produjo un archivo que no es WAV PCM compatible.");
        } catch (IllegalArgumentException unsupportedConversion) {
            throw new AudioQualityException("No se pudo normalizar la salida Qwen a WAV PCM mono de 24 kHz.");
        }
    }

    static Profile profileFor(String styleId) {
        return Profile.forStyle(styleId);
    }

    private static short[] direct(short[] input, Profile profile) {
        if (input.length == 0) return input;
        int outputLength = Math.max(1, (int) Math.round(input.length / profile.rate()));
        short[] output = new short[outputLength];
        int fadeSamples = Math.min(outputLength / 2, Math.round(SAMPLE_RATE * 0.012f));
        for (int index = 0; index < outputLength; index++) {
            double sourcePosition = index * profile.rate();
            int left = Math.min(input.length - 1, (int) sourcePosition);
            int right = Math.min(input.length - 1, left + 1);
            double fraction = sourcePosition - left;
            double sample = input[left] * (1.0 - fraction) + input[right] * fraction;
            double progress = index / (double) Math.max(1, outputLength - 1);
            double envelope = 1.0 + profile.contrast()
                    * Math.sin(Math.PI * Math.min(1.0, progress * 2.0));
            if (progress > 0.55) {
                envelope = 1.0 + profile.contrast()
                        * Math.sin(Math.PI * (1.0 - progress) / 0.45);
            }
            double fade = 1.0;
            if (fadeSamples > 0 && index < fadeSamples) fade = index / (double) fadeSamples;
            if (fadeSamples > 0 && index >= outputLength - fadeSamples) {
                fade = Math.min(fade, (outputLength - 1 - index) / (double) fadeSamples);
            }
            double normalized = sample / 32768.0 * profile.gain() * envelope * Math.max(0.0, fade);
            // Smooth limiting reinforces energy without allowing clipped canonical WAVs.
            double limited = Math.tanh(normalized * 1.15) / Math.tanh(1.15);
            output[index] = (short) Math.round(Math.max(-1.0, Math.min(1.0, limited)) * 32767.0);
        }
        return output;
    }

    private static byte[] readAll(AudioInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
    }

    private static short[] decode(byte[] pcm) {
        short[] samples = new short[pcm.length / 2];
        for (int index = 0; index < samples.length; index++) {
            int low = pcm[index * 2] & 0xff;
            int high = pcm[index * 2 + 1];
            samples[index] = (short) ((high << 8) | low);
        }
        return samples;
    }

    private static void write(Path output, short[] samples) throws IOException {
        byte[] pcm = new byte[samples.length * 2];
        for (int index = 0; index < samples.length; index++) {
            pcm[index * 2] = (byte) (samples[index] & 0xff);
            pcm[index * 2 + 1] = (byte) ((samples[index] >>> 8) & 0xff);
        }
        Path temporary = Files.createTempFile(output.getParent(), ".qwen-prosody-", ".wav");
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), CANONICAL, samples.length)) {
            if (AudioSystem.write(stream, AudioFileFormat.Type.WAVE, temporary.toFile()) <= 0) {
                throw new IOException("No se pudo escribir el WAV canónico de Qwen.");
            }
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    record Profile(double gain, double rate, double contrast, String family) {
        Profile {
            gain = Math.max(0.65, Math.min(1.40, gain));
            rate = Math.max(0.90, Math.min(1.08, rate));
            contrast = Math.max(0.0, Math.min(0.16, contrast));
            family = family == null ? "neutral" : family;
        }

        boolean neutral() {
            return gain == 1.0 && rate == 1.0 && contrast == 0.0;
        }

        static Profile forStyle(String styleId) {
            String style = normalize(styleId);
            if (contains(style, "angry", "enoj", "authoritative", "autoritar", "threat", "amenaz",
                    "tense", "tensa", "defiant", "desafiante")) {
                return new Profile(1.32, 1.035, 0.12, "forceful");
            }
            if (contains(style, "happy", "feliz", "joy", "alegr", "enthusi", "entusiasm",
                    "euphor", "eufor", "hope", "esperanz", "surpris", "sorprend")) {
                return new Profile(1.16, 1.045, 0.10, "bright");
            }
            if (contains(style, "sad", "trist", "melanch", "melanc", "repent", "arrepent",
                    "vulnerab", "tired", "cansad")) {
                return new Profile(0.80, 0.935, 0.04, "subdued");
            }
            if (contains(style, "dramatic", "dramat", "hero", "solemn", "myster", "mister",
                    "plead", "suplic", "theatr")) {
                return new Profile(1.18, 0.975, 0.15, "dramatic");
            }
            if (contains(style, "calm", "calmad", "tender", "tiern", "soft", "sutil")) {
                return new Profile(0.88, 0.965, 0.03, "gentle");
            }
            if (contains(style, "rush", "apurad", "nerv", "afraid", "asust")) {
                return new Profile(1.08, 1.065, 0.09, "urgent");
            }
            if (contains(style, "worried", "preocup", "doubt", "dudos", "confus", "confund",
                    "distrust", "desconfi")) {
                return new Profile(0.92, 0.955, 0.07, "hesitant");
            }
            if (contains(style, "ironic", "ironic", "sarcast", "mock", "burlon")) {
                return new Profile(1.05, 1.02, 0.08, "wry");
            }
            if (contains(style, "serious", "seria", "cold", "fria", "bored", "aburr",
                    "resign")) {
                return new Profile(0.86, 0.96, 0.02, "restrained");
            }
            return new Profile(1.0, 1.0, 0.0, "neutral");
        }

        private static String normalize(String value) {
            return value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                    .replace('_', '-');
        }

        private static boolean contains(String value, String... candidates) {
            for (String candidate : candidates) if (value.contains(candidate)) return true;
            return false;
        }
    }
}
