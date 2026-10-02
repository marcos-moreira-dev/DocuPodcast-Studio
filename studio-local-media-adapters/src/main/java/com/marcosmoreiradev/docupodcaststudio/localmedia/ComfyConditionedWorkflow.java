package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the executable SD1.5 regional IP-Adapter graph owned by the local adapter. */
final class ComfyConditionedWorkflow {
    record Uploaded(MediaReference reference, String engineName) { }

    private final LinkedHashMap<String, String> nodes = new LinkedHashMap<>();
    private int sequence;

    String render(String checkpoint,
                  String scribbleControlNet,
                  String prompt,
                  String negativePrompt,
                  int width,
                  int height,
                  long seed,
                  int steps,
                  double cfg,
                  String prefix,
                  List<Uploaded> uploaded) {
        nodes.clear();
        sequence = 0;
        String checkpointNode = add("CheckpointLoaderSimple",
                "\"ckpt_name\":\"" + json(checkpoint) + "\"");
        String positive = add("CLIPTextEncode",
                "\"text\":\"" + json(prompt) + "\",\"clip\":" + ref(checkpointNode, 1));
        String negative = add("CLIPTextEncode",
                "\"text\":\"" + json(negativePrompt) + "\",\"clip\":" + ref(checkpointNode, 1));
        String loader = add("IPAdapterUnifiedLoader",
                "\"model\":" + ref(checkpointNode, 0)
                        + ",\"preset\":\"PLUS (high strength)\"");
        String model = loader;

        Map<String, List<Uploaded>> identities = new LinkedHashMap<>();
        for (Uploaded item : safe(uploaded)) {
            if (MediaReferenceRole.REGIONAL_IDENTITY.equals(item.reference().role())) {
                String subject = item.reference().metadata().getOrDefault(
                        "subjectId", item.reference().id());
                identities.computeIfAbsent(subject, ignored -> new ArrayList<>()).add(item);
            }
        }
        for (List<Uploaded> subjectReferences : identities.values()) {
            Uploaded primary = subjectReferences.getFirst();
            String image = loadImage(primary);
            // A second full-body view must not be batched as another subject. The
            // frontal/primary view is the generation authority; additional views
            // remain in provenance and can be used by a later identity-specific pass.
            Region region = Region.from(primary.reference().metadata(), width, height);
            String mask = mask(region, width, height);
            double bodyWeight = Math.min(0.68, primary.reference().strength() * 0.72);
            model = add("IPAdapterAdvanced",
                    "\"model\":" + ref(model, 0) + ",\"ipadapter\":" + ref(loader, 1)
                            + ",\"image\":" + ref(image, 0)
                            + ",\"weight\":" + decimal(bodyWeight)
                            + ",\"weight_type\":\"style transfer precise\",\"combine_embeds\":\"average\","
                            + "\"start_at\":0.0,\"end_at\":0.82,"
                            + "\"embeds_scaling\":\"V only\","
                            + "\"attn_mask\":" + ref(mask, 0));
            String regionalText = String.join("; ", List.of(
                    primary.reference().metadata().getOrDefault("subjectName", primary.reference().id()),
                    primary.reference().metadata().getOrDefault("characterNotes", ""),
                    "preserve exact identity and complete wardrobe from references",
                    primary.reference().metadata().getOrDefault("location", "")));
            String regionalConditioning = add("CLIPTextEncode",
                    "\"text\":\"" + json(regionalText) + "\",\"clip\":" + ref(checkpointNode, 1));
            String maskedConditioning = add("ConditioningSetMask",
                    "\"conditioning\":" + ref(regionalConditioning, 0)
                            + ",\"mask\":" + ref(mask, 0)
                            + ",\"strength\":1.15,\"set_cond_area\":\"mask bounds\"");
            positive = add("ConditioningCombine",
                    "\"conditioning_1\":" + ref(positive, 0)
                            + ",\"conditioning_2\":" + ref(maskedConditioning, 0));
        }

        for (Uploaded item : safe(uploaded)) {
            if (!globalIpAdapterRole(item.reference().role())) continue;
            String image = loadImage(item);
            String regionalMask = item.reference().metadata().containsKey("regionX")
                    ? ",\"attn_mask\":" + ref(mask(
                    Region.from(item.reference().metadata(), width, height), width, height), 0)
                    : "";
            model = add("IPAdapterAdvanced",
                    "\"model\":" + ref(model, 0) + ",\"ipadapter\":" + ref(loader, 1)
                            + ",\"image\":" + ref(image, 0)
                            + ",\"weight\":" + decimal(item.reference().strength())
                            + ",\"weight_type\":\"composition\",\"combine_embeds\":\"average\","
                            + "\"start_at\":0.0,\"end_at\":0.55,"
                            + "\"embeds_scaling\":\"K+mean(V) w/ C penalty\""
                            + regionalMask);
        }

        Uploaded storyboard = safe(uploaded).stream()
                .filter(item -> MediaReferenceRole.COMPOSITION_GUIDE.equals(item.reference().role())
                        || MediaReferenceRole.DRAWN_GUIDE.equals(item.reference().role()))
                .findFirst().orElse(null);
        String latent;
        double denoise;
        if (storyboard != null && MediaReferenceRole.COMPOSITION_GUIDE.equals(storyboard.reference().role())) {
            String image = loadImage(storyboard);
            String scaled = add("ImageScale", "\"image\":" + ref(image, 0)
                    + ",\"upscale_method\":\"lanczos\",\"width\":" + width
                    + ",\"height\":" + height + ",\"crop\":\"center\"");
            latent = add("VAEEncode", "\"pixels\":" + ref(scaled, 0)
                    + ",\"vae\":" + ref(checkpointNode, 2));
            // An approved/imported storyboard is the composition authority. Keep denoise
            // deliberately low so img2img can improve rendering without replacing faces,
            // wardrobe, pose or the assigned camera.
            denoise = value(storyboard.reference().metadata(), "compositionDenoise", 0.20);
        } else {
            latent = add("EmptyLatentImage", "\"width\":" + width
                    + ",\"height\":" + height + ",\"batch_size\":1");
            denoise = 1.0;
            if (storyboard != null) {
                String image = loadImage(storyboard);
                String scaled = add("ImageScale", "\"image\":" + ref(image, 0)
                        + ",\"upscale_method\":\"lanczos\",\"width\":" + width
                        + ",\"height\":" + height + ",\"crop\":\"center\"");
                String control = add("ControlNetLoader",
                        "\"control_net_name\":\"" + json(scribbleControlNet) + "\"");
                String applied = add("ControlNetApplyAdvanced",
                        "\"positive\":" + ref(positive, 0) + ",\"negative\":" + ref(negative, 0)
                                + ",\"control_net\":" + ref(control, 0) + ",\"image\":" + ref(scaled, 0)
                                + ",\"strength\":"
                                + decimal(value(storyboard.reference().metadata(),
                                "controlStrength", 0.78))
                                + ",\"start_percent\":0.0,\"end_percent\":0.85");
                positive = applied;
                negative = applied;
            }
        }
        String sampler = add("KSampler", "\"seed\":" + seed + ",\"steps\":" + Math.max(12, steps)
                + ",\"cfg\":" + decimal(cfg) + ",\"sampler_name\":\"dpmpp_2m\","
                + "\"scheduler\":\"karras\",\"denoise\":" + decimal(denoise)
                + ",\"model\":" + ref(model, 0) + ",\"positive\":" + ref(positive, 0)
                + ",\"negative\":" + ref(negative, MediaReferenceRole.DRAWN_GUIDE.equals(
                storyboard == null ? null : storyboard.reference().role()) ? 1 : 0)
                + ",\"latent_image\":" + ref(latent, 0));
        String decoded = add("VAEDecode", "\"samples\":" + ref(sampler, 0)
                + ",\"vae\":" + ref(checkpointNode, 2));
        add("SaveImage", "\"filename_prefix\":\"" + json(prefix)
                + "\",\"images\":" + ref(decoded, 0));
        return "{" + String.join(",", nodes.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue()).toList()) + "}";
    }

    private String loadImage(Uploaded uploaded) {
        return add("LoadImage", "\"image\":\"" + json(uploaded.engineName()) + "\"");
    }

    private String mask(Region region, int width, int height) {
        String black = add("SolidMask", "\"value\":0.0,\"width\":" + width + ",\"height\":" + height);
        String white = add("SolidMask", "\"value\":1.0,\"width\":" + region.width
                + ",\"height\":" + region.height);
        return add("MaskComposite", "\"destination\":" + ref(black, 0)
                + ",\"source\":" + ref(white, 0) + ",\"x\":" + region.x + ",\"y\":" + region.y
                + ",\"operation\":\"add\"");
    }

    private String add(String classType, String inputs) {
        String id = Integer.toString(++sequence);
        nodes.put(id, "{\"class_type\":\"" + classType + "\",\"inputs\":{" + inputs + "}}");
        return id;
    }

    private static String ref(String node, int output) {
        return "[\"" + node + "\"," + output + "]";
    }

    private static boolean globalIpAdapterRole(MediaReferenceRole role) {
        return MediaReferenceRole.OBJECT.equals(role)
                || MediaReferenceRole.ENVIRONMENT.equals(role)
                || MediaReferenceRole.CAMERA_GUIDE.equals(role)
                || MediaReferenceRole.PREVIOUS_FRAME.equals(role)
                || MediaReferenceRole.NEXT_FRAME.equals(role);
    }

    private static List<Uploaded> safe(List<Uploaded> values) {
        return values == null ? List.of() : values;
    }

    private static String json(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n").replace("\t", "\\t");
    }

    private static String decimal(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static double value(Map<String, String> metadata, String key, double fallback) {
        try {
            return Double.parseDouble(metadata.getOrDefault(key, Double.toString(fallback)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private record Region(int x, int y, int width, int height) {
        static Region from(Map<String, String> metadata, int targetWidth, int targetHeight) {
            double x = ComfyConditionedWorkflow.value(metadata, "regionX", 0.0);
            double y = ComfyConditionedWorkflow.value(metadata, "regionY", 0.08);
            double width = ComfyConditionedWorkflow.value(metadata, "regionWidth", 0.34);
            double height = ComfyConditionedWorkflow.value(metadata, "regionHeight", 0.84);
            int px = clamp((int) Math.round(x * targetWidth), 0, targetWidth - 1);
            int py = clamp((int) Math.round(y * targetHeight), 0, targetHeight - 1);
            int pw = clamp((int) Math.round(width * targetWidth), 8, targetWidth - px);
            int ph = clamp((int) Math.round(height * targetHeight), 8, targetHeight - py);
            return new Region(px, py, pw, ph);
        }

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(Math.max(minimum, maximum), value));
        }
    }

}
