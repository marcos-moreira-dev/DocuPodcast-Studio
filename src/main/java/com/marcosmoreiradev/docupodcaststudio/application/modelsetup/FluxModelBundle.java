package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Resolved FLUX.1-dev component bundle shared by settings, preflight and workflows. */
public record FluxModelBundle(
        Path model,
        Path vae,
        Path clipL,
        Path t5,
        Path workflow,
        List<String> missingComponents
) {
    public static final String MODEL_NAME = "flux1-dev.safetensors";
    public static final String VAE_NAME = "ae.safetensors";
    public static final String CLIP_L_NAME = "clip_l.safetensors";
    public static final String WORKFLOW_NAME = "workflow-flux-reference.json";

    public FluxModelBundle {
        missingComponents = List.copyOf(missingComponents == null ? List.of() : missingComponents);
    }

    public static FluxModelBundle inspect(Path applicationRoot) {
        Path root = root(applicationRoot).resolve("models/image");
        Path model = root.resolve(MODEL_NAME);
        Path vae = root.resolve("vae").resolve(VAE_NAME);
        Path clip = root.resolve("text_encoders").resolve(CLIP_L_NAME);
        Path t5 = firstExisting(root.resolve("text_encoders"),
                "t5xxl_fp8_e4m3fn_scaled.safetensors",
                "t5xxl_fp8_e4m3fn.safetensors",
                "t5xxl_fp16.safetensors",
                "t5xxl_bf16.safetensors");
        Path workflow = root.resolve("workflows").resolve(WORKFLOW_NAME);
        ArrayList<String> missing = new ArrayList<>();
        require(model, MODEL_NAME, missing);
        require(vae, "vae/" + VAE_NAME, missing);
        require(clip, "text_encoders/" + CLIP_L_NAME, missing);
        if (t5 == null) {
            missing.add("text_encoders/T5XXL FP8, FP16 o BF16");
        }
        return new FluxModelBundle(model, vae, clip, t5, workflow, missing);
    }

    public boolean ready() {
        return missingComponents.isEmpty();
    }

    public String modelName() {
        return model == null ? "" : model.getFileName().toString();
    }

    public String vaeName() {
        return vae == null ? "" : vae.getFileName().toString();
    }

    public String clipLName() {
        return clipL == null ? "" : clipL.getFileName().toString();
    }

    public String t5Name() {
        return t5 == null ? "" : t5.getFileName().toString();
    }

    private static void require(Path path, String name, List<String> missing) {
        if (!Files.isRegularFile(path)) {
            missing.add(name);
        }
    }

    private static Path firstExisting(Path directory, String... names) {
        for (String name : names) {
            Path candidate = directory.resolve(name);
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
