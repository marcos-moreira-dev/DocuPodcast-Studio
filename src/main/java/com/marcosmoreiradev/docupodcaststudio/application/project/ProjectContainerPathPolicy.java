package com.marcosmoreiradev.docupodcaststudio.application.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves user-facing save targets into the canonical DocuPodcast project layout.
 *
 * <p>A DocuPodcast project is intentionally a folder, not a single loose file:
 * the folder contains the .docupodcast.json descriptor and all derived folders
 * such as source/, document/, script/, storyboard/, assets/, jobs/ and exports/.
 * This avoids scattering project resources directly on the Desktop or any other
 * directory selected by the user.</p>
 */
public final class ProjectContainerPathPolicy {
    public static final String PROJECT_EXTENSION = ".docupodcast.json";
    private static final String DEFAULT_PROJECT_NAME = "Proyecto DocuPodcast";

    /**
     * Converts a Save As selection into a contained project file.
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code Desktop/Obra.docupodcast.json -> Desktop/Obra/Obra.docupodcast.json}</li>
     *     <li>{@code Desktop/Obra -> Desktop/Obra/Obra.docupodcast.json}</li>
     *     <li>{@code Desktop/Obra/Obra.docupodcast.json -> Desktop/Obra/Obra.docupodcast.json}</li>
     * </ul>
     */
    public Path resolveSaveAsTarget(Path selectedPath) {
        Objects.requireNonNull(selectedPath, "selectedPath");
        Path normalized = selectedPath.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) {
            String folderName = safeProjectName(fileName(normalized));
            return normalized.resolve(folderName + PROJECT_EXTENSION).normalize();
        }

        Path projectFileCandidate = ensureProjectExtension(normalized);
        String baseName = projectBaseName(projectFileCandidate);
        String folderName = safeProjectName(baseName);
        Path parent = projectFileCandidate.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
        }
        if (sameName(parent.getFileName(), folderName) && sameName(projectFileCandidate.getFileName(), folderName + PROJECT_EXTENSION)) {
            return projectFileCandidate;
        }
        return parent.resolve(folderName).resolve(folderName + PROJECT_EXTENSION).normalize();
    }

    /** Returns the folder-safe project name used for a container directory and descriptor file. */
    public String sanitizeProjectName(String value) {
        return safeProjectName(value);
    }

    public Path projectRoot(Path projectFile) {
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) {
            return Path.of(".").toAbsolutePath().normalize();
        }
        return root;
    }

    public Path ensureProjectExtension(Path selectedPath) {
        Objects.requireNonNull(selectedPath, "selectedPath");
        String text = selectedPath.toString();
        if (text.toLowerCase(Locale.ROOT).endsWith(PROJECT_EXTENSION)) {
            return selectedPath;
        }
        return Path.of(text + PROJECT_EXTENSION);
    }

    public String projectBaseName(Path projectFile) {
        Objects.requireNonNull(projectFile, "projectFile");
        String name = fileName(projectFile);
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(PROJECT_EXTENSION)) {
            return name.substring(0, name.length() - PROJECT_EXTENSION.length());
        }
        return name;
    }

    private static String fileName(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return DEFAULT_PROJECT_NAME;
        }
        String value = fileName.toString().trim();
        return value.isBlank() ? DEFAULT_PROJECT_NAME : value;
    }

    private static String safeProjectName(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).trim();
        if (normalized.isBlank()) {
            return DEFAULT_PROJECT_NAME;
        }
        String cleaned = normalized
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? DEFAULT_PROJECT_NAME : cleaned;
    }

    private static boolean sameName(Path pathName, String expected) {
        if (pathName == null) {
            return false;
        }
        return pathName.toString().equalsIgnoreCase(expected);
    }
}
