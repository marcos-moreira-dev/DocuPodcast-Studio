package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.io.IOException;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Duration;
import java.util.Objects;

/** Imports gated FLUX components selected by the user after accepting provider terms. */
public final class FluxComponentImportUseCase {
    private static final long T5_REQUIRED_FREE_BYTES = 11_000_000_000L;
    private final ExternalProcessRunner processRunner;

    public FluxComponentImportUseCase(ExternalProcessRunner processRunner) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    public FluxComponentImportReport importFrom(Path source, Path applicationRoot, ModelSetupProgressListener listener) {
        return importFrom(source, applicationRoot, listener, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
    }

    public FluxComponentImportReport importFrom(Path source, Path applicationRoot, ModelSetupProgressListener listener,
                                               ImageModelPackageProfile profile) {
        ModelSetupProgressListener progress = listener == null ? ModelSetupProgressListener.noop() : listener;
        if (source == null || !Files.exists(source)) {
            return new FluxComponentImportReport(false, source, List.of(), "Selecciona un archivo o carpeta FLUX existente.");
        }
        Path root = root(applicationRoot);
        Path models = root.resolve("models/image");
        ArrayList<Path> installed = new ArrayList<>();
        try {
            Files.createDirectories(models.resolve("vae"));
            Files.createDirectories(models.resolve("text_encoders"));
            if (Files.isDirectory(source) && Files.isRegularFile(source.resolve("model.safetensors.index.json"))) {
                progress.onProgress("Validando shards T5-v1.1-XXL y espacio libre...");
                Path target = models.resolve("text_encoders/t5xxl_bf16.safetensors");
                if (Files.exists(target)) throw new IOException("T5 ya existe; no se reemplazó el archivo administrado.");
                ensureFreeSpace(target, T5_REQUIRED_FREE_BYTES);
                mergeT5(source, target, root, progress);
                validateTensorContainer(target);
                installed.add(target);
            } else if (Files.isDirectory(source)) {
                importRecognizedDirectory(source, models, installed, progress);
            } else {
                importRecognizedFile(source, models, installed, progress);
            }
            if (installed.isEmpty()) {
                return new FluxComponentImportReport(false, source, List.of(),
                        "No se reconocieron componentes FLUX. Selecciona flux1-dev, ae, clip_l, T5XXL o una carpeta Diffusers text_encoder/text_encoder_2.");
            }
            FluxModelBundle bundle = profile == ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT
                    ? FluxModelBundle.inspectKontext(root) : FluxModelBundle.inspect(root);
            String suffix = bundle.ready() ? " Bundle FLUX completo." : " Aun faltan: " + String.join(", ", bundle.missingComponents()) + ".";
            return new FluxComponentImportReport(true, source, installed,
                    "Componentes FLUX importados: " + installed.size() + "." + suffix);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new FluxComponentImportReport(false, source, installed,
                    "No se pudo importar FLUX: " + ex.getMessage());
        }
    }

    private void importRecognizedDirectory(Path source, Path models, List<Path> installed,
                                                  ModelSetupProgressListener progress) throws IOException, InterruptedException {
        try (var stream = Files.walk(source, 3)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                boolean officialClipFolder = file.getParent() != null && file.getParent().getFileName() != null
                        && file.getParent().getFileName().toString().equalsIgnoreCase("text_encoder");
                if (name.equals("model.safetensors.index.json") && file.getParent().getFileName().toString().equals("text_encoder_2")) {
                    Path target = models.resolve("text_encoders/t5xxl_bf16.safetensors");
                    if (Files.exists(target)) throw new IOException("T5 ya existe; no se reemplazó el archivo administrado.");
                    ensureFreeSpace(target, T5_REQUIRED_FREE_BYTES);
                    mergeT5(file.getParent(), target, models.getParent().getParent(), progress);
                    validateTensorContainer(target);
                    installed.add(target);
                } else if (name.equals("model.safetensors") && officialClipFolder) {
                    Path target = models.resolve("text_encoders/clip_l.safetensors");
                    install(file, target, progress);
                    installed.add(target);
                } else if (recognizedFlatName(name)) {
                    importRecognizedFile(file, models, installed, progress);
                }
            }
        }
    }

    private static void importRecognizedFile(Path source, Path models, List<Path> installed,
                                             ModelSetupProgressListener progress) throws IOException {
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        Path target;
        if (name.equals("flux1-dev.safetensors") || name.equals("flux1-kontext-dev.safetensors")) {
            target = models.resolve(name);
        } else if (name.equals("ae.safetensors")) {
            target = models.resolve("vae/ae.safetensors");
        } else if (name.equals("clip_l.safetensors") || name.equals("model.safetensors")) {
            target = models.resolve("text_encoders/clip_l.safetensors");
        } else if (name.startsWith("t5xxl") && name.endsWith(".safetensors")) {
            target = models.resolve("text_encoders").resolve(source.getFileName());
        } else {
            return;
        }
        install(source, target, progress);
        installed.add(target);
    }

