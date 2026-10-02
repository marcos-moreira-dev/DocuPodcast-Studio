package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Shared UTF-8 JSON writer that never exposes a partially written canonical file.
 *
 * <p>The payload is forced to disk and parsed before the final move. Callers that
 * coordinate more than one file still need a higher-level transaction, but every
 * individual pointer file receives the same durability contract.</p>
 */
public final class AtomicJsonFileWriter implements
        com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.AtomicJsonWriter {
    @Override
    public void write(Path target, String json) throws IOException {
        Objects.requireNonNull(target, "target");
        Path normalized = target.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null) {
            throw new IOException("JSON target has no parent: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, normalized.getFileName() + ".", ".tmp");
        byte[] bytes = Objects.requireNonNullElse(json, "").getBytes(StandardCharsets.UTF_8);
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            String validationCopy = Files.readString(temporary, StandardCharsets.UTF_8);
            try {
                SimpleJsonParser.parse(validationCopy);
            } catch (RuntimeException ex) {
                throw new IOException("El JSON temporal no superó la validación.", ex);
            }
            try {
                Files.move(temporary, normalized,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, normalized, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
