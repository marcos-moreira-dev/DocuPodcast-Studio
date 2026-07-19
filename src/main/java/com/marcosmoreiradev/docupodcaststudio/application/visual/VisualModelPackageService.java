package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Category-neutral model store inspection and hash reuse.
 *
 * <p>Downloads remain an explicit UI operation. This service never contacts the
 * network and never removes model files.</p>
 */
public final class VisualModelPackageService {
    public VisualModelPackageInspection inspect(Path modelStore,
                                                VisualGenerationProfile profile,
                                                boolean licenseAccepted) {
        Path root = normalizeRoot(modelStore);
        VisualGenerationProfile selected = profile == null
                ? VisualGenerationProfile.DIAGNOSTIC_SD15
                : profile;
        LinkedHashMap<String, Path> components = new LinkedHashMap<>();
        ArrayList<String> missing = new ArrayList<>();
        requirements(selected).forEach((component, alternatives) -> {
            Path resolved = firstExisting(root, alternatives);
            if (resolved == null) {
                missing.add(component + " (" + String.join(" | ", alternatives) + ")");
            } else {
                components.put(component, resolved);
            }
        });
        if (selected.explicitLicenseAcceptance() && !licenseAccepted) {
            missing.add("aceptacion explicita de licencia no comercial");
        }
        long required = Math.max(0L, approximateBytes(selected) - existingBytes(components.values()));
        long free = usableSpace(root);
        VisualModelManifest manifest = new VisualModelManifest(
                selected.name().toLowerCase(java.util.Locale.ROOT),
                selected,
                components,
                Map.of(),
                selected.explicitLicenseAcceptance() ? "FLUX-1-NON-COMMERCIAL" : "",
                licenseAccepted,
                approximateBytes(selected));
        ArrayList<String> diagnostics = new ArrayList<>();
        diagnostics.add("modelStore=" + root);
        diagnostics.add("profile=" + selected);
        diagnostics.add("freeBytes=" + free);
        diagnostics.add("requiredBytes=" + required);
        if (free < required) {
            diagnostics.add("Espacio insuficiente para completar el paquete seleccionado.");
        }
        return new VisualModelPackageInspection(
                manifest, missing, Map.of(), free, required, diagnostics);
    }

    public Map<String, List<Path>> findDuplicatesByHash(List<Path> roots) throws IOException {
        LinkedHashMap<Long, List<Path>> bySize = new LinkedHashMap<>();
        for (Path root : roots == null ? List.<Path>of() : roots) {
            if (root == null || !Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(Files::isRegularFile)
                        .filter(this::looksLikeModel)
                        .forEach(path -> {
                            try {
                                bySize.computeIfAbsent(Files.size(path), ignored -> new ArrayList<>()).add(path);
                            } catch (IOException ignored) {
                                // Unreadable files are reported by their package preflight instead.
                            }
                        });
            }
        }
        LinkedHashMap<String, List<Path>> duplicates = new LinkedHashMap<>();
        for (List<Path> candidates : bySize.values()) {
            if (candidates.size() < 2) {
                continue;
            }
            LinkedHashMap<String, List<Path>> byHash = new LinkedHashMap<>();
            for (Path candidate : candidates) {
                byHash.computeIfAbsent(sha256(candidate), ignored -> new ArrayList<>()).add(candidate);
            }
            byHash.forEach((hash, paths) -> {
                if (paths.size() > 1) {
                    duplicates.put(hash, List.copyOf(paths));
                }
            });
        }
        return Map.copyOf(duplicates);
    }

