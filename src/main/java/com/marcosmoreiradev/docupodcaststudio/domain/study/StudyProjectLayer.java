package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Project-side study data. It never mutates the imported source document. */
public record StudyProjectLayer(
        List<TechnicalProblem> technicalProblems,
        DocumentStudyVideoConfiguration documentaryVideoConfiguration
) {
    public StudyProjectLayer {
        technicalProblems = technicalProblems == null ? List.of() : technicalProblems.stream()
                .map(problem -> Objects.requireNonNull(problem, "problem"))
                .toList();
        documentaryVideoConfiguration = Objects.requireNonNullElseGet(
                documentaryVideoConfiguration, DocumentStudyVideoConfiguration::empty);
    }

    public StudyProjectLayer(List<TechnicalProblem> technicalProblems) {
        this(technicalProblems, DocumentStudyVideoConfiguration.empty());
    }

    public static StudyProjectLayer empty() {
        return new StudyProjectLayer(List.of(), DocumentStudyVideoConfiguration.empty());
    }

    public StudyProjectLayer withTechnicalProblem(TechnicalProblem problem) {
        Objects.requireNonNull(problem, "problem");
        ArrayList<TechnicalProblem> updated = new ArrayList<>(technicalProblems);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).id().equals(problem.id())) {
                updated.set(i, problem);
                return new StudyProjectLayer(updated, documentaryVideoConfiguration);
            }
        }
        updated.add(problem);
        return new StudyProjectLayer(updated, documentaryVideoConfiguration);
    }

    public StudyProjectLayer withoutTechnicalProblem(String problemId) {
        String normalized = problemId == null ? "" : problemId.strip();
        if (normalized.isBlank()) {
            return this;
        }
        return new StudyProjectLayer(technicalProblems.stream()
                .filter(problem -> !problem.id().equals(normalized))
                .toList(), documentaryVideoConfiguration);
    }

    public StudyProjectLayer withDocumentaryVideoConfiguration(DocumentStudyVideoConfiguration configuration) {
        return new StudyProjectLayer(technicalProblems,
                Objects.requireNonNullElseGet(configuration, DocumentStudyVideoConfiguration::empty));
    }
}
