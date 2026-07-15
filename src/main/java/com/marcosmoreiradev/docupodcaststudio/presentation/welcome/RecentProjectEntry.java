package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import java.nio.file.Path;

public record RecentProjectEntry(String displayName, Path projectFile, String projectType) {
    public RecentProjectEntry(String displayName, Path projectFile) {
        this(displayName, projectFile, "");
    }

    public RecentProjectEntry {
        projectFile = projectFile == null ? Path.of("") : projectFile.toAbsolutePath().normalize();
        String fallback = projectFile.getFileName() == null ? "Proyecto DocuPodcast" : projectFile.getFileName().toString();
        displayName = displayName == null || displayName.isBlank() ? cleanProjectName(fallback) : displayName.strip();
        projectType = projectType == null ? "" : projectType.strip();
    }

    public String folderLabel() {
        Path parent = projectFile.getParent();
        return parent == null ? projectFile.toString() : parent.toString();
    }

    public String typeLabel() {
        return projectType.isBlank() ? "Proyecto DocuPodcast" : projectType;
    }

    private static String cleanProjectName(String fileName) {
        String value = fileName == null ? "" : fileName.strip();
        if (value.endsWith(".docupodcast.json")) {
            value = value.substring(0, value.length() - ".docupodcast.json".length());
        }
        return value.isBlank() ? "Proyecto DocuPodcast" : value;
    }
}
