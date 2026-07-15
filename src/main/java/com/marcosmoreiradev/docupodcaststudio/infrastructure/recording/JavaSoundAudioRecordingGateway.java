package com.marcosmoreiradev.docupodcaststudio.infrastructure.recording;

import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioRecordingGateway;
import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Records microphone audio to managed WAV files using Java Sound. */
public final class JavaSoundAudioRecordingGateway implements AudioRecordingGateway {
    private static final AudioFormat FORMAT = new AudioFormat(16_000.0f, 16, 1, true, false);
    private static final String RECORDINGS_DIR = "recordings";

    private TargetDataLine activeLine;
    private Thread writerThread;
    private Path activeOutputFile;
    private volatile IOException writerFailure;

    @Override
    public synchronized Path startRecording(Path projectDirectory, String suggestedFileName) throws IOException {
        return startRecording(projectDirectory, suggestedFileName, AudioInputDevice.DEFAULT_ID);
    }

    @Override
    public synchronized Path startRecording(Path projectDirectory, String suggestedFileName, String inputDeviceId) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        if (recording()) {
            throw new IOException("Ya existe una grabación activa");
        }
        Path output = projectDirectory.toAbsolutePath().normalize()
                .resolve(RECORDINGS_DIR)
                .resolve(safeWavFileName(suggestedFileName))
                .normalize();
        Path recordingsDir = projectDirectory.toAbsolutePath().normalize().resolve(RECORDINGS_DIR).normalize();
        if (!output.startsWith(recordingsDir)) {
            throw new IOException("Ruta de grabación inválida");
        }
        Files.createDirectories(output.getParent());
        TargetDataLine line = openMicrophoneLine(inputDeviceId);
        activeLine = line;
        activeOutputFile = output;
        writerFailure = null;
        writerThread = new Thread(() -> writeWav(line, output), "docupodcast-voice-recording");
        writerThread.setDaemon(true);
        line.start();
        writerThread.start();
        return output;
    }

    @Override
    public synchronized Path stopRecording() throws IOException {
        if (!recording()) {
            throw new IOException("No hay una grabación activa");
        }
        TargetDataLine line = activeLine;
        Thread thread = writerThread;
        Path output = activeOutputFile;
        line.stop();
        line.close();
        try {
            thread.join(5_000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido esperando cierre de grabación", exception);
        } finally {
            activeLine = null;
            writerThread = null;
            activeOutputFile = null;
        }
        if (writerFailure != null) {
            IOException failure = writerFailure;
            writerFailure = null;
            throw failure;
        }
        if (!Files.isRegularFile(output) || Files.size(output) == 0L) {
            throw new IOException("La grabación no produjo un WAV válido: " + output);
        }
        return output;
    }


    @Override
    public synchronized void cancelRecording() throws IOException {
        if (!recording()) {
            return;
        }
        Path output = activeOutputFile;
        try {
            stopRecording();
        } finally {
            if (output != null) {
                Files.deleteIfExists(output);
            }
        }
    }

    @Override
    public synchronized boolean recording() {
        return activeLine != null && activeLine.isOpen();
    }

    @Override
    public List<AudioInputDevice> inputDevices() {
        ArrayList<AudioInputDevice> devices = new ArrayList<>();
        devices.add(AudioInputDevice.systemDefault());
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
        for (int index = 0; index < mixers.length; index++) {
            Mixer mixer = AudioSystem.getMixer(mixers[index]);
            if (mixer.isLineSupported(lineInfo)) {
                String name = mixers[index].getName() == null ? "" : mixers[index].getName().strip();
                String description = mixers[index].getDescription() == null ? "" : mixers[index].getDescription().strip();
                String label = name.isBlank() ? "Entrada de audio " + (index + 1) : name;
                if (!description.isBlank() && !description.equalsIgnoreCase(name)) {
                    label = label + " - " + description;
                }
                devices.add(new AudioInputDevice("mixer:" + index, label, false));
            }
        }
        return List.copyOf(devices);
    }

    private static TargetDataLine openMicrophoneLine(String inputDeviceId) throws IOException {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            String normalized = inputDeviceId == null ? "" : inputDeviceId.strip();
            if (!normalized.isBlank() && !AudioInputDevice.DEFAULT_ID.equals(normalized)) {
                if (!normalized.startsWith("mixer:")) {
                    throw new IOException("Microfono no reconocido: " + normalized);
                }
                int index = Integer.parseInt(normalized.substring("mixer:".length()));
                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                if (index < 0 || index >= mixers.length) {
                    throw new IOException("Microfono no disponible: " + normalized);
                }
                Mixer mixer = AudioSystem.getMixer(mixers[index]);
                if (!mixer.isLineSupported(info)) {
                    throw new IOException("El microfono seleccionado no soporta WAV PCM 16 kHz mono");
                }
                TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                line.open(FORMAT);
                return line;
            }
            if (!AudioSystem.isLineSupported(info)) {
                throw new IOException("El micrófono no soporta WAV PCM 16 kHz mono");
            }
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            return line;
        } catch (LineUnavailableException | IllegalArgumentException exception) {
            throw new IOException("No se pudo abrir el micrófono para grabación", exception);
        }
    }

    private void writeWav(TargetDataLine line, Path output) {
        try (AudioInputStream stream = new AudioInputStream(line)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output.toFile());
        } catch (IOException exception) {
            writerFailure = exception;
        }
    }

    private static String safeWavFileName(String suggestedFileName) {
        String base = suggestedFileName == null ? "voice-recording.wav" : suggestedFileName.strip();
        if (base.isBlank()) {
            base = "voice-recording.wav";
        }
        String clean = base.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        if (!clean.endsWith(".wav")) {
            clean = clean + ".wav";
        }
        return clean.length() > 96 ? clean.substring(0, 92) + ".wav" : clean;
    }
}
