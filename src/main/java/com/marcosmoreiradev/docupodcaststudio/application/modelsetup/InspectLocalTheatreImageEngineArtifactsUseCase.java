package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Performs image-engine inspection with anti-placeholder rules. */
public final class InspectLocalTheatreImageEngineArtifactsUseCase {
    private static final long MIN_CHECKPOINT_BYTES = 1_000_000L;
    private static final long MIN_REFERENCE_BYTES = 1_024L;
    private static final int MAX_TEXT_PROBE_BYTES = 32_768;

    public ImageEngineArtifactInspectionReport inspect(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path runtime = root.resolve("tools/image").normalize();
        Path models = root.resolve("models/image").normalize();

        ArrayList<String> missing = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<String> discovered = new ArrayList<>();
        ImageEnginePresetSupport presetSupport = ImageEnginePresetSupportPolicy.forPresetId(current.imageGeneration().preset());
        boolean flux = presetSupport.preset() == com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset.HIGH_QUALITY_FLUX;

        ImageEngineComponentStatus runtimeStatus = runtimeStatus(runtime);
        if (runtimeStatus == ImageEngineComponentStatus.MISSING) {
            missing.add("Runtime local ejecutable en tools/image");
        } else if (runtimeStatus != ImageEngineComponentStatus.READY) {
            missing.add("Lanzador compatible en tools/image (start-image-engine.bat, ComfyUI.bat, run.bat o ComfyUI/main.py con Python local)");
        }

        FluxModelBundle fluxBundle = flux ? FluxModelBundle.inspect(root) : null;
        Artifact checkpoint = flux
                ? artifactFor(fluxBundle.model(), FluxModelBundle.MODEL_NAME)
                : inspectCheckpoint(models, current);
        discovered.addAll(checkpoint.discovered());
        if (checkpoint.status() == ImageEngineComponentStatus.MISSING) {
            missing.add("Checkpoint de imagen en models/image");
        } else if (checkpoint.status() == ImageEngineComponentStatus.INVALID
                || checkpoint.status() == ImageEngineComponentStatus.PLACEHOLDER) {
            missing.add("Checkpoint real de imagen (archivo actual invalido o placeholder)");
        }

        Artifact workflow = flux
                ? new Artifact(ImageEngineComponentStatus.READY, presetSupport.workflowName(), List.of(presetSupport.workflowName()))
                : inspectWorkflow(models, presetSupport);
        discovered.addAll(workflow.discovered());
        if (workflow.status() == ImageEngineComponentStatus.MISSING) {
            missing.add("Workflow ComfyUI real para " + presetSupport.preset().displayName()
                    + " en models/image/" + presetSupport.workflowName());
        } else if (workflow.status() == ImageEngineComponentStatus.INVALID
                || workflow.status() == ImageEngineComponentStatus.PLACEHOLDER) {
            missing.add("Workflow ComfyUI real para " + presetSupport.preset().displayName()
                    + "; el JSON actual es invalido o placeholder");
        }
        if (flux) {
            for (String component : fluxBundle.missingComponents()) {
                if (!component.equals(FluxModelBundle.MODEL_NAME)) {
                    missing.add("Componente FLUX: " + component);
                }
            }
            if (!new FluxLicenseAcceptanceStore().accepted(root)) {
                missing.add("Confirmacion local de licencia FLUX.1-dev");
            }
        }

        Artifact adapters = inspectOptionalArtifacts(models.resolve("adapters"), "adaptador");
        discovered.addAll(adapters.discovered());
        if (adapters.status() == ImageEngineComponentStatus.PLACEHOLDER
                || adapters.status() == ImageEngineComponentStatus.INVALID) {
            warnings.add("Adaptadores de referencia no instalados: hay archivos placeholder o invalidos.");
        } else if (adapters.status() == ImageEngineComponentStatus.OPTIONAL_MISSING) {
            warnings.add("Generacion basica lista; consistencia avanzada requiere adaptadores o LoRA.");
        }

        Artifact loras = inspectOptionalArtifacts(models.resolve("loras"), "lora");
        discovered.addAll(loras.discovered());
        if (loras.status() == ImageEngineComponentStatus.PLACEHOLDER
                || loras.status() == ImageEngineComponentStatus.INVALID) {
            warnings.add("LoRA opcional no instalado: hay archivos placeholder o invalidos.");
        }

        String message;
        if (missing.isEmpty()) {
            message = warnings.isEmpty()
                    ? "Imagen IA teatral lista para prueba real."
                    : "Imagen IA teatral lista para prueba basica. " + String.join(" ", warnings);
        } else {
            message = "Imagen IA teatral pendiente: " + String.join(" - ", missing) + ".";
        }

        return new ImageEngineArtifactInspectionReport(
                runtime,
                models,
                runtimeStatus,
                checkpoint.status(),
                workflow.status(),
                adapters.status(),
                loras.status(),
                checkpoint.name(),
                workflow.name(),
                missing,
                warnings,
                discovered,
                message);
    }

