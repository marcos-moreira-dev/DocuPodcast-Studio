package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** One monotonic deadline shared by every blocking phase of an operation. */
public final class OperationDeadline {
    private static final OperationDeadline NONE = new OperationDeadline(
            Long.MAX_VALUE, System::nanoTime, false);

    private final long deadlineNanos;
    private final LongSupplier clock;
    private final boolean bounded;

    private OperationDeadline(long deadlineNanos, LongSupplier clock,
                              boolean bounded) {
        this.deadlineNanos = deadlineNanos;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.bounded = bounded;
    }

    public static OperationDeadline none() { return NONE; }

    public static OperationDeadline after(Duration timeout) {
        return after(timeout, System::nanoTime);
    }

    public static OperationDeadline after(Duration timeout, LongSupplier clock) {
        Duration safe = Objects.requireNonNull(timeout, "timeout");
        if (safe.isNegative() || safe.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        LongSupplier ticker = Objects.requireNonNull(clock, "clock");
        long now = ticker.getAsLong();
        long delta;
        try {
            delta = safe.toNanos();
        } catch (ArithmeticException overflow) {
            delta = Long.MAX_VALUE;
        }
        long target = delta == Long.MAX_VALUE || now > Long.MAX_VALUE - delta
                ? Long.MAX_VALUE : now + delta;
        return new OperationDeadline(target, ticker, true);
    }

    public boolean bounded() { return bounded; }

    public long remainingNanos() {
        if (!bounded) return Long.MAX_VALUE;
        return Math.max(0L, deadlineNanos - clock.getAsLong());
    }

    public long remainingMillis() {
        if (!bounded) return Long.MAX_VALUE;
        long nanos = remainingNanos();
        if (nanos == 0L) return 0L;
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(nanos));
    }

    public Duration remainingOr(Duration nominal) {
        Duration safe = Objects.requireNonNull(nominal, "nominal");
        if (!bounded) return safe;
        long remaining = remainingNanos();
        if (remaining <= 0L) return Duration.ZERO;
        return Duration.ofNanos(Math.min(remaining, safe.toNanos()));
    }

    public boolean expired() { return bounded && remainingNanos() == 0L; }
}