    public void reuseByHash(Path source, Path target) throws IOException {
        Path from = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        Path to = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        if (!Files.isRegularFile(from)) {
            throw new IOException("El modelo fuente no existe: " + from);
        }
        Files.createDirectories(to.getParent());
        if (Files.isRegularFile(to) && sha256(from).equals(sha256(to))) {
            return;
        }
        Path staging = to.resolveSibling(to.getFileName() + ".part");
        Files.deleteIfExists(staging);
        try {
            Files.createLink(staging, from);
        } catch (IOException | UnsupportedOperationException ex) {
            Files.copy(from, staging, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!sha256(from).equals(sha256(staging))) {
            Files.deleteIfExists(staging);
            throw new IOException("La copia del modelo no coincide con su hash fuente.");
        }
        Files.move(staging, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    private Map<String, List<String>> requirements(VisualGenerationProfile profile) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        switch (profile) {
            case DIAGNOSTIC_SD15 -> result.put("checkpoint",
                    List.of("v1-5-pruned-emaonly-fp16.safetensors",
                            "checkpoints/v1-5-pruned-emaonly-fp16.safetensors"));
            case PRODUCTION_SDXL_REFERENCE -> {
                result.put("sdxl-base", List.of("sd_xl_base_1.0.safetensors",
                        "checkpoints/sd_xl_base_1.0.safetensors"));
                result.put("sdxl-refiner", List.of("sd_xl_refiner_1.0.safetensors",
                        "checkpoints/sd_xl_refiner_1.0.safetensors"));
                result.put("clip-vision", List.of("clip_vision/CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors"));
                result.put("ip-adapter-plus", List.of("ipadapter/ip-adapter-plus_sdxl_vit-h.safetensors"));
                result.put("ip-adapter-faceid", List.of("ipadapter/ip-adapter-faceid-plusv2_sdxl.bin"));
                result.put("faceid-lora", List.of(
                        "loras/ip-adapter-faceid-plusv2_sdxl_lora.safetensors",
                        "loras/ip-adapter-faceid-plusv2_sdxl_lora.pt"));
                result.put("insightface", List.of(
                        "insightface/models/antelopev2/glintr100.onnx",
                        "insightface/models/antelopev2/w600k_r50.onnx",
                        "insightface/models/antelopev2/1k3d68.onnx"));
                result.put("controlnet", List.of("controlnet/diffusers_xl_canny_full.safetensors",
                        "controlnet/controlnet-scribble-sdxl-1.0.safetensors"));
                result.put("workflow", List.of("workflows/workflow-sdxl-ipadapter-reference-api.json"));
            }
            case ADVANCED_FLUX_KONTEXT -> {
                result.put("flux-kontext", List.of("flux1-kontext-dev.safetensors",
                        "diffusion_models/flux1-kontext-dev.safetensors"));
                result.put("vae", List.of("vae/ae.safetensors"));
                result.put("clip-l", List.of("text_encoders/clip_l.safetensors"));
                result.put("t5xxl", List.of("text_encoders/t5xxl_fp8_e4m3fn_scaled.safetensors",
                        "text_encoders/t5xxl_fp8_e4m3fn.safetensors",
                        "text_encoders/t5xxl_fp16.safetensors"));
                result.put("workflow", List.of("workflows/workflow-flux-kontext-reference-api.json"));
            }
            case CUSTOM_COMFY_WORKFLOW -> result.put("workflow",
                    List.of("workflows/workflow-custom-comfy.json"));
        }
        return result;
    }

    private static long approximateBytes(VisualGenerationProfile profile) {
        return switch (profile) {
            case DIAGNOSTIC_SD15 -> 2_200_000_000L;
            case PRODUCTION_SDXL_REFERENCE -> 15_000_000_000L;
            case ADVANCED_FLUX_KONTEXT -> 24_000_000_000L;
            case CUSTOM_COMFY_WORKFLOW -> 0L;
        };
    }

    private static Path firstExisting(Path root, List<String> alternatives) {
        for (String relative : alternatives) {
            Path candidate = root.resolve(relative).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static long existingBytes(Iterable<Path> paths) {
        long total = 0L;
        for (Path path : paths) {
            try {
                total += Files.size(path);
            } catch (IOException ignored) {
                // Missing or unreadable components remain visible in the inspection result.
            }
        }
        return total;
    }

    private static long usableSpace(Path root) {
        try {
            Files.createDirectories(root);
            FileStore store = Files.getFileStore(root);
            return store.getUsableSpace();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private boolean looksLikeModel(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".safetensors") || name.endsWith(".ckpt")
                || name.endsWith(".bin") || name.endsWith(".pt") || name.endsWith(".pth");
    }

    private static Path normalizeRoot(Path modelStore) {
        return (modelStore == null ? Path.of("models/image") : modelStore)
                .toAbsolutePath().normalize();
    }
}
