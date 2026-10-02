package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Common validation and honest readiness reporting for one concrete local adapter. */
abstract class AbstractLocalEngineAdministration implements EngineAdministration {
    protected final MediaEngine engine;
    protected final RuntimeAssetCatalog assets;
    protected final SafeRuntimeOperations runtime;
    private final List<EngineActionDescriptor> actions;

    AbstractLocalEngineAdministration(MediaEngine engine, RuntimeAssetCatalog assets,
                                      List<EngineActionDescriptor> actions) {
        this.engine = engine;
        this.assets = assets;
        this.runtime = new SafeRuntimeOperations(assets.root());
        this.actions = List.copyOf(actions);
    }

    @Override public final EngineId engineId() { return engine.descriptor().id(); }
    @Override public final List<EngineActionDescriptor> actions() { return actions; }

    @Override public final EngineActionResult execute(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (request == null || !engineId().equals(request.engineId())) {
            throw new IllegalArgumentException("La accion no pertenece a este motor.");
        }
        if (actions.stream().noneMatch(action -> action.id().equals(request.actionId()))) {
            throw new IllegalArgumentException("Accion no publicada por " + engineId().value() + ".");
        }
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("admin-" + engineId().value()) : context;
        current.cancellation().throwIfCancellationRequested();
        List<GenerationArtifact> artifacts = perform(request, current);
        current.cancellation().throwIfCancellationRequested();
        EngineReadiness readiness = engine.inspectReadiness(null);
        boolean success = true;
        String message = readiness.ready()
                ? readiness.summary()
                : actionCompletedMessage(request.actionId()) + " " + readiness.summary();
        current.progress().report("COMPLETED", 1.0, message);
        return new EngineActionResult(success, message, readiness, artifacts,
                Map.of("engineId", engineId().value(), "actionId", request.actionId().value(),
                        "globalReadiness", readiness.state().name()));
    }

    protected abstract List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException;

    private static String actionCompletedMessage(EngineActionId actionId) {
        if (EngineActionId.START.equals(actionId)) return "El arranque terminó correctamente.";
        if (EngineActionId.STOP.equals(actionId)) return "El proceso administrado se detuvo correctamente.";
        if (EngineActionId.INSTALL.equals(actionId)) return "La instalación terminó correctamente.";
        if (EngineActionId.REPAIR.equals(actionId)) return "La reparación terminó correctamente.";
        if (EngineActionId.SMOKE_TEST.equals(actionId)) return "La prueba terminó correctamente.";
        return "La acción terminó correctamente.";
    }

    protected Path inputPath(EngineActionRequest request, String key) {
        return Path.of(SafeRuntimeOperations.required(request.inputs().get(key), key));
    }

    protected static EngineActionDescriptor action(EngineActionId id, String name, String description,
                                                    boolean confirmation, EngineConfigurationField... inputs) {
        return new EngineActionDescriptor(id, name, description, List.of(inputs), confirmation, 0L, Map.of());
    }

    protected static EngineConfigurationField file(String key, String label, boolean required) {
        return new EngineConfigurationField(key, label, "Selecciona un recurso local.",
                ConfigurationFieldType.FILE, required, "");
    }

    protected static EngineConfigurationField directory(String key, String label, boolean required) {
        return new EngineConfigurationField(key, label, "Selecciona un directorio local.",
                ConfigurationFieldType.DIRECTORY, required, "");
    }

    protected static EngineConfigurationField url(String key, String label, boolean required) {
        return new EngineConfigurationField(key, label, "Origen HTTP/HTTPS; el destino lo decide el adaptador.",
                ConfigurationFieldType.URL, required, "");
    }
}
