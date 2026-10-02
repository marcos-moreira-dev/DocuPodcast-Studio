package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationLifecycleCoordinatorTest {
    @Test void closesInPhasesAndIsIdempotent() {
        ArrayList<String> events = new ArrayList<>();
        LifecycleParticipant first = participant("first", events);
        LifecycleParticipant second = participant("second", events);
        ApplicationLifecycleCoordinator coordinator = new ApplicationLifecycleCoordinator(
                Duration.ofSeconds(1), List.of(first, second));

        coordinator.close();
        coordinator.close();

        assertEquals(List.of(
                "first-stop", "second-stop", "first-cancel", "second-cancel",
                "first-await", "second-await", "first-persist", "second-persist",
                "second-close", "first-close"), events);
    }

    private static LifecycleParticipant participant(String name, List<String> events) {
        return new LifecycleParticipant() {
            @Override public void stopAcceptingWork() { events.add(name + "-stop"); }
            @Override public void requestCancellation() { events.add(name + "-cancel"); }
            @Override public boolean awaitTermination(Duration remaining) { events.add(name + "-await"); return true; }
            @Override public void persistState() { events.add(name + "-persist"); }
            @Override public void close() { events.add(name + "-close"); }
        };
    }
}
