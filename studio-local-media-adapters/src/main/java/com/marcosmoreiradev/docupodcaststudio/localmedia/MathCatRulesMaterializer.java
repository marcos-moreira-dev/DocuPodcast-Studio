package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Publishes the bundled MathCAT rules into the managed runtime without network access. */
final class MathCatRulesMaterializer {
    private static final String RESOURCE = "/mathcat/mathcat-rules.zip";
    private final Path componentRoot;

    MathCatRulesMaterializer(Path runtimeRoot) {
        componentRoot = runtimeRoot.toAbsolutePath().normalize()
                .resolve("tools/document-ai/mathcat");
    }

    synchronized Path ensureRules() throws IOException {
        Path rules = componentRoot.resolve("MathCAT/Rules");
        if (valid(rules)) return rules;
        if (Files.exists(componentRoot)) {
            throw new IOException("La copia integrada de las reglas MathCAT está incompleta. "
                    + "Repara la instalación de DocuPodcast Studio.");
        }
        Path staging = componentRoot.resolveSibling(
                componentRoot.getFileName() + ".staging-" + UUID.randomUUID());
        try {
            Files.createDirectories(staging);
            try (InputStream resource = MathCatRulesMaterializer.class.getResourceAsStream(RESOURCE)) {
                if (resource == null) {
                    throw new IOException("El paquete de reglas MathCAT no está incluido en la aplicación.");
                }
                extract(resource, staging);
            }
            Path stagedRules = staging.resolve("MathCAT/Rules");
            if (!valid(stagedRules)) {
                throw new IOException("El paquete integrado de reglas MathCAT no superó la validación.");
            }
            Files.createDirectories(componentRoot.getParent());
            promote(staging, componentRoot);
            return componentRoot.resolve("MathCAT/Rules");
        } catch (IOException failure) {
            deleteTree(staging);
            throw failure;
        }
    }

    Path rulesIfValid() {
        Path rules = componentRoot.resolve("MathCAT/Rules");
        return valid(rules) ? rules : null;
    }

    private static boolean valid(Path rules) {
        return Files.isRegularFile(rules.resolve("Languages/es/SimpleSpeak_Rules.yaml"))
                && Files.isRegularFile(rules.resolve("Languages/en/SimpleSpeak_Rules.yaml"))
                && Files.isRegularFile(rules.resolve("Intent/general.yaml"));
    }

    private static void extract(InputStream input, Path staging) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = staging.resolve(entry.getName()).normalize();
                if (!target.startsWith(staging)) {
                    throw new IOException("El paquete MathCAT contiene una ruta insegura.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private static void promote(Path staging, Path target) throws IOException {
        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(staging, target);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best effort cleanup of unpublished staging only.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup of unpublished staging only.
        }
    }
}
