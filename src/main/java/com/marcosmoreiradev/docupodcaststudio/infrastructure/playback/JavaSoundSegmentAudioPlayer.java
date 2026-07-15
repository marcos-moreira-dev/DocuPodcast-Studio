package com.marcosmoreiradev.docupodcaststudio.infrastructure.playback;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Segment audio player backed by Java Sound streaming output. It is WAV-first and avoids Clip-only limitations. */
public final class JavaSoundSegmentAudioPlayer implements SegmentAudioPlayer {
    private final Object lock = new Object();
    private SourceDataLine line;
    private Thread playbackThread;
    private Path currentFile;
    private volatile boolean stopRequested;
    private volatile boolean paused;
    private volatile boolean playing;
    private volatile long playbackGeneration;
    private volatile double playbackStartOffsetSeconds;
    private volatile double lastKnownPositionSeconds;
    private volatile double playbackRate = 1.0;
    private volatile double activeLinePlaybackRate = 1.0;
    private volatile double volume = 1.0;
    private volatile double stopAtSeconds = Double.POSITIVE_INFINITY;
    private volatile double fadeStartSeconds;
    private volatile double fadeEndSeconds = Double.POSITIVE_INFINITY;
    private volatile double fadeDurationSeconds;
    private volatile Consumer<Path> onPlaybackFinished = ignored -> { };
    private String lastStatus = "Reproductor interno listo para audio por fragmento.";

