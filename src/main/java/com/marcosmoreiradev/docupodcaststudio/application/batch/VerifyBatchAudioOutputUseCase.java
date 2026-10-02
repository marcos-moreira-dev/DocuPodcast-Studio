package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Only reuses audio whose successful final export was recorded and has not changed. */
public final class VerifyBatchAudioOutputUseCase {
    public boolean verify(Path audio, AudioExportFormat format) {
        try {
            return validTarget(audio, format) && Files.isRegularFile(receipt(audio))
                    && Files.readString(receipt(audio)).equals(format.name() + "\n" + digest(audio));
        } catch (IOException ex) { return false; }
    }

    /** Called after the audio exporter completes successfully, never for partial output. */
    public void recordCompleted(Path audio, AudioExportFormat format) throws IOException {
        if (!validTarget(audio, format)) throw new IOException("La exportación no produjo audio válido: " + audio);
        Path receipt = receipt(audio);
        Path temporary = receipt.resolveSibling(receipt.getFileName() + ".tmp");
        Files.writeString(temporary, format.name() + "\n" + digest(audio));
        Files.move(temporary, receipt, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean validTarget(Path audio, AudioExportFormat format) throws IOException {
        return audio != null && format != null && audio.toString().endsWith(format.extension())
                && Files.isRegularFile(audio) && Files.size(audio) > 44;
    }

    private static Path receipt(Path audio) { return audio.resolveSibling(audio.getFileName() + ".completed.sha256"); }

    private static String digest(Path audio) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(audio)) {
                byte[] buffer = new byte[65536];
                for (int count; (count = stream.read(buffer)) != -1;) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
