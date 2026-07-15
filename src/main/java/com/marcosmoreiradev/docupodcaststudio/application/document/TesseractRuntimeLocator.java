package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Locates a managed or configured Tesseract CLI without binding UI code to infrastructure. */
public final class TesseractRuntimeLocator {
    private final boolean includeSystemInstallations;

    public TesseractRuntimeLocator() {
        this(true);
    }

    TesseractRuntimeLocator(boolean includeSystemInstallations) {
        this.includeSystemInstallations = includeSystemInstallations;
    }

    public TesseractToolDiscovery locate(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(applicationRoot);
        ArrayList<String> diagnostics = new ArrayList<>();
        for (Path candidate : managedCandidates(paths)) {
            diagnostics.add("Candidato portable: " + candidate);
            if (Files.isRegularFile(candidate)) {
                return new TesseractToolDiscovery(candidate.toString(), candidate, true,
                        "managed-local", paths.tesseractRoot(), diagnostics);
            }
        }
        Path configured = configuredExecutable(current.ocr().tesseractExecutable(), paths);
        if (configured != null && Files.isRegularFile(configured)) {
            diagnostics.add("Tesseract configurado: " + configured);
            return new TesseractToolDiscovery(configured.toString(), configured, true,
                    "settings", configured.getParent(), diagnostics);
        }
        diagnostics.add("Tesseract configurado: " + (configured == null ? "no" : configured));
        if (includeSystemInstallations) {
            for (Path candidate : commonWindowsCandidates()) {
                diagnostics.add("Candidato instalado: " + candidate);
                if (Files.isRegularFile(candidate)) {
                    return new TesseractToolDiscovery(candidate.toString(), candidate, true,
                            "windows-install", candidate.getParent(), diagnostics);
                }
            }
            Path pathCandidate = pathCandidate();
            if (pathCandidate != null) {
                diagnostics.add("Tesseract en PATH: " + pathCandidate);
                return new TesseractToolDiscovery(pathCandidate.toString(), pathCandidate, true,
                        "path", pathCandidate.getParent(), diagnostics);
            }
        } else {
            diagnostics.add("Busqueda de instalaciones del sistema omitida.");
        }
        diagnostics.add("No se encontro Tesseract portable, configurado, instalado ni en PATH.");
        return new TesseractToolDiscovery("tesseract", null, false, "path-fallback",
                paths.tesseractRoot(), diagnostics);
    }

    public TesseractLanguageDiscovery inspectLanguages(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return inspectLanguages(locate(current, applicationRoot), current.ocr().languages());
    }

    public TesseractLanguageDiscovery inspectLanguages(TesseractToolDiscovery discovery, String languages) {
        List<String> requested = requestedLanguages(languages);
        List<Path> directories = tessdataCandidates(discovery);
        ArrayList<String> missing = new ArrayList<>();
        for (String language : requested) {
            boolean found = directories.stream()
                    .anyMatch(directory -> Files.isRegularFile(directory.resolve(language + ".traineddata")));
            if (!found) {
                missing.add(language);
            }
        }
        return new TesseractLanguageDiscovery(requested, missing, directories);
    }

    private static Path configuredExecutable(String value, RuntimeArtifactPaths paths) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return paths.resolveConfiguredPath(value.strip());
        } catch (RuntimeException ex) {
            return Path.of(value.strip());
        }
    }

    private static List<Path> managedCandidates(RuntimeArtifactPaths paths) {
        return List.of(
                paths.tesseractExecutable(),
                paths.tesseractBinDirectory().resolve("tesseract.exe").normalize(),
                paths.tesseractRoot().resolve("tesseract.exe").normalize());
    }

    private static List<Path> commonWindowsCandidates() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return List.of();
        }
        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("C:\\Program Files\\Tesseract-OCR\\tesseract.exe"));
        candidates.add(Path.of("C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            candidates.add(Path.of(localAppData, "Programs", "Tesseract-OCR", "tesseract.exe"));
        }
        return candidates;
    }

    private static Path pathCandidate() {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] names = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? new String[] {"tesseract.exe", "tesseract.cmd", "tesseract.bat"}
                : new String[] {"tesseract"};
        for (String entry : path.split(java.io.File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path directory;
            try {
                directory = Path.of(entry.strip());
            } catch (RuntimeException ex) {
                continue;
            }
            for (String name : names) {
                Path candidate = directory.resolve(name).normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static List<String> requestedLanguages(String languages) {
        String raw = languages == null || languages.isBlank()
                ? OperationalSettings.OcrSettings.DEFAULT_LANGUAGES
                : languages.strip();
        ArrayList<String> values = new ArrayList<>();
        for (String token : raw.split("[+,;\\s]+")) {
            String language = token == null ? "" : token.strip().toLowerCase(Locale.ROOT);
            if (!language.isBlank() && !values.contains(language)) {
                values.add(language);
            }
        }
        return values.isEmpty() ? List.of("spa", "eng") : values;
    }

    private static List<Path> tessdataCandidates(TesseractToolDiscovery discovery) {
        LinkedHashSet<Path> directories = new LinkedHashSet<>();
        if (discovery != null) {
            addTessdataCandidates(directories, discovery.expectedFolder());
            Path executable = discovery.executable();
            if (executable != null) {
                addTessdataCandidates(directories, executable.getParent());
                Path parent = executable.getParent() == null ? null : executable.getParent().getParent();
                addTessdataCandidates(directories, parent);
            }
        }
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix != null && !tessdataPrefix.isBlank()) {
            try {
                addTessdataCandidates(directories, Path.of(tessdataPrefix.strip()));
            } catch (RuntimeException ignored) {
                // Invalid environment paths are exposed as missing language data by the readiness result.
            }
        }
        return directories.stream().toList();
    }

    private static void addTessdataCandidates(Set<Path> directories, Path base) {
        if (base == null) {
            return;
        }
        directories.add(base.resolve("tessdata").normalize());
        if (base.getFileName() != null && base.getFileName().toString().equalsIgnoreCase("tessdata")) {
            directories.add(base.normalize());
        }
    }
}
