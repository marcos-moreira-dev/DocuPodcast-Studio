package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Declarative local lifecycle actions shared by bundled adapters. */
final class ReadinessEngineAdministration implements EngineAdministration {
    private static final EngineActionDescriptor INSTALL = action(EngineActionId.INSTALL,
            "Instalar recurso", "Descarga un recurso a una ruta local mediante staging recuperable.", true, List.of(
                    field("sourceUrl", "URL de origen", ConfigurationFieldType.URL, true),
                    field("target", "Archivo de destino", ConfigurationFieldType.FILE, true)));
    private static final EngineActionDescriptor IMPORT = action(EngineActionId.IMPORT,
            "Importar recurso", "Copia un archivo o directorio local dentro del layout del runtime.", true, List.of(
                    field("source", "Origen", ConfigurationFieldType.FILE, true),
                    field("target", "Destino", ConfigurationFieldType.FILE, true)));
    private static final EngineActionDescriptor START = action(EngineActionId.START,
            "Iniciar runtime", "Inicia un proceso local administrado por esta sesión.", false, List.of(
                    field("command", "Ejecutable", ConfigurationFieldType.FILE, true),
                    field("arguments", "Argumentos separados por |", ConfigurationFieldType.TEXT, false),
                    field("workingDirectory", "Directorio de trabajo", ConfigurationFieldType.DIRECTORY, false)));
    private static final EngineActionDescriptor STOP = action(EngineActionId.STOP,
            "Detener runtime", "Detiene el proceso iniciado por esta sesión.", true, List.of());
    private static final EngineActionDescriptor REPAIR = action(EngineActionId.REPAIR,
            "Reparar", "Limpia staging incompleto y vuelve a comprobar la disponibilidad.", true, List.of(
                    field("stagingDirectory", "Directorio de staging", ConfigurationFieldType.DIRECTORY, false)));
    private static final EngineActionDescriptor SMOKE = action(EngineActionId.SMOKE_TEST,
            "Probar motor", "Comprueba recursos y disponibilidad sin generar contenido.", false, List.of());

    private final MediaEngine engine;
    private volatile Process managedProcess;

    private ReadinessEngineAdministration(MediaEngine engine) { this.engine = engine; }

    static ReadinessEngineAdministration forEngine(MediaEngine engine) {
        return new ReadinessEngineAdministration(engine);
    }

    @Override public EngineId engineId() { return engine.descriptor().id(); }

    @Override public List<EngineActionDescriptor> actions() {
        return List.of(INSTALL, IMPORT, START, STOP, REPAIR, SMOKE);
    }

    @Override public synchronized EngineActionResult execute(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (request == null) throw new IllegalArgumentException("engine action request is required");
        ExecutionContext current = context == null ? ExecutionContext.defaults("engine-administration") : context;
        current.cancellation().throwIfCancellationRequested();
        if (EngineActionId.INSTALL.equals(request.actionId())) install(request.inputs(), current);
        else if (EngineActionId.IMPORT.equals(request.actionId())) importResource(request.inputs(), current);
        else if (EngineActionId.START.equals(request.actionId())) start(request.inputs(), current);
        else if (EngineActionId.STOP.equals(request.actionId())) stop(current);
        else if (EngineActionId.REPAIR.equals(request.actionId())) repair(request.inputs(), current);
        else if (!EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            throw new IllegalArgumentException("Acción administrativa no publicada por " + engineId());
        }
        current.cancellation().throwIfCancellationRequested();
        current.progress().report("CHECKING", 0.9, "Comprobando disponibilidad del motor.");
        EngineReadiness readiness = engine.inspectReadiness(null);
        current.progress().report("COMPLETED", 1.0, readiness.summary());
        boolean success = readiness.state() == ReadinessState.READY;
        return new EngineActionResult(success, readiness.summary(), readiness, List.of(), Map.of(
                "engineId", engineId().value(), "actionId", request.actionId().value()));
    }