    private static ImageEngineComponentStatus runtimeStatus(Path runtime) {
        if (!Files.isDirectory(runtime)) {
            return ImageEngineComponentStatus.MISSING;
        }
        if (hasCompatibleLauncher(runtime)) {
            return ImageEngineComponentStatus.READY;
        }
        return ImageEngineComponentStatus.INVALID;
    }

    private static Artifact artifactFor(Path path, String name) {
        if (!Files.isRegularFile(path)) {
            return new Artifact(ImageEngineComponentStatus.MISSING, "", List.of());
        }
        return regularFileSize(path) >= MIN_CHECKPOINT_BYTES
                ? new Artifact(ImageEngineComponentStatus.READY, name, List.of(name))
                : new Artifact(ImageEngineComponentStatus.INVALID, "", List.of(name));
    }

    static boolean hasCompatibleLauncher(Path runtime) {
        return Files.isRegularFile(runtime.resolve("start-image-engine.bat"))
                || Files.isRegularFile(runtime.resolve("start.bat"))
                || Files.isRegularFile(runtime.resolve("run.bat"))
                || Files.isRegularFile(runtime.resolve("ComfyUI.bat"))
                || (Files.isRegularFile(runtime.resolve("main.py")) && hasLocalPython(runtime))
                || (Files.isRegularFile(runtime.resolve("ComfyUI/main.py")) && hasLocalPython(runtime));
    }

    static boolean hasLocalPython(Path runtime) {
        return Files.isRegularFile(runtime.resolve("python_embeded/python.exe"))
                || Files.isRegularFile(runtime.resolve("python_embedded/python.exe"))
                || Files.isRegularFile(runtime.resolve("venv/Scripts/python.exe"))
                || Files.isRegularFile(runtime.resolve(".venv/Scripts/python.exe"));
    }

    private static Artifact inspectCheckpoint(Path models, OperationalSettings settings) {
        ArrayList<Path> candidates = new ArrayList<>();
        ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(settings.imageGeneration().preset());
        addCandidate(candidates, models.resolve(settings.imageGeneration().modelName()));
        if (!profile.checkpointName().isBlank()) {
            addCandidate(candidates, models.resolve(profile.checkpointName()));
        }
        candidates.addAll(findFiles(models, List.of(".safetensors", ".ckpt")));
        return firstReal(candidates, true);
    }

