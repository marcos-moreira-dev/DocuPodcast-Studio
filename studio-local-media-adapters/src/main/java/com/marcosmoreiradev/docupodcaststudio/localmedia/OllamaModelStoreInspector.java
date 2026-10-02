package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** Validates Ollama's content-addressed local model manifest and every referenced blob. */
final class OllamaModelStoreInspector {
    private static final Pattern DIGEST_ENTRY = Pattern.compile(
            "\"digest\"\\s*:\\s*\"sha256:([0-9a-fA-F]{64})\""
                    + "[^{}]*?\"size\"\\s*:\\s*(\\d+)",
            Pattern.DOTALL);

    private OllamaModelStoreInspector() { }

    static Inspection inspect(Path modelsRoot, String model) throws IOException {
        return inspect(modelsRoot, model, true);
    }

    static Inspection inspectFast(Path modelsRoot, String model) throws IOException {
        return inspect(modelsRoot, model, false);
    }

    private static Inspection inspect(Path modelsRoot, String model,
                                      boolean verifyContentHash) throws IOException {
        Path manifest = manifest(modelsRoot, model);
        if (!Files.isRegularFile(manifest)) return new Inspection(false, 0L, "", List.of("manifest-missing"));
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        var matcher = DIGEST_ENTRY.matcher(json);
        ArrayList<String> issues = new ArrayList<>();
        long bytes = 0L;
        int found = 0;
        while (matcher.find()) {
            found++;
            String digest = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            long expectedSize;
            try {
                expectedSize = Long.parseLong(matcher.group(2));
            } catch (NumberFormatException invalid) {
                issues.add("blob-size-invalid:" + digest);
                continue;
            }
            Path blob = modelsRoot.resolve("blobs").resolve("sha256-" + digest).normalize();
            if (!blob.startsWith(modelsRoot.toAbsolutePath().normalize()) || !Files.isRegularFile(blob)) {
                issues.add("blob-missing:" + digest);
                continue;
            }
            long actualSize = Files.size(blob);
            bytes += actualSize;
            if (actualSize != expectedSize) {
                issues.add("blob-size-mismatch:" + digest);
            } else if (verifyContentHash && !digest.equals(sha256(blob))) {
                issues.add("blob-corrupt:" + digest);
            }
        }
        if (found == 0) issues.add("manifest-without-digests");
        return new Inspection(issues.isEmpty(), bytes, sha256(manifest), List.copyOf(issues));
    }

    static Path manifest(Path modelsRoot, String model) {
        String[] parts = model.split(":", 2);
        String name = parts[0];
        String tag = parts.length == 2 ? parts[1] : "latest";
        return modelsRoot.toAbsolutePath().normalize().resolve("manifests")
                .resolve("registry.ollama.ai").resolve("library").resolve(name).resolve(tag).normalize();
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    record Inspection(boolean valid, long bytes, String manifestSha256, List<String> issues) { }
}
