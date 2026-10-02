package com.marcosmoreiradev.docupodcaststudio.media.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExecutionPolicyTest {
    @Test
    void defaultPolicyKeepsItsFiniteSafetyTimeout() {
        assertTrue(ExecutionPolicy.defaults().hasTimeout());
    }

    @Test
    void unboundedPolicyReliesOnCooperativeCancellation() {
        assertFalse(ExecutionPolicy.unbounded().hasTimeout());
    }
}