    private static boolean recognizedFlatName(String name) {
        return name.equals("flux1-dev.safetensors") || name.equals("flux1-kontext-dev.safetensors") || name.equals("ae.safetensors")
                || name.equals("clip_l.safetensors") || name.startsWith("t5xxl");
    }

    private static void install(Path source, Path target, ModelSetupProgressListener progress) throws IOException {
        validateTensorContainer(source);
        Files.createDirectories(target.getParent());
        if (Files.exists(target) && Files.isSameFile(source, target)) {
            return;
        }
        progress.onProgress("Importando " + source.getFileName() + "...");
        if (Files.exists(target)) {
            if (Files.size(source) == Files.size(target) && Files.mismatch(source, target) == -1) return;
            throw new IOException("Ya existe " + target.getFileName() + ". No se reemplazó el recurso existente.");
        }
        Path staging = Files.createTempFile(target.getParent(), "flux-import-", ".part");
        try {
            Files.copy(source, staging, StandardCopyOption.REPLACE_EXISTING);
            Files.move(staging, target);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    /** Bounded structural check; not a model identity/hash verification or inference test. */
    public static void validateTensorContainer(Path source) throws IOException {
        long size = Files.size(source);
        try (var input = Files.newInputStream(source)) {
            byte[] prefix = input.readNBytes(8);
            if (prefix.length != 8) throw new IOException("Archivo incompleto: " + source.getFileName());
            long headerSize = java.nio.ByteBuffer.wrap(prefix).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
            if (headerSize < 2 || headerSize > 8_000_000 || headerSize >= size - 8)
                throw new IOException("Cabecera de modelo inválida: " + source.getFileName());
            String header = new String(input.readNBytes((int) headerSize), java.nio.charset.StandardCharsets.UTF_8).strip();
            if (!header.startsWith("{") || !header.endsWith("}"))
                throw new IOException("Cabecera de modelo inválida: " + source.getFileName());
            var offsets = java.util.regex.Pattern.compile("\\\"data_offsets\\\"\\s*:\\s*\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\]").matcher(header);
            boolean tensor = false;
            while (offsets.find()) {
                tensor = true;
                long start = Long.parseLong(offsets.group(1));
                long end = Long.parseLong(offsets.group(2));
                if (start > end || end > size - 8 - headerSize)
                    throw new IOException("Datos de modelo truncados: " + source.getFileName());
            }
            if (!tensor) throw new IOException("El archivo no contiene tensores reconocibles: " + source.getFileName());
        } catch (NumberFormatException failure) {
            throw new IOException("Tamaño de tensor inválido: " + source.getFileName(), failure);
        }
    }

    private void mergeT5(Path source, Path target, Path root, ModelSetupProgressListener progress)
            throws IOException, InterruptedException {
        Path python = firstExisting(root,
                "tools/image/venv/Scripts/python.exe", "tools/image/.venv/Scripts/python.exe",
                "tools/image/python_embeded/python.exe", "tools/image/python_embedded/python.exe");
        Path script = root.resolve("tools/scripts/merge_safetensors_shards.py");
        if (python == null || !Files.isRegularFile(script)) {
            throw new IOException("Falta Python local o tools/scripts/merge_safetensors_shards.py.");
        }
        ExternalProcessResult result = processRunner.run(ExternalProcessRequest.of(
                        List.of(python.toString(), script.toString(), source.toString(), target.toString()),
                        "flux-t5-consolidation",
                        Duration.ofMinutes(45))
                .withWorkingDirectory(root)
                .redirectingErrorStream());
        if (!result.combinedOutputTail().isBlank()) {
            progress.onProgress(result.combinedOutputTail());
        }
        if (!result.succeeded() || !Files.isRegularFile(target)) {
            Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".part"));
            throw new IOException("La consolidacion T5 termino con codigo " + result.exitCode() + ". " + result.combinedOutputTail());
        }
    }

    private static void ensureFreeSpace(Path target, long required) throws IOException {
        Files.createDirectories(target.getParent());
        FileStore store = Files.getFileStore(target.getParent());
        if (store.getUsableSpace() < required) {
            throw new IOException("Espacio insuficiente. Se requieren al menos 11 GB libres para consolidar T5.");
        }
    }

    private static Path firstExisting(Path root, String... names) {
        for (String name : names) {
            Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path root(Path applicationRoot) {
        return applicationRoot == null ? Path.of(".").toAbsolutePath().normalize()
                : applicationRoot.toAbsolutePath().normalize();
    }
}
