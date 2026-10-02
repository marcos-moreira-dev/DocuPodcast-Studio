package com.marcosmoreiradev.docupodcaststudio.application.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cheap deterministic guard used before reusing or accepting a batch MP4. */
public final class VerifyBatchVideoOutputUseCase {
    public boolean verify(Path candidate) {
        if (candidate == null) return false;
        try {
            if (!Files.isRegularFile(candidate) || Files.size(candidate) < 32L) return false;
            byte[] header = new byte[64];
            int read;
            try (var input = Files.newInputStream(candidate)) {
                read = input.read(header);
            }
            if (read < 12) return false;
            String signature = new String(header, 0, read, StandardCharsets.ISO_8859_1);
            return signature.indexOf("ftyp", 4) >= 4;
        } catch (IOException unreadable) {
            return false;
        }
    }
}
