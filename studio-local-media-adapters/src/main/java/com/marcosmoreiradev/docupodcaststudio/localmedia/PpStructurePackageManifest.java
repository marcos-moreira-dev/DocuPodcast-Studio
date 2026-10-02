package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verifies a relocatable, offline PP-Structure package before publication. */
final class PpStructurePackageManifest {
    static final String FILE_NAME = "package-manifest.json";
    static final String PYTHON_VERSION = "3.12.11";
    static final String PADDLE_VERSION = "3.3.0";
    static final String PADDLE_OCR_VERSION = "3.7.0";
    private static final Pattern FILE_ENTRY = Pattern.compile(
            "\\{\\s*\"path\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"size\"\\s*:\\s*(\\d+)\\s*,\\s*"
                    + "\"sha256\"\\s*:\\s*\"([0-9a-fA-F]{64})\"\\s*}",
            Pattern.DOTALL);
    private static final List<String> REQUIRED_MODEL_KEYS = List.of(
            "layout_detection_model_dir",
            "text_detection_model_dir",
            "text_recognition_model_dir",
            "table_classification_model_dir",
            "wired_table_structure_recognition_model_dir",
            "wireless_table_structure_recognition_model_dir",
            "wired_table_cells_detection_model_dir",
            "wireless_table_cells_detection_model_dir",
            "table_orientation_classify_model_dir",
            "formula_recognition_model_dir");

    private PpStructurePackageManifest() { }

    static Verification verify(Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path manifest = normalizedRoot.resolve(FILE_NAME);
        if (!Files.isRegularFile(manifest)) {
            return new Verification(false, List.of("manifest-missing"), "", "", "", "");
        }
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        ArrayList<String> issues = new ArrayList<>();
        String python = property(json, "pythonVersion");
        String paddle = property(json, "paddleVersion");
        String ocr = property(json, "paddleOcrVersion");
        String profile = property(json, "profile").toLowerCase(Locale.ROOT);
        String license = property(json, "license");
        if (!PYTHON_VERSION.equals(python)) issues.add("python-version:" + python);
        if (!PADDLE_VERSION.equals(paddle)) issues.add("paddle-version:" + paddle);
        if (!PADDLE_OCR_VERSION.equals(ocr)) issues.add("paddleocr-version:" + ocr);
        if (license.isBlank()) issues.add("license-missing");
        if (!Pattern.compile("\"sources\"\\s*:\\s*\\[\\s*\"[^\"]+\"",
                Pattern.DOTALL).matcher(json).find()) {
            issues.add("sources-missing");
        }
        if (!List.of("cpu", "gpu-cu118").contains(profile)) {
            issues.add("profile-invalid:" + profile);
        }
        Matcher matcher = FILE_ENTRY.matcher(json);
        int entries = 0;
        Set<String> declaredPaths = new HashSet<>();
        while (matcher.find()) {
            entries++;
            String relativeText = matcher.group(1).replace('\\', '/');
            declaredPaths.add(relativeText);
            Path relative;
            try {
                relative = Path.of(relativeText);
            } catch (RuntimeException invalid) {
                issues.add("path-invalid:" + relativeText);
                continue;
            }
            if (relative.isAbsolute() || relativeText.contains("../")) {
                issues.add("path-not-relocatable:" + relativeText);
                continue;
            }
            Path file = normalizedRoot.resolve(relative).normalize();
            if (!file.startsWith(normalizedRoot) || !Files.isRegularFile(file)) {
                issues.add("file-missing:" + relativeText);
                continue;
            }
            long expectedSize = Long.parseLong(matcher.group(2));
            if (Files.size(file) != expectedSize) {
                issues.add("size-mismatch:" + relativeText);
                continue;
            }
            String expectedHash = matcher.group(3).toLowerCase(Locale.ROOT);
            if (!expectedHash.equals(ManagedDownloadPreflightInspector.sha256(file))) {
                issues.add("sha256-mismatch:" + relativeText);
            }
        }
        if (entries == 0) issues.add("files-empty");
        for (String required : List.of("python/python.exe", "bridge/pp_structure_v3.py",
                "models/manifest.json")) {
            if (!json.replace('\\', '/').contains("\"path\":\"" + required + "\"")
                    && !json.replace('\\', '/').contains("\"path\": \"" + required + "\"")) {
                issues.add("required-entry-missing:" + required);
            }
        }
        try (var files = Files.walk(normalizedRoot)) {
            files.filter(Files::isRegularFile)
                    .map(normalizedRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> !FILE_NAME.equals(path))
                    .filter(path -> !declaredPaths.contains(path))
                    .forEach(path -> issues.add("undeclared-file:" + path));
        }
        verifyModelInventory(normalizedRoot, issues);
        if (Files.exists(normalizedRoot.resolve(".venv"))
                || Files.exists(normalizedRoot.resolve("pyvenv.cfg"))) {
            issues.add("non-portable-virtualenv");
        }
        return new Verification(issues.isEmpty(), List.copyOf(issues),
                python, paddle, ocr, profile);
    }

    private static void verifyModelInventory(Path root, List<String> issues)
            throws IOException {
        Path manifest = root.resolve("models/manifest.json");
        if (!Files.isRegularFile(manifest)) return;
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        for (String key : REQUIRED_MODEL_KEYS) {
            String relative = property(json, key).replace('\\', '/');
            if (relative.isBlank()) {
                issues.add("model-entry-missing:" + key);
                continue;
            }
            Path path;
            try {
                path = Path.of(relative);
            } catch (RuntimeException invalid) {
                issues.add("model-path-invalid:" + key);
                continue;
            }
            Path resolved = root.resolve("models").resolve(path).normalize();
            if (path.isAbsolute() || !resolved.startsWith(root.resolve("models"))
                    || !Files.isDirectory(resolved)) {
                issues.add("model-directory-invalid:" + key);
            }
        }
    }

    private static String property(String json, String name) {
        return OllamaJson.stringProperty(json, name).strip();
    }

    record Verification(boolean valid, List<String> issues, String pythonVersion,
                        String paddleVersion, String paddleOcrVersion, String profile) { }
}