    @Override
    public void play(Path audioFile, double startSeconds) throws IOException {
        Path target = audioFile == null ? null : audioFile.toAbsolutePath().normalize();
        if (target == null || !Files.exists(target)) {
            lastStatus = "No existe el audio de segmento: " + audioFile;
            throw new IOException(lastStatus);
        }
        stop();
        try {
            AudioInputStream sourceStream = playableStream(target);
            AudioFormat sourceFormat = sourceStream.getFormat();
            double rate = playbackRate;
            long skipFrames = Math.max(0L, Math.round(startSeconds * sourceFormat.getFrameRate()));
            skipFrames(sourceStream, skipFrames);
            AudioInputStream stream = playbackStream(sourceStream, sourceFormat, rate);
            AudioFormat outputFormat = stream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, outputFormat);
            SourceDataLine opened = (SourceDataLine) AudioSystem.getLine(info);
            opened.open(outputFormat);
            long generation;
            synchronized (lock) {
                generation = ++playbackGeneration;
                line = opened;
                currentFile = target;
                stopRequested = false;
                paused = false;
                playing = true;
                activeLinePlaybackRate = rate;
                playbackStartOffsetSeconds = Math.max(0.0, startSeconds);
                lastKnownPositionSeconds = playbackStartOffsetSeconds;
                lastStatus = "Reproduciendo " + target.getFileName() + " · " + rateLabel(rate) + speedModeLabel(rate) + " · " + formatLabel(outputFormat);
            }
            playbackThread = new Thread(() -> streamAudio(stream, opened, outputFormat, rate, generation), "docupodcast-segment-player");
            playbackThread.setDaemon(true);
            playbackThread.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException ex) {
            lastStatus = "El reproductor interno no pudo abrir este WAV: " + ex.getMessage();
            throw new IOException(lastStatus, ex);
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            paused = true;
            if (line != null) {
                line.stop();
            }
            lastStatus = "Playback pausado a " + rateLabel(playbackRate) + ".";
        }
    }

    @Override
    public void resume() {
        synchronized (lock) {
            paused = false;
            if (line != null) {
                line.start();
            }
            lock.notifyAll();
            lastStatus = "Playback reanudado a " + rateLabel(playbackRate) + ".";
        }
    }

    @Override
    public void stop() {
        SourceDataLine toClose;
        synchronized (lock) {
            playbackGeneration++;
            stopRequested = true;
            paused = false;
            playing = false;
            toClose = line;
            line = null;
            currentFile = null;
            lastKnownPositionSeconds = 0.0;
            playbackStartOffsetSeconds = 0.0;
            activeLinePlaybackRate = playbackRate;
            lock.notifyAll();
        }
        if (toClose != null) {
            toClose.stop();
            toClose.flush();
            toClose.close();
        }
        lastStatus = "Playback detenido.";
    }

    @Override
    public void setPlaybackRate(double rate) {
        double normalized = normalizeRate(rate);
        Path file;
        boolean restart;
        boolean wasPaused;
        double position;
        synchronized (lock) {
            playbackRate = normalized;
            file = currentFile;
            restart = playing && file != null;
            wasPaused = paused;
        }
        if (!restart) {
            lastStatus = "Velocidad de lectura preparada: " + rateLabel(normalized) + ".";
            return;
        }
        position = currentPositionSeconds();
        try {
            play(file, position);
            if (wasPaused) {
                pause();
            }
            lastStatus = "Velocidad de lectura cambiada a " + rateLabel(normalized) + ".";
        } catch (IOException ex) {
            lastStatus = "No se pudo cambiar la velocidad de lectura: " + ex.getMessage();
        }
    }

    @Override
    public void setVolume(double volume) {
        this.volume = Double.isFinite(volume) ? Math.max(0.0, Math.min(1.0, volume)) : 1.0;
    }

    @Override
    public void setStopAtSeconds(double sourcePositionSeconds) {
        stopAtSeconds = Double.isFinite(sourcePositionSeconds)
                ? Math.max(0.0, sourcePositionSeconds) : Double.POSITIVE_INFINITY;
    }

    @Override
    public void setFadeEnvelope(double sourceStartSeconds, double sourceEndSeconds, double fadeDurationSeconds) {
        fadeStartSeconds = Double.isFinite(sourceStartSeconds) ? Math.max(0.0, sourceStartSeconds) : 0.0;
        fadeEndSeconds = Double.isFinite(sourceEndSeconds)
                ? Math.max(fadeStartSeconds, sourceEndSeconds) : Double.POSITIVE_INFINITY;
        this.fadeDurationSeconds = Double.isFinite(fadeDurationSeconds)
                ? Math.max(0.0, Math.min(fadeDurationSeconds, (fadeEndSeconds - fadeStartSeconds) * 0.25)) : 0.0;
    }

    @Override
    public void setOnPlaybackFinished(Consumer<Path> callback) {
        onPlaybackFinished = callback == null ? ignored -> { } : callback;
    }

    @Override
    public double playbackRate() {
        return playbackRate;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean playing() {
        return playing;
    }

    @Override
    public double currentPositionSeconds() {
        SourceDataLine current = line;
        if (current != null) {
            double position = playbackStartOffsetSeconds + Math.max(0.0, current.getMicrosecondPosition() / 1_000_000.0) * activeLinePlaybackRate;
            lastKnownPositionSeconds = position;
            return position;
        }
        return Math.max(0.0, lastKnownPositionSeconds);
    }

    public Path currentFile() {
        return currentFile;
    }

    @Override
    public String statusLabel() {
        return lastStatus;
    }

    private void streamAudio(AudioInputStream stream, SourceDataLine opened, AudioFormat outputFormat, double rate, long generation) {
        byte[] buffer = new byte[Math.max(4096, outputFormat.getFrameSize() * 2048)];
        opened.start();
        boolean reachedNaturalEnd = false;
        boolean notifyFinished = false;
        Path completedFile = null;
        try (stream) {
            int read;
            while (isCurrentPlayback(opened, generation) && (read = stream.read(buffer, 0, buffer.length)) >= 0) {
                waitIfPaused(opened, generation);
                if (!isCurrentPlayback(opened, generation)) {
                    break;
                }
                int writable = bytesBeforeCutoff(read, outputFormat, rate);
                if (writable <= 0) {
                    break;
                }
                double bufferStart = currentPositionSeconds();
                applyPcm16Gain(buffer, writable, volume, outputFormat, rate, bufferStart,
                        fadeStartSeconds, fadeEndSeconds, fadeDurationSeconds);
                opened.write(buffer, 0, writable);
                if (writable < read) {
                    break;
                }
            }
            reachedNaturalEnd = isCurrentPlayback(opened, generation);
            if (reachedNaturalEnd) {
                opened.drain();
            }
        } catch (IOException ex) {
            if (isCurrentPlayback(opened, generation)) {
                lastStatus = "El reproductor interno se interrumpió: " + ex.getMessage();
            }
        } finally {
            synchronized (lock) {
                if (line == opened && playbackGeneration == generation) {
                    lastKnownPositionSeconds = playbackStartOffsetSeconds
                            + Math.max(0.0, opened.getMicrosecondPosition() / 1_000_000.0) * rate;
                    opened.stop();
                    opened.flush();
                    opened.close();
                    completedFile = currentFile;
                    line = null;
                    currentFile = null;
                    playing = false;
                    notifyFinished = reachedNaturalEnd && !stopRequested && !lastStatus.startsWith("El reproductor interno");
                    if (notifyFinished) {
                        lastStatus = "Fragmento reproducido a " + rateLabel(rate) + ".";
                    }
                } else {
                    try {
                        opened.stop();
                        opened.flush();
                        opened.close();
                    } catch (RuntimeException ignored) {
                        // The previous audio line may already be closed by an explicit stop or fragment jump.
                    }
                }
            }
            if (notifyFinished) {
                onPlaybackFinished.accept(completedFile);
            }
        }
    }

    private boolean isCurrentPlayback(SourceDataLine opened, long generation) {
        return !stopRequested && playbackGeneration == generation && line == opened;
    }

    private void waitIfPaused(SourceDataLine opened, long generation) {
        synchronized (lock) {
            while (paused && isCurrentPlayback(opened, generation)) {
                try {
                    lock.wait(150L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    stopRequested = true;
                    return;
                }
            }
        }
    }

    private static AudioInputStream playbackStream(AudioInputStream sourceStream, AudioFormat sourceFormat, double rate) throws IOException {
        double normalized = normalizeRate(rate);
        if (normalized <= 1.01) {
            return sourceStream;
        }
        byte[] original = sourceStream.readAllBytes();
        sourceStream.close();
        byte[] processed = PcmTimeStretchProcessor.speedUpPreservePitch(original, sourceFormat, normalized);
        long frames = sourceFormat.getFrameSize() <= 0 ? processed.length : processed.length / sourceFormat.getFrameSize();
        return new AudioInputStream(new ByteArrayInputStream(processed), sourceFormat, frames);
    }

    private static AudioInputStream playableStream(Path target) throws UnsupportedAudioFileException, IOException {
        AudioInputStream source = AudioSystem.getAudioInputStream(target.toFile());
        AudioFormat format = source.getFormat();
        if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && format.getSampleSizeInBits() == 16 && !format.isBigEndian()) {
            return source;
        }
        AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(), 16, Math.max(1, format.getChannels()),
                Math.max(1, format.getChannels()) * 2, format.getSampleRate(), false);
        try {
            return AudioSystem.getAudioInputStream(decoded, source);
        } catch (IllegalArgumentException ex) {
            source.close();
            throw new UnsupportedAudioFileException("Formato no compatible con reproducción interna: " + formatLabel(format));
        }
    }

    private static void skipFrames(AudioInputStream stream, long frames) throws IOException {
        if (frames <= 0 || stream.getFormat().getFrameSize() <= 0) {
            return;
        }
        long bytes = frames * stream.getFormat().getFrameSize();
        while (bytes > 0) {
            long skipped = stream.skip(bytes);
            if (skipped <= 0) {
                return;
            }
            bytes -= skipped;
        }
    }

    private int bytesBeforeCutoff(int requested, AudioFormat format, double rate) {
        if (!Double.isFinite(stopAtSeconds) || format.getFrameSize() <= 0 || format.getFrameRate() <= 0.0f) {
            return requested;
        }
        double remainingSourceSeconds = stopAtSeconds - currentPositionSeconds();
        if (remainingSourceSeconds <= 0.0) return 0;
        long frames = (long) Math.ceil((remainingSourceSeconds / Math.max(1.0, rate)) * format.getFrameRate());
        long bytes = frames * format.getFrameSize();
        return (int) Math.max(0L, Math.min(requested, bytes - (bytes % format.getFrameSize())));
    }

    private static void applyPcm16Gain(byte[] data, int length, double volume, AudioFormat format,
                                       double rate, double bufferStartSeconds,
                                       double fadeStart, double fadeEnd, double fadeDuration) {
        if (data == null || format.getFrameSize() <= 0 || format.getFrameRate() <= 0.0f) return;
        int frameSize = format.getFrameSize();
        int channels = Math.max(1, format.getChannels());
        boolean bigEndian = format.isBigEndian();
        for (int frame = 0; frame * frameSize < length; frame++) {
            double position = bufferStartSeconds + frame * Math.max(1.0, rate) / format.getFrameRate();
            double gain = volume * fadeGain(position, fadeStart, fadeEnd, fadeDuration);
            for (int channel = 0; channel < channels; channel++) {
                int i = frame * frameSize + channel * 2;
                if (i + 1 >= length) break;
                int lowIndex = bigEndian ? i + 1 : i;
                int highIndex = bigEndian ? i : i + 1;
                short sample = (short) (((data[highIndex] & 0xff) << 8) | (data[lowIndex] & 0xff));
                short scaled = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
                data[lowIndex] = (byte) (scaled & 0xff);
                data[highIndex] = (byte) ((scaled >>> 8) & 0xff);
            }
        }
    }

    private static double fadeGain(double position, double start, double end, double duration) {
        if (duration <= 0.0) return 1.0;
        double fadeIn = Math.max(0.0, Math.min(1.0, (position - start) / duration));
        double fadeOut = Math.max(0.0, Math.min(1.0, (end - position) / duration));
        return Math.min(fadeIn, fadeOut);
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

    private static String rateLabel(double rate) {
        double normalized = normalizeRate(rate);
        return normalized == 1.75 ? "1.75x" : normalized == 1.5 ? "1.5x" : "1x";
    }

    private static String speedModeLabel(double rate) {
        return normalizeRate(rate) > 1.01 ? " · tono natural" : "";
    }

    private static String formatLabel(AudioFormat format) {
        if (format == null) {
            return "formato desconocido";
        }
        return Math.round(format.getSampleRate()) + " Hz, " + format.getSampleSizeInBits()
                + " bits, " + format.getChannels() + " canal(es), " + format.getEncoding();
    }
}