    private static Artifact inspectWorkflow(Path models, ImageEnginePresetSupport presetSupport) {
        ImageEnginePresetSupport support = presetSupport == null
                ? ImageEnginePresetSupportPolicy.forPresetId("")
                : presetSupport;
        if (support.requiresImportedWorkflow()) {
            Path expected = models.resolve(support.workflowName()).normalize();
            Artifact specific = inspectWorkflowCandidate(models, expected);
            if (specific.status() != ImageEngineComponentStatus.MISSING) {
                return specific;
            }
            ArrayList<String> discovered = new ArrayList<>(findFiles(models.resolve("workflows"), List.of(".json"))
                    .stream()
                    .map(path -> relativeName(models, path))
                    .toList());
            return new Artifact(ImageEngineComponentStatus.MISSING, "", discovered);
        }
        List<Path> candidates = findFiles(models, List.of(".json"));
        ArrayList<String> discovered = new ArrayList<>();
        boolean sawInvalid = false;
        boolean sawPlaceholder = false;
        for (Path candidate : candidates) {
            String relative = relativeName(models, candidate);
            String name = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.equals("manifest.json") || name.equals("model-manifest.json")
                    || name.equals("package.json") || name.contains("sha256")) {
                continue;
            }
            discovered.add(relative);
            String text = readText(candidate);
            if (looksPlaceholder(candidate, text)) {
                sawPlaceholder = true;
                continue;
            }
            if (looksLikeComfyWorkflow(text)) {
                return new Artifact(ImageEngineComponentStatus.READY, relative, discovered);
            }
            sawInvalid = true;
        }
        if (sawPlaceholder) {
            return new Artifact(ImageEngineComponentStatus.PLACEHOLDER, "", discovered);
        }
        if (sawInvalid) {
            return new Artifact(ImageEngineComponentStatus.INVALID, "", discovered);
        }
        return new Artifact(ImageEngineComponentStatus.MISSING, "", discovered);
    }

    private static Artifact inspectWorkflowCandidate(Path models, Path candidate) {
        if (!Files.isRegularFile(candidate)) {
            return new Artifact(ImageEngineComponentStatus.MISSING, "", List.of());
        }
        String relative = relativeName(models, candidate);
        String text = readText(candidate);
        if (looksPlaceholder(candidate, text)) {
            return new Artifact(ImageEngineComponentStatus.PLACEHOLDER, "", List.of(relative));
        }
        if (looksLikeComfyWorkflow(text)) {
            return new Artifact(ImageEngineComponentStatus.READY, relative, List.of(relative));
        }
        return new Artifact(ImageEngineComponentStatus.INVALID, "", List.of(relative));
    }

    private static Artifact inspectOptionalArtifacts(Path directory, String kind) {
        if (!Files.isDirectory(directory)) {
            return new Artifact(ImageEngineComponentStatus.OPTIONAL_MISSING, "", List.of());
        }
        List<Path> candidates = findFiles(directory, List.of(".safetensors", ".ckpt", ".pt", ".pth", ".bin"));
        ArrayList<String> discovered = new ArrayList<>();
        boolean sawPlaceholder = false;
        boolean sawInvalid = false;
        for (Path candidate : candidates) {
            String relative = relativeName(directory.getParent(), candidate);
            discovered.add(relative);
            String text = readText(candidate);
            if (looksPlaceholder(candidate, text)) {
                sawPlaceholder = true;
                continue;
            }
            if (regularFileSize(candidate) >= MIN_REFERENCE_BYTES) {
                return new Artifact(ImageEngineComponentStatus.READY, relative, discovered);
            }
            sawInvalid = true;
        }
        if (sawPlaceholder) {
            return new Artifact(ImageEngineComponentStatus.PLACEHOLDER, "", discovered);
        }
        if (sawInvalid) {
            return new Artifact(ImageEngineComponentStatus.INVALID, "", discovered);
        }
        return new Artifact(ImageEngineComponentStatus.OPTIONAL_MISSING, "", discovered);
    }

    private static Artifact firstReal(List<Path> candidates, boolean checkpoint) {
        ArrayList<String> discovered = new ArrayList<>();
        boolean sawInvalid = false;
        boolean sawPlaceholder = false;
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            discovered.add(candidate.getFileName().toString());
            String text = readText(candidate);
            if (looksPlaceholder(candidate, text)) {
                sawPlaceholder = true;
                continue;
            }
            long size = regularFileSize(candidate);
            long minimum = checkpoint ? MIN_CHECKPOINT_BYTES : MIN_REFERENCE_BYTES;
            if (size >= minimum) {
                return new Artifact(ImageEngineComponentStatus.READY, candidate.getFileName().toString(), discovered);
            }
            sawInvalid = true;
        }
        if (sawPlaceholder) {
            return new Artifact(ImageEngineComponentStatus.PLACEHOLDER, "", discovered);
        }
        if (sawInvalid) {
            return new Artifact(ImageEngineComponentStatus.INVALID, "", discovered);
        }
        return new Artifact(ImageEngineComponentStatus.MISSING, "", discovered);
    }

    private static List<Path> findFiles(Path root, List<String> suffixes) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return suffixes.stream().anyMatch(lower::endsWith);
                    })
                    .sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static void addCandidate(List<Path> candidates, Path candidate) {
        if (candidate != null && !candidates.contains(candidate)) {
            candidates.add(candidate.normalize());
        }
    }

    private static boolean looksLikeComfyWorkflow(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return lower.contains("\"class_type\"")
                && lower.contains("checkpointloadersimple")
                && lower.contains("ksampler")
                && lower.contains("saveimage");
    }

    private static boolean looksPlaceholder(Path path, String text) {
        String name = path == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return name.contains("placeholder")
                || name.contains("mock")
                || lower.contains("placeholder")
                || lower.contains("\"purpose\"")
                || lower.contains("mock");
    }

    private static String readText(Path path) {
        try {
            long size = Files.size(path);
            if (size > MAX_TEXT_PROBE_BYTES) {
                return "";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ex) {
            return "";
        }
    }

    private static long regularFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private static String relativeName(Path root, Path path) {
        try {
            return root.toAbsolutePath().normalize()
                    .relativize(path.toAbsolutePath().normalize())
                    .toString()
                    .replace('\\', '/');
        } catch (RuntimeException ex) {
            return path.getFileName().toString();
        }
    }

    private record Artifact(ImageEngineComponentStatus status, String name, List<String> discovered) {
        private Artifact {
            status = status == null ? ImageEngineComponentStatus.MISSING : status;
            name = name == null ? "" : name.strip();
            discovered = List.copyOf(discovered == null ? List.of() : discovered);
        }
    }
}
