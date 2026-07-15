package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Builds the saved technical-problems projection without changing project schema. */
public final class BuildStudyProblemsProjectionUseCase {
    public StudyProblemsProjection build(DocuPodcastProject project, Path projectDirectory) {
        return build(project, projectDirectory, StudyProblemFilter.empty());
    }

    public StudyProblemsProjection build(DocuPodcastProject project, Path projectDirectory, StudyProblemFilter filter) {
        if (project == null || project.study().technicalProblems().isEmpty()) {
            return StudyProblemsProjection.empty();
        }
        StudyProblemFilter effectiveFilter = filter == null ? StudyProblemFilter.empty() : filter;
        List<TechnicalProblem> ordered = project.study().technicalProblems().stream()
                .sorted(Comparator.comparing(TechnicalProblem::updatedAt).reversed())
                .toList();
        List<StudyProblemDetail> allDetails = ordered.stream()
                .map(problem -> detail(project, projectDirectory, problem))
                .toList();
        List<StudyProblemDetail> details = allDetails.stream()
                .filter(detail -> matches(detail, effectiveFilter))
                .toList();
        List<StudyProblemListItem> items = details.stream()
                .map(BuildStudyProblemsProjectionUseCase::item)
                .toList();
        return StudyProblemsProjection.ordered(items, details, allDetails.size());
    }

    private static StudyProblemListItem item(StudyProblemDetail detail) {
        List<String> pages = detail.sources().stream()
                .map(StudyProblemSourceProjection::sourcePage)
                .filter(page -> !page.isBlank())
                .distinct()
                .toList();
        StudyProblemSourceProjection firstSource = detail.sources().isEmpty() ? null : detail.sources().getFirst();
        return new StudyProblemListItem(
                detail.id(),
                detail.title(),
                preview(detail.problemText()),
                detail.sources().size(),
                pages,
                firstSource == null ? "" : firstSource.blockId(),
                firstSource == null ? "" : firstSource.sourcePage(),
                preview(detail.solutionText()),
                preview(detail.notes()),
                detail.hasSolutionText(),
                detail.hasSolutionImage(),
                detail.hasSourceCrops(),
                detail.createdAt(),
                detail.updatedAt());
    }

    private static StudyProblemDetail detail(DocuPodcastProject project, Path projectDirectory, TechnicalProblem problem) {
        List<StudyProblemSourceProjection> sources = problem.sources().stream()
                .map(source -> source(project, projectDirectory, source))
                .toList();
        Path solutionImage = assetPath(project, projectDirectory, problem.solutionImageAssetId()).orElse(null);
        Path canvasState = canvasStatePath(projectDirectory, problem.id()).orElse(null);
        return new StudyProblemDetail(
                problem.id(),
                problem.title(),
                sources,
                problem.problemText(),
                problem.solutionText(),
                problem.solutionImageAssetId(),
                solutionImage,
                canvasState,
                problem.createdAt(),
                problem.updatedAt(),
                problem.notes());
    }

    private static StudyProblemSourceProjection source(DocuPodcastProject project, Path projectDirectory, StudySourceReference source) {
        Path crop = assetPath(project, projectDirectory, source.sourceCropAssetId()).orElse(null);
        return new StudyProblemSourceProjection(
                source.blockId(),
                source.selectedText(),
                source.sourcePage(),
                source.bbox(),
                source.sourceCropAssetId(),
                crop);
    }

    private static Optional<Path> assetPath(DocuPodcastProject project, Path projectDirectory, String assetId) {
        if (project == null || projectDirectory == null || assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        Optional<ProjectAssetReference> asset = project.assets().byId(assetId.strip());
        if (asset.isEmpty()) {
            return Optional.empty();
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.get().relativePath()).toAbsolutePath().normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private static Optional<Path> canvasStatePath(Path projectDirectory, String problemId) {
        if (projectDirectory == null || problemId == null || problemId.isBlank()) {
            return Optional.empty();
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path resolved = root.resolve(Path.of("study", "problems",
                problemId.strip().toLowerCase(Locale.ROOT), "solution", "canvas-state.json"))
                .toAbsolutePath().normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private static boolean matches(StudyProblemDetail detail, StudyProblemFilter filter) {
        return matchesQuery(detail, filter.query())
                && matchesBlock(detail, filter.blockQuery())
                && matchesPage(detail, filter.sourcePageFrom(), filter.sourcePageTo())
                && matchesStatus(detail, filter.status());
    }

    private static boolean matchesQuery(StudyProblemDetail detail, String query) {
        String needle = normalizedNeedle(query);
        if (needle.isBlank()) {
            return true;
        }
        StringBuilder haystack = new StringBuilder()
                .append(detail.id()).append(' ')
                .append(detail.title()).append(' ')
                .append(detail.problemText()).append(' ')
                .append(detail.solutionText()).append(' ')
                .append(detail.notes());
        for (StudyProblemSourceProjection source : detail.sources()) {
            haystack.append(' ')
                    .append(source.blockId()).append(' ')
                    .append(source.sourcePage()).append(' ')
                    .append(source.selectedText());
        }
        return normalizedNeedle(haystack.toString()).contains(needle);
    }

    private static boolean matchesBlock(StudyProblemDetail detail, String blockQuery) {
        String needle = normalizedNeedle(blockQuery);
        if (needle.isBlank()) {
            return true;
        }
        return detail.sources().stream()
                .map(StudyProblemSourceProjection::blockId)
                .map(BuildStudyProblemsProjectionUseCase::normalizedNeedle)
                .anyMatch(blockId -> blockId.contains(needle));
    }

    private static boolean matchesPage(StudyProblemDetail detail, Integer from, Integer to) {
        if (from == null && to == null) {
            return true;
        }
        return detail.sources().stream()
                .map(StudyProblemSourceProjection::sourcePage)
                .flatMap(page -> parsePositiveInt(page).stream())
                .anyMatch(page -> (from == null || page >= from) && (to == null || page <= to));
    }

    private static boolean matchesStatus(StudyProblemDetail detail, StudyProblemStatusFilter status) {
        return switch (status == null ? StudyProblemStatusFilter.ALL : status) {
            case ALL -> true;
            case UNSOLVED -> !detail.hasSolutionText() && !detail.hasSolutionImage();
            case WITH_TEXT -> detail.hasSolutionText();
            case WITH_CANVAS -> detail.hasSolutionImage();
            case WITH_CROPS -> detail.hasSourceCrops();
        };
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        if (value == null || !value.strip().matches("\\d+")) {
            return Optional.empty();
        }
        int parsed = Integer.parseInt(value.strip());
        return parsed > 0 ? Optional.of(parsed) : Optional.empty();
    }

    private static String normalizedNeedle(String value) {
        return (value == null ? "" : value)
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private static String preview(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 160).strip() + "...";
    }
}
