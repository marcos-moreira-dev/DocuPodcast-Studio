package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreRefreshPlan;

import java.util.List;
import java.util.Objects;

public record TheatreRefreshPreflightReport(
        TheatreRefreshPlan plan,
        List<TheatreRefreshDiagnostic> diagnostics
) {
    public TheatreRefreshPreflightReport {
        plan = Objects.requireNonNull(plan, "plan");
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public boolean canCommit() {
        return !plan.hasConflicts() && diagnostics.stream()
                .noneMatch(item -> item.severity() == TheatreRefreshDiagnosticSeverity.ERROR);
    }

    public long errors() { return count(TheatreRefreshDiagnosticSeverity.ERROR); }
    public long warnings() { return count(TheatreRefreshDiagnosticSeverity.WARNING); }
    public long newAssets() { return plan.count(TheatrePackageDeltaStatus.NEW); }
    public long modifiedAssets() { return plan.count(TheatrePackageDeltaStatus.MODIFIED); }
    public long unchangedAssets() { return plan.count(TheatrePackageDeltaStatus.UNCHANGED); }
    public long retainedMissingAssets() { return plan.count(TheatrePackageDeltaStatus.MISSING_SOURCE_RETAINED); }
    private long count(TheatreRefreshDiagnosticSeverity severity) {
        return diagnostics.stream().filter(item -> item.severity() == severity).count();
    }
}
