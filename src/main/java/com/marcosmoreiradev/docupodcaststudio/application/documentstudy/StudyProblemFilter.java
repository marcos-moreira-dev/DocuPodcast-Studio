package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

/** Read-only filter used to project saved technical problems in the study SideDock. */
public record StudyProblemFilter(
        String query,
        Integer sourcePageFrom,
        Integer sourcePageTo,
        String blockQuery,
        StudyProblemStatusFilter status
) {
    public StudyProblemFilter {
        query = normalize(query);
        sourcePageFrom = positiveOrNull(sourcePageFrom);
        sourcePageTo = positiveOrNull(sourcePageTo);
        if (sourcePageFrom != null && sourcePageTo != null && sourcePageFrom > sourcePageTo) {
            int from = sourcePageTo;
            sourcePageTo = sourcePageFrom;
            sourcePageFrom = from;
        }
        blockQuery = normalize(blockQuery);
        status = status == null ? StudyProblemStatusFilter.ALL : status;
    }

    public static StudyProblemFilter empty() {
        return new StudyProblemFilter("", null, null, "", StudyProblemStatusFilter.ALL);
    }

    public boolean emptyFilter() {
        return query.isBlank()
                && sourcePageFrom == null
                && sourcePageTo == null
                && blockQuery.isBlank()
                && status == StudyProblemStatusFilter.ALL;
    }

    private static Integer positiveOrNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }
}
