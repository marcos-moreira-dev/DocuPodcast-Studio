package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes validated UTF-8 JSON without exposing a partially written canonical file. */
final class AtomicUtf8JsonFileWriter {
    void write(Path target, String json) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null) throw new IOException("JSON target has no parent: " + target);
        Files.createDirectories(parent);
        Path temporary = parent.resolve(normalized.getFileName() + ".tmp");
        byte[] bytes = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            String validationCopy = Files.readString(
                    temporary, StandardCharsets.UTF_8);
            try {
                SimpleJsonParser.parse(validationCopy);
            } catch (RuntimeException ex) {
                throw new IOException(
                        "El JSON temporal no superó la validación.", ex);
            }
            try {
                Files.move(temporary, normalized,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, normalized,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Never replace the canonical write failure with cleanup noise.
            }
        }
    }
}
