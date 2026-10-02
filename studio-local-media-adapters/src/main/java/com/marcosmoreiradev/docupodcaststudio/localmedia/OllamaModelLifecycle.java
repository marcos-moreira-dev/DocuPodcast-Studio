package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ModelResidencyKey;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Short critical-section lifecycle state for one managed Ollama runtime.
 * It never performs HTTP or process I/O and is independent from resource admission.
 */
final class OllamaModelLifecycle {
    private final Map<String, RequestSnapshot> activeRequests = new LinkedHashMap<>();
    private final Map<String, ResidencyOwner> residencyOwners = new LinkedHashMap<>();
    private final Set<ModelResidencyKey> residentModels = new LinkedHashSet<>();
    private final Set<ModelResidencyKey> pendingUnloads = new LinkedHashSet<>();
    private final Set<ModelResidencyKey> unloadsInProgress = new LinkedHashSet<>();
    private ComputePreference activePreference;
    private ComputePreference pendingPreference;
    private long runtimeGeneration;
    private boolean shuttingDown;

    synchronized BeginResult tryBeginRequest(String requestId,
                                              ModelResidencyKey model,
                                              ComputePreference preference) {
        String id = normalized(requestId, "request");
        Objects.requireNonNull(model, "model");
        ComputePreference requested = Objects.requireNonNullElseGet(
                preference, ComputePreference::automatic);
        if (shuttingDown) return BeginResult.shuttingDown();
        if (unloadsInProgress.contains(model)) return BeginResult.waitForSafePoint();
        if (activePreference != null && !activePreference.equals(requested)) {
            pendingPreference = requested;
            return activeRequests.isEmpty()
                    ? BeginResult.restartRequired(requested)
                    : BeginResult.waitForSafePoint();
        }
        if (activeRequests.containsKey(id)) {
            throw new IllegalStateException("duplicate Ollama request id: " + id);
        }
        RequestSnapshot snapshot = new RequestSnapshot(id, model, requested,
                runtimeGeneration);
        activeRequests.put(id, snapshot);
        return BeginResult.acquired(snapshot);
    }

    synchronized void runtimeStarted(ComputePreference preference) {
        if (!activeRequests.isEmpty()) {
            throw new IllegalStateException("cannot replace Ollama runtime while requests are active");
        }
        activePreference = Objects.requireNonNullElseGet(
                preference, ComputePreference::automatic);
        pendingPreference = null;
        residentModels.clear();
        pendingUnloads.clear();
        unloadsInProgress.clear();
        runtimeGeneration++;
        notifyAll();
    }

    synchronized void confirmModelLoaded(ModelResidencyKey model) {
        residentModels.add(Objects.requireNonNull(model));
        pendingUnloads.remove(model);
    }

    synchronized Action finishRequest(RequestSnapshot snapshot,
                                      boolean unloadWhenIdle) {
        if (snapshot == null) return Action.none();
        RequestSnapshot active = activeRequests.get(snapshot.requestId());
        if (active == snapshot) activeRequests.remove(snapshot.requestId());
        if (unloadWhenIdle) pendingUnloads.add(snapshot.model());
        Action action = nextUnloadAction(snapshot.model());
        notifyAll();
        return action;
    }

    synchronized void retainResidency(String ownerId, ModelResidencyKey model) {
        String id = normalized(ownerId, "owner");
        ResidencyOwner current = residencyOwners.get(id);
        if (current != null && !current.model().equals(model)) {
            throw new IllegalStateException("residency owner cannot change model identity");
        }
        residencyOwners.put(id, current == null
                ? new ResidencyOwner(model, 1)
                : new ResidencyOwner(model, current.depth() + 1));
        pendingUnloads.remove(model);
    }

    synchronized Action releaseResidency(String ownerId, boolean unloadWhenIdle) {
        String id = normalized(ownerId, "owner");
        ResidencyOwner current = residencyOwners.get(id);
        if (current == null) return Action.none();
        if (current.depth() > 1) {
            residencyOwners.put(id, new ResidencyOwner(
                    current.model(), current.depth() - 1));
            return Action.none();
        }
        residencyOwners.remove(id);
        if (unloadWhenIdle) pendingUnloads.add(current.model());
        Action action = nextUnloadAction(current.model());
        notifyAll();
        return action;
    }

