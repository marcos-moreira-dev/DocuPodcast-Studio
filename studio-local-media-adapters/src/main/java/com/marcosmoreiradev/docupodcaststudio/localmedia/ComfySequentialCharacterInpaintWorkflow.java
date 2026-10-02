package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds exactly one referenced character at a time to an already composed scene.
 * Each sampler is restricted by an inpaint mask, so identity conditioning cannot
 * create extra subjects elsewhere in the frame.
 */
final class ComfySequentialCharacterInpaintWorkflow {
    private final LinkedHashMap<String, String> nodes = new LinkedHashMap<>();
    private int sequence;

    String render(String checkpoint,
                  String prompt,
                  String negativePrompt,
                  int width,
                  int height,
                  long seed,
                  int steps,
                  String prefix,
                  String baseImageName,
                  List<ComfyConditionedWorkflow.Uploaded> uploaded) {
        nodes.clear();
        sequence = 0;
        String checkpointNode = add("CheckpointLoaderSimple",
                "\"ckpt_name\":\"" + json(checkpoint) + "\"");
        String loader = add("IPAdapterUnifiedLoader",
                "\"model\":" + ref(checkpointNode, 0)
                        + ",\"preset\":\"PLUS (high strength)\"");
        String current = add("LoadImage", "\"image\":\"" + json(baseImageName) + "\"");
        current = add("ImageScale", "\"image\":" + ref(current, 0)
                + ",\"upscale_method\":\"lanczos\",\"width\":" + width
                + ",\"height\":" + height + ",\"crop\":\"center\"");

        for (ComfyConditionedWorkflow.Uploaded subject : primaryIdentities(uploaded)) {
            MediaReference reference = subject.reference();
            Region region = Region.from(reference.metadata(), width, height);
            String mask = mask(region, width, height);
            String image = add("LoadImage", "\"image\":\"" + json(subject.engineName()) + "\"");
            String model = add("IPAdapterAdvanced",
                    "\"model\":" + ref(checkpointNode, 0) + ",\"ipadapter\":" + ref(loader, 1)
                            + ",\"image\":" + ref(image, 0)
                            + ",\"weight\":0.72,\"weight_type\":\"linear\","
                            + "\"combine_embeds\":\"average\",\"start_at\":0.0,\"end_at\":0.82,"
                            + "\"embeds_scaling\":\"K+mean(V) w/ C penalty\","
                            + "\"attn_mask\":" + ref(mask, 0));
            String subjectPrompt = prompt + ". Exactly one person inside the active region: "
                    + reference.metadata().getOrDefault("subjectName", reference.id()) + ". "
                    + reference.metadata().getOrDefault("characterNotes", "")
                    + ". Preserve the exact face, moustache, goggles, hair, scarf, jacket, trousers "
                    + "and boots from this reference. Full body, coherent scale and eyeline.";
            String positive = add("CLIPTextEncode",
                    "\"text\":\"" + json(subjectPrompt) + "\",\"clip\":" + ref(checkpointNode, 1));
            String negative = add("CLIPTextEncode",
                    "\"text\":\"" + json(negativePrompt
                            + ", duplicate person, two people in the active region, extra head, extra body")
                            + "\",\"clip\":" + ref(checkpointNode, 1));
            String latent = add("VAEEncodeForInpaint",
                    "\"pixels\":" + ref(current, 0) + ",\"vae\":" + ref(checkpointNode, 2)
                            + ",\"mask\":" + ref(mask, 0) + ",\"grow_mask_by\":12");
            String sampler = add("KSampler",
                    "\"seed\":" + (seed + sequence) + ",\"steps\":" + Math.max(24, steps)
                            + ",\"cfg\":6.0,\"sampler_name\":\"dpmpp_2m\","
                            + "\"scheduler\":\"karras\",\"denoise\":0.82,"
                            + "\"model\":" + ref(model, 0) + ",\"positive\":" + ref(positive, 0)
                            + ",\"negative\":" + ref(negative, 0)
                            + ",\"latent_image\":" + ref(latent, 0));
            current = add("VAEDecode", "\"samples\":" + ref(sampler, 0)
                    + ",\"vae\":" + ref(checkpointNode, 2));
        }
        add("SaveImage", "\"filename_prefix\":\"" + json(prefix)
                + "\",\"images\":" + ref(current, 0));
        return "{" + String.join(",", nodes.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue()).toList()) + "}";
    }

    private List<ComfyConditionedWorkflow.Uploaded> primaryIdentities(
            List<ComfyConditionedWorkflow.Uploaded> uploaded) {
        LinkedHashMap<String, ComfyConditionedWorkflow.Uploaded> identities = new LinkedHashMap<>();
        if (uploaded != null) {
            for (ComfyConditionedWorkflow.Uploaded item : uploaded) {
                if (!MediaReferenceRole.REGIONAL_IDENTITY.equals(item.reference().role())) continue;
                String subject = item.reference().metadata().getOrDefault(
                        "subjectId", item.reference().id());
                identities.putIfAbsent(subject, item);
            }
        }
        return new ArrayList<>(identities.values());
    }

    private String mask(Region region, int width, int height) {
        String black = add("SolidMask", "\"value\":0.0,\"width\":" + width + ",\"height\":" + height);
        String white = add("SolidMask", "\"value\":1.0,\"width\":" + region.width
                + ",\"height\":" + region.height);
        String feathered = add("FeatherMask", "\"mask\":" + ref(white, 0)
                + ",\"left\":24,\"top\":24,\"right\":24,\"bottom\":24");
        return add("MaskComposite", "\"destination\":" + ref(black, 0)
                + ",\"source\":" + ref(feathered, 0) + ",\"x\":" + region.x + ",\"y\":" + region.y
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

    private static String json(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n").replace("\t", "\\t");
    }

    private record Region(int x, int y, int width, int height) {
        static Region from(Map<String, String> metadata, int targetWidth, int targetHeight) {
            double x = value(metadata, "regionX", 0.0);
            double y = value(metadata, "regionY", 0.08);
            double width = value(metadata, "regionWidth", 0.34);
            double height = value(metadata, "regionHeight", 0.84);
            int px = clamp((int) Math.round(x * targetWidth), 0, targetWidth - 1);
            int py = clamp((int) Math.round(y * targetHeight), 0, targetHeight - 1);
            int pw = clamp((int) Math.round(width * targetWidth), 8, targetWidth - px);
            int ph = clamp((int) Math.round(height * targetHeight), 8, targetHeight - py);
            return new Region(px, py, pw, ph);
        }

        private static double value(Map<String, String> metadata, String key, double fallback) {
            try {
                return Double.parseDouble(metadata.getOrDefault(key, Double.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(Math.max(minimum, maximum), value));
        }
    }
}
