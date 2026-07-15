package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Stores local recent project shortcuts in a small user-private text file. */
public final class RecentProjectsStore {
    private static final int LIMIT = 8;
    private static final Pattern MODE_PATTERN = Pattern.compile("\"mode\"\\s*:\\s*\"([^\"]+)\"");
    private final Path storeFile;
    private final ProjectTypeResolver projectTypeResolver;

    public RecentProjectsStore() {
        this(defaultStoreFile(), RecentProjectsStore::resolveProjectTypeFallback);
    }

    public RecentProjectsStore(ProjectTypeResolver projectTypeResolver) {
        this(defaultStoreFile(), projectTypeResolver);
    }

    RecentProjectsStore(Path storeFile) {
        this(storeFile, RecentProjectsStore::resolveProjectTypeFallback);
    }

    RecentProjectsStore(Path storeFile, ProjectTypeResolver projectTypeResolver) {
        this.storeFile = storeFile == null ? defaultStoreFile() : storeFile.toAbsolutePath().normalize();
        this.projectTypeResolver = projectTypeResolver == null
                ? RecentProjectsStore::resolveProjectTypeFallback
                : projectTypeResolver;
    }

    @FunctionalInterface
    public interface ProjectTypeResolver {
        String resolve(Path projectFile, String storedType);
    }

    public List<RecentProjectEntry> load() {
        if (!Files.isRegularFile(storeFile)) {
            return List.of();
        }
        try {
            List<String> storedLines = Files.readAllLines(storeFile, StandardCharsets.UTF_8);
            List<RecentProjectEntry> entries = new ArrayList<>();
            for (String line : storedLines) {
                parse(line).ifPresent(entries::add);
            }
            List<RecentProjectEntry> existing = entries.stream()
                    .filter(entry -> Files.isRegularFile(entry.projectFile()))
                    .limit(LIMIT)
                    .toList();
            List<String> resolvedLines = existing.stream().map(RecentProjectsStore::format).toList();
            if (!storedLines.equals(resolvedLines)) {
                persistLimited(existing);
            }
            return List.copyOf(existing);
        } catch (IOException ex) {
            return List.of();
        }
    }

    public List<RecentProjectEntry> remember(Path projectFile, String displayName) {
        return remember(projectFile, displayName, "");
    }

    public List<RecentProjectEntry> remember(Path projectFile, String displayName, String projectType) {
        if (projectFile == null) {
            return load();
        }
        Path normalized = projectFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            return load();
        }
        Map<Path, RecentProjectEntry> ordered = new LinkedHashMap<>();
        ordered.put(normalized, new RecentProjectEntry(displayName, normalized, resolveProjectType(normalized, projectType)));
        for (RecentProjectEntry entry : load()) {
            ordered.putIfAbsent(entry.projectFile(), entry);
        }
        return persistLimited(ordered.values().stream().limit(LIMIT).toList());
    }

    public List<RecentProjectEntry> forget(Path projectFile) {
        if (projectFile == null) {
            return load();
        }
        Path normalized = projectFile.toAbsolutePath().normalize();
        List<RecentProjectEntry> kept = load().stream()
                .filter(entry -> !entry.projectFile().equals(normalized))
                .toList();
        return persistLimited(kept);
    }

    Path storeFile() {
        return storeFile;
    }

    private List<RecentProjectEntry> persistLimited(List<RecentProjectEntry> entries) {
        List<RecentProjectEntry> limited = entries.stream().limit(LIMIT).toList();
        try {
            Path parent = storeFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(storeFile, limited.stream().map(RecentProjectsStore::format).toList(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Recent-project shortcuts are convenience state; failing to persist must not block project work.
        }
        return List.copyOf(limited);
    }

    private Optional<RecentProjectEntry> parse(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String[] parts = line.split("\t", 3);
        String displayName = parts.length >= 2 ? parts[0] : "";
        String storedType = parts.length >= 3 ? parts[1] : "";
        String pathText = parts.length >= 3 ? parts[2] : parts.length >= 2 ? parts[1] : parts[0];
        if (pathText.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(pathText).toAbsolutePath().normalize();
        return Optional.of(new RecentProjectEntry(displayName, path, resolveProjectType(path, storedType)));
    }

    private static String format(RecentProjectEntry entry) {
        return sanitize(entry.displayName()) + "\t" + sanitize(entry.projectType()) + "\t" + entry.projectFile();
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').strip();
    }

    private static Path defaultStoreFile() {
        return Path.of(System.getProperty("user.home", "."), ".docupodcast-studio", "recent-projects.txt");
    }

    private String resolveProjectType(Path projectFile, String storedType) {
        try {
            String resolved = projectTypeResolver.resolve(projectFile, storedType);
            if (resolved != null && !resolved.isBlank()) {
                return resolved.strip();
            }
        } catch (RuntimeException ignored) {
            // Fall through to the lightweight presentation fallback.
        }
        return resolveProjectTypeFallback(projectFile, storedType);
    }

    private static String resolveProjectTypeFallback(Path projectFile, String storedType) {
        String fallback = storedType == null ? "" : storedType.strip();
        if (projectFile != null && Files.isRegularFile(projectFile)) {
            try {
                String json = Files.readString(projectFile, StandardCharsets.UTF_8);
                java.util.regex.Matcher matcher = MODE_PATTERN.matcher(json);
                if (matcher.find()) {
                    return ProjectMode.valueOf(matcher.group(1)).displayName();
                }
            } catch (IOException | IllegalArgumentException ignored) {
                // Recent project type is presentation-only metadata; failure falls back to the stored label.
            }
        }
        return fallback;
    }

}
