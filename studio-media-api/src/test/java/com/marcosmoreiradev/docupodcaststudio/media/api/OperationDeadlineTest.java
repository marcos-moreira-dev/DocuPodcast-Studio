package com.marcosmoreiradev.docupodcaststudio.media.api;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

final class OperationDeadlineTest {
    @Test void capsEveryPhaseToTheSameMonotonicDeadline() {
        AtomicLong clock = new AtomicLong(1_000L);
        OperationDeadline deadline = OperationDeadline.after(
                Duration.ofNanos(500L), clock::get);
        assertEquals(Duration.ofNanos(500L),
                deadline.remainingOr(Duration.ofMinutes(3)));
        clock.addAndGet(460L);
        assertEquals(Duration.ofNanos(40L),
                deadline.remainingOr(Duration.ofMinutes(3)));
        clock.addAndGet(40L);
        assertTrue(deadline.expired());
        assertEquals(Duration.ZERO,
                deadline.remainingOr(Duration.ofMinutes(3)));
    }
}
