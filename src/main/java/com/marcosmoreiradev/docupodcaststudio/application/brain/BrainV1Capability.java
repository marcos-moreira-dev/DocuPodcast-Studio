package com.marcosmoreiradev.docupodcaststudio.application.brain;

import java.util.Objects;

/** One explicit capability or non-capability of the frozen V1 brain. */
public record BrainV1Capability(
        String id,
        BrainV1CapabilityArea area,
        String title,
        BrainV1CapabilityStatus status,
        String userContract,
        String limitation,
        String evidence
) {
    public BrainV1Capability {
        id = requireText(id, "id");
        area = Objects.requireNonNull(area, "area");
        title = requireText(title, "title");
        status = Objects.requireNonNull(status, "status");
        userContract = requireText(userContract, "userContract");
        limitation = requireText(limitation, "limitation");
        evidence = requireText(evidence, "evidence");
    }

    public boolean availableInV1() {
        return status.isAvailableInV1();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