    private void install(Map<String, String> inputs, ExecutionContext context) throws IOException, InterruptedException {
        URI source = URI.create(required(inputs, "sourceUrl"));
        Path target = Path.of(required(inputs, "target")).toAbsolutePath().normalize();
        if (target.getParent() == null) throw new IOException("El destino necesita un directorio.");
        Files.createDirectories(target.getParent());
        Path partial = target.resolveSibling(target.getFileName() + ".partial");
        context.progress().report("DOWNLOADING", 0.05, "Descargando recurso recuperable.");
        HttpRequest request = HttpRequest.newBuilder(source).timeout(Duration.ofHours(2)).GET().build();
        HttpResponse<InputStream> response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .build().send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("La descarga respondió HTTP " + response.statusCode() + ".");
        }
        long expected = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        try (InputStream input = response.body(); var output = Files.newOutputStream(partial)) {
            byte[] buffer = new byte[128 * 1024];
            long copied = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                context.cancellation().throwIfCancellationRequested();
                output.write(buffer, 0, read);
                copied += read;
                double ratio = expected > 0 ? Math.min(0.85, 0.05 + (copied / (double) expected) * 0.8) : 0.4;
                context.progress().report("DOWNLOADING", ratio, "Descargados " + copied + " bytes.");
            }
        }
        promote(partial, target);
    }

    private void importResource(Map<String, String> inputs, ExecutionContext context)
            throws IOException, InterruptedException {
        Path source = Path.of(required(inputs, "source")).toAbsolutePath().normalize();
        Path target = Path.of(required(inputs, "target")).toAbsolutePath().normalize();
        if (!Files.exists(source)) throw new IOException("No existe el recurso de origen: " + source);
        context.progress().report("IMPORTING", 0.1, "Importando recurso local.");
        if (Files.isDirectory(source)) copyDirectory(source, target, context);
        else {
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            context.cancellation().throwIfCancellationRequested();
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void start(Map<String, String> inputs, ExecutionContext context) throws IOException {
        if (managedProcess != null && managedProcess.isAlive()) throw new IOException("El runtime ya está iniciado.");
        ArrayList<String> command = new ArrayList<>();
        command.add(required(inputs, "command"));
        String arguments = inputs.getOrDefault("arguments", "").strip();
        if (!arguments.isBlank()) for (String argument : arguments.split("\\|", -1)) if (!argument.isBlank()) command.add(argument.strip());
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        String work = inputs.getOrDefault("workingDirectory", "").strip();
        if (!work.isBlank()) builder.directory(Path.of(work).toAbsolutePath().normalize().toFile());
        context.progress().report("STARTING", 0.4, "Iniciando runtime local.");
        managedProcess = builder.start();
    }

    private void stop(ExecutionContext context) throws InterruptedException {
        Process process = managedProcess;
        if (process == null || !process.isAlive()) return;
        context.progress().report("STOPPING", 0.4, "Deteniendo runtime local.");
        process.destroy();
        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly();
        managedProcess = null;
    }

    private void repair(Map<String, String> inputs, ExecutionContext context) throws IOException, InterruptedException {
        String value = inputs.getOrDefault("stagingDirectory", "").strip();
        if (value.isBlank()) return;
        Path staging = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isDirectory(staging) || !staging.getFileName().toString().contains("staging")) return;
        context.progress().report("REPAIRING", 0.4, "Limpiando staging incompleto.");
        try (var paths = Files.walk(staging)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                context.cancellation().throwIfCancellationRequested();
                Files.deleteIfExists(path);
            }
        }
    }

    private static void copyDirectory(Path source, Path target, ExecutionContext context)
            throws IOException, InterruptedException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                context.cancellation().throwIfCancellationRequested();
                Path destination = target.resolve(source.relativize(path)).normalize();
                if (!destination.startsWith(target)) throw new IOException("Ruta de importación inválida.");
                if (Files.isDirectory(path)) Files.createDirectories(destination);
                else Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void promote(Path partial, Path target) throws IOException {
        try { Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) {
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String required(Map<String, String> inputs, String key) {
        String value = inputs == null ? "" : inputs.getOrDefault(key, "").strip();
        if (value.isBlank()) throw new IllegalArgumentException("Falta el campo " + key + ".");
        return value;
    }

    private static EngineActionDescriptor action(EngineActionId id, String name, String description,
                                                 boolean confirmation, List<EngineConfigurationField> inputs) {
        return new EngineActionDescriptor(id, name, description, inputs, confirmation, 0L, Map.of());
    }

    private static EngineConfigurationField field(String key, String label, ConfigurationFieldType type,
                                                   boolean required) {
        return new EngineConfigurationField(key, label, "", type, required, "");
    }
}
