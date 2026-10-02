package com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage;

import java.util.Objects;
import java.util.Optional;

/** One explicit decision in an incremental theatre refresh. */
public record TheatrePackageDelta(
        TheatrePackageDeltaStatus status,
        TheatrePackageEntry before,
        TheatrePackageEntry after,
        String reason
) {
    public TheatrePackageDelta {
        status = Objects.requireNonNull(status, "status");
        reason = Objects.requireNonNullElse(reason, "").strip();
        if (before == null && after == null) throw new IllegalArgumentException("A delta needs before or after");
    }

    public String logicalId() {
        return after != null ? after.logicalId() : before.logicalId();
    }

    public Optional<TheatrePackageEntry> previous() { return Optional.ofNullable(before); }
    public Optional<TheatrePackageEntry> current() { return Optional.ofNullable(after); }
}