    synchronized Action requestUnload(ModelResidencyKey model) {
        pendingUnloads.add(Objects.requireNonNull(model));
        return nextUnloadAction(model);
    }

    synchronized void completeUnload(ModelResidencyKey model, boolean succeeded) {
        unloadsInProgress.remove(model);
        if (succeeded) {
            residentModels.remove(model);
            pendingUnloads.remove(model);
        } else {
            pendingUnloads.add(model);
        }
        notifyAll();
    }

    synchronized Set<ModelResidencyKey> runtimeDied() {
        Set<ModelResidencyKey> invalidated = Set.copyOf(residentModels);
        residentModels.clear();
        pendingUnloads.clear();
        unloadsInProgress.clear();
        activePreference = null;
        runtimeGeneration++;
        notifyAll();
        return invalidated;
    }

    synchronized void beginShutdown() {
        shuttingDown = true;
        notifyAll();
    }

    synchronized boolean requestRuntimeValid(RequestSnapshot request) {
        return request != null && request.runtimeGeneration() == runtimeGeneration
                && activeRequests.get(request.requestId()) == request;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(runtimeGeneration, activePreference, pendingPreference,
                activeRequests.size(), residencyOwners.size(),
                Set.copyOf(residentModels), Set.copyOf(pendingUnloads),
                shuttingDown);
    }

    synchronized void awaitStateChange(long milliseconds) throws InterruptedException {
        wait(Math.max(1L, milliseconds));
    }

    private Action nextUnloadAction(ModelResidencyKey model) {
        if (!pendingUnloads.contains(model)
                || unloadsInProgress.contains(model)
                || activeRequests.values().stream().anyMatch(
                        request -> request.model().equals(model))
                || residencyOwners.values().stream().anyMatch(
                        owner -> owner.model().equals(model))) {
            return Action.none();
        }
        if (!residentModels.contains(model)) {
            pendingUnloads.remove(model);
            return Action.none();
        }
        unloadsInProgress.add(model);
        return Action.unload(model);
    }

    record RequestSnapshot(String requestId, ModelResidencyKey model,
                           ComputePreference preference,
                           long runtimeGeneration) { }

    record Snapshot(long runtimeGeneration, ComputePreference activePreference,
                    ComputePreference pendingPreference, int activeRequests,
                    int residencyOwners, Set<ModelResidencyKey> residentModels,
                    Set<ModelResidencyKey> pendingUnloads,
                    boolean shuttingDown) { }

    record BeginResult(BeginStatus status, RequestSnapshot request,
                       ComputePreference restartPreference) {
        static BeginResult acquired(RequestSnapshot request) {
            return new BeginResult(BeginStatus.ACQUIRED, request, null);
        }
        static BeginResult waitForSafePoint() {
            return new BeginResult(BeginStatus.WAIT, null, null);
        }
        static BeginResult restartRequired(ComputePreference preference) {
            return new BeginResult(BeginStatus.RESTART_REQUIRED, null, preference);
        }
        static BeginResult shuttingDown() {
            return new BeginResult(BeginStatus.SHUTTING_DOWN, null, null);
        }
    }

    enum BeginStatus { ACQUIRED, WAIT, RESTART_REQUIRED, SHUTTING_DOWN }

    record Action(ActionType type, ModelResidencyKey model) {
        static Action none() { return new Action(ActionType.NONE, null); }
        static Action unload(ModelResidencyKey model) {
            return new Action(ActionType.UNLOAD_MODEL, model);
        }
    }

    enum ActionType { NONE, UNLOAD_MODEL }

    private record ResidencyOwner(ModelResidencyKey model, int depth) { }

    private static String normalized(String value, String fallback) {
        String safe = value == null ? "" : value.strip();
        return safe.isBlank() ? fallback : safe;
    }
}
