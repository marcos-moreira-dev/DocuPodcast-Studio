package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Saved technical-problems projection for the document study SideDock. */
public record StudyProblemsProjection(
        List<StudyProblemListItem> problems,
        Map<String, StudyProblemDetail> detailsById,
        int totalProblemCount
) {
    public StudyProblemsProjection(List<StudyProblemListItem> problems, Map<String, StudyProblemDetail> detailsById) {
        this(problems, detailsById, problems == null ? 0 : problems.size());
    }

    public StudyProblemsProjection {
        problems = problems == null ? List.of() : List.copyOf(problems);
        detailsById = detailsById == null ? Map.of() : Map.copyOf(detailsById);
        totalProblemCount = Math.max(0, totalProblemCount);
    }

    public static StudyProblemsProjection empty() {
        return new StudyProblemsProjection(List.of(), Map.of(), 0);
    }

    public Optional<StudyProblemDetail> detail(String problemId) {
        if (problemId == null || problemId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(detailsById.get(problemId.strip()));
    }

    static StudyProblemsProjection ordered(List<StudyProblemListItem> problems, List<StudyProblemDetail> details) {
        return ordered(problems, details, problems == null ? 0 : problems.size());
    }

    static StudyProblemsProjection ordered(List<StudyProblemListItem> problems, List<StudyProblemDetail> details, int totalProblemCount) {
        LinkedHashMap<String, StudyProblemDetail> byId = new LinkedHashMap<>();
        for (StudyProblemDetail detail : details) {
            byId.put(detail.id(), detail);
        }
        return new StudyProblemsProjection(problems, byId, totalProblemCount);
    }
}
