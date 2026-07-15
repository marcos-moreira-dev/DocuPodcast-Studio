package com.marcosmoreiradev.docupodcaststudio.infrastructure.playback;

import javax.sound.sampled.AudioFormat;

/**
 * Small pitch-preserving PCM time-stretch processor for generated WAV fragments.
 *
 * <p>The reader only exposes 1x, 1.5x and 1.75x. For the faster modes this class shortens
 * the waveform with overlap-add instead of raising the output sample rate, so the voice keeps
 * a more natural pitch while the fragment finishes earlier.</p>
 */
final class PcmTimeStretchProcessor {
    private PcmTimeStretchProcessor() {
    }

    static byte[] speedUpPreservePitch(byte[] pcm, AudioFormat format, double rate) {
        double normalized = normalizeRate(rate);
        if (pcm == null || pcm.length == 0 || normalized <= 1.01 || !supported(format)) {
            return pcm == null ? new byte[0] : pcm;
        }
        int channels = Math.max(1, format.getChannels());
        int frameSize = Math.max(channels * 2, format.getFrameSize());
        int inputFrames = pcm.length / frameSize;
        if (inputFrames < 256) {
            return pcm;
        }
        float[][] input = decodePcm16(pcm, inputFrames, channels, frameSize);
        int window = windowSize(format.getSampleRate(), inputFrames);
        int synthesisHop = Math.max(64, window / 4);
        int analysisHop = Math.max(synthesisHop + 1, (int) Math.round(synthesisHop * normalized));
        int searchRadius = Math.max(32, Math.min(window / 4, analysisHop / 2));
        int outputCapacity = Math.max(window * 2, (int) Math.ceil(inputFrames / normalized) + window * 3);
        float[][] output = new float[channels][outputCapacity];
        float[] weights = new float[outputCapacity];
        double[] windowValues = hann(window);
        int inputPos = 0;
        int outputPos = 0;
        boolean first = true;
        while (inputPos + window <= inputFrames && outputPos + window <= outputCapacity) {
            int alignedInput = first ? inputPos : bestCorrelationOffset(input, output, weights, inputPos, outputPos, window, searchRadius, inputFrames);
            overlapAdd(input, output, weights, windowValues, alignedInput, outputPos, window, channels);
            first = false;
            inputPos += analysisHop;
            outputPos += synthesisHop;
        }
        int producedFrames = Math.min(outputCapacity, Math.max(window, outputPos + window));
        return encodePcm16(output, weights, producedFrames, channels, frameSize);
    }

    private static int bestCorrelationOffset(float[][] input, float[][] output, float[] weights,
                                             int idealInputPos, int outputPos, int window,
                                             int searchRadius, int inputFrames) {
        int from = Math.max(0, idealInputPos - searchRadius);
        int to = Math.min(inputFrames - window, idealInputPos + searchRadius);
        int overlap = Math.max(64, window / 2);
        double bestScore = Double.NEGATIVE_INFINITY;
        int best = Math.max(0, Math.min(idealInputPos, inputFrames - window));
        for (int candidate = from; candidate <= to; candidate += 8) {
            double cross = 0.0;
            double inEnergy = 0.0;
            double outEnergy = 0.0;
            for (int i = 0; i < overlap && outputPos + i < output[0].length && candidate + i < inputFrames; i += 2) {
                double outSample = weights[outputPos + i] > 0.0f ? output[0][outputPos + i] / weights[outputPos + i] : 0.0;
                double inSample = input[0][candidate + i];
                cross += outSample * inSample;
                outEnergy += outSample * outSample;
                inEnergy += inSample * inSample;
            }
            double score = cross / Math.sqrt(Math.max(1.0e-9, outEnergy * inEnergy));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static void overlapAdd(float[][] input, float[][] output, float[] weights, double[] windowValues,
                                   int inputPos, int outputPos, int window, int channels) {
        for (int i = 0; i < window; i++) {
            float weight = (float) windowValues[i];
            int outIndex = outputPos + i;
            int inIndex = inputPos + i;
            weights[outIndex] += weight;
            for (int channel = 0; channel < channels; channel++) {
                output[channel][outIndex] += input[channel][inIndex] * weight;
            }
        }
    }

    private static float[][] decodePcm16(byte[] pcm, int frames, int channels, int frameSize) {
        float[][] decoded = new float[channels][frames];
        for (int frame = 0; frame < frames; frame++) {
            int base = frame * frameSize;
            for (int channel = 0; channel < channels; channel++) {
                int index = base + channel * 2;
                int low = pcm[index] & 0xff;
                int high = pcm[index + 1];
                short sample = (short) ((high << 8) | low);
                decoded[channel][frame] = sample / 32768.0f;
            }
        }
        return decoded;
    }

    private static byte[] encodePcm16(float[][] samples, float[] weights, int frames, int channels, int frameSize) {
        byte[] encoded = new byte[frames * frameSize];
        for (int frame = 0; frame < frames; frame++) {
            float weight = weights[frame] <= 0.0f ? 1.0f : weights[frame];
            int base = frame * frameSize;
            for (int channel = 0; channel < channels; channel++) {
                float normalized = samples[channel][frame] / weight;
                int value = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(normalized * 32767.0f)));
                int index = base + channel * 2;
                encoded[index] = (byte) (value & 0xff);
                encoded[index + 1] = (byte) ((value >>> 8) & 0xff);
            }
        }
        return encoded;
    }

    private static double[] hann(int size) {
        double[] values = new double[size];
        if (size == 1) {
            values[0] = 1.0;
            return values;
        }
        for (int i = 0; i < size; i++) {
            values[i] = 0.5 - 0.5 * Math.cos((2.0 * Math.PI * i) / (size - 1));
        }
        return values;
    }

    private static int windowSize(float sampleRate, int frames) {
        int target = Math.max(512, Math.min(4096, Math.round(sampleRate * 0.046f)));
        int size = 1;
        while (size < target) {
            size <<= 1;
        }
        return Math.max(128, Math.min(size, Math.max(128, frames / 2)));
    }

    private static boolean supported(AudioFormat format) {
        return format != null
                && format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && format.getSampleSizeInBits() == 16
                && !format.isBigEndian()
                && format.getChannels() >= 1
                && format.getFrameSize() >= format.getChannels() * 2;
    }

    private static double normalizeRate(double rate) {
        if (rate >= 1.74) {
            return 1.75;
        }
        if (rate >= 1.49) {
            return 1.5;
        }
        return 1.0;
    }
}
