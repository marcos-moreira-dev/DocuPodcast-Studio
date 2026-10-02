package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Repairs the location-dependent metadata of a managed Python environment. */
final class ManagedPythonEnvironmentRepair {
    private ManagedPythonEnvironmentRepair() {
    }

    static String repair(Path runtimeRoot, Path environmentPython, String componentName)
            throws IOException {
        Path root = runtimeRoot.toAbsolutePath().normalize();
        Path python = environmentPython.toAbsolutePath().normalize();
        if (!python.startsWith(root)) {
            throw new IOException("El Python administrado escapa del runtime actual: " + python);
        }
        Path scripts = python.getParent();
        Path environment = scripts == null ? null : scripts.getParent();
        Path configuration = environment == null ? null : environment.resolve("pyvenv.cfg");
        if (configuration == null || !Files.isRegularFile(configuration)) {
            return "";
        }

        String current = Files.readString(configuration, StandardCharsets.UTF_8);
        String configuredHome = property(current, "home");
        if (configuredHome.isBlank()) {
            return "";
        }
        Path previousHome;
        try {
            previousHome = Path.of(configuredHome).toAbsolutePath().normalize();
        } catch (RuntimeException invalidPath) {
            previousHome = Path.of("");
        }
        if (previousHome.startsWith(root) && Files.isDirectory(previousHome)) {
            return "";
        }

        Path managedHome = findManagedPythonHome(root);
        if (managedHome == null) {
            throw new IOException("El entorno Python de " + componentName + " fue trasladado desde "
                    + configuredHome + " y no existe un Python administrado compatible en "
                    + root.resolve("tools/python") + ".");
        }
        String repaired = replaceProperty(current, "home", managedHome.toString());
        if (!writeAtomicallyWhenChanged(configuration, repaired)) {
            return "";
        }
        return "Se reparó el entorno Python de " + componentName + " para usar " + managedHome + ".";
    }

    private static Path findManagedPythonHome(Path root) throws IOException {
        Path pythonRoot = root.resolve("tools/python").normalize();
        if (!Files.isDirectory(pythonRoot)) {
            return null;
        }
        try (var candidates = Files.walk(pythonRoot, 5)) {
            return candidates
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("python.exe"))
                    .map(Path::getParent)
                    .filter(path -> path != null && path.startsWith(root))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static String property(String content, String key) {
        for (String line : content.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator > 0 && line.substring(0, separator).strip().equalsIgnoreCase(key)) {
                return line.substring(separator + 1).strip();
            }
        }
        return "";
    }

    private static String replaceProperty(String content, String key, String value) {
        StringBuilder result = new StringBuilder();
        boolean replaced = false;
        for (String line : content.split("\\R", -1)) {
            int separator = line.indexOf('=');
            if (separator > 0 && line.substring(0, separator).strip().equalsIgnoreCase(key)) {
                result.append(key).append(" = ").append(value);
                replaced = true;
            } else {
                result.append(line);
            }
            result.append(System.lineSeparator());
        }
        if (!replaced) {
            result.append(key).append(" = ").append(value).append(System.lineSeparator());
        }
        return result.toString();
    }

    private static boolean writeAtomicallyWhenChanged(Path target, String content) throws IOException {
        byte[] expected = content.getBytes(StandardCharsets.UTF_8);
        if (Files.isRegularFile(target) && java.util.Arrays.equals(Files.readAllBytes(target), expected)) {
            return false;
        }
        Path staged = target.resolveSibling(target.getFileName() + ".staging");
        Files.write(staged, expected);
        try {
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
        return true;
    }
}
