package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adapter-owned workflow materialization. Product code never sees ComfyUI node graphs. */
final class ComfyWorkflowTemplate {
    private static final Pattern POSITIVE = Pattern.compile("(\"class_type\"\\s*:\\s*\"CLIPTextEncode\"[^}]*?\"text\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern NUMBER_FIELD = Pattern.compile("(\"%s\"\\s*:\\s*)-?\\d+");
    private static final Pattern PREFIX = Pattern.compile("(\"filename_prefix\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern CHECKPOINT = Pattern.compile("(\"ckpt_name\"\\s*:\\s*\")[^\"]*(\")");

    private ComfyWorkflowTemplate() { }

    static String image(Path template, String prompt, String negativePrompt, int width, int height,
                        long seed, int batchSize, String prefix) throws IOException {
        return image(template, prompt, negativePrompt, width, height, seed, batchSize, prefix, "");
    }

    static String image(Path template, String prompt, String negativePrompt, int width, int height,
                        long seed, int batchSize, String prefix, String checkpointName) throws IOException {
        String workflow = read(template);
        Matcher matcher = POSITIVE.matcher(workflow);
        StringBuffer rendered = new StringBuffer();
        int occurrence = 0;
        while (matcher.find()) {
            String text = occurrence++ == 0 ? prompt : negativePrompt;
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(1) + json(text) + matcher.group(2)));
        }
        matcher.appendTail(rendered);
        workflow = rendered.toString();
        workflow = replaceNumber(workflow, "width", width);
        workflow = replaceNumber(workflow, "height", height);
        workflow = replaceNumber(workflow, "seed", seed);
        workflow = replaceNumber(workflow, "batch_size", batchSize);
        if (checkpointName != null && !checkpointName.isBlank()) {
            Matcher checkpointMatcher = CHECKPOINT.matcher(workflow);
            if (checkpointMatcher.find()) {
                workflow = checkpointMatcher.replaceFirst(Matcher.quoteReplacement(checkpointMatcher.group(1)
                        + json(checkpointName) + checkpointMatcher.group(2)));
            }
        }
        Matcher prefixMatcher = PREFIX.matcher(workflow);
        if (!prefixMatcher.find()) return workflow;
        return prefixMatcher.replaceFirst(Matcher.quoteReplacement(prefixMatcher.group(1)
                + json(prefix) + prefixMatcher.group(2)));
    }

    static String video(Path template, String prompt, String negativePrompt, int width, int height,
                        int fps, int frames, long seed, String prefix, String initialImage) throws IOException {
        String workflow = read(template);
        return workflow.replace("{{prompt}}", json(prompt))
                .replace("{{negativePrompt}}", json(negativePrompt))
                .replace("{{width}}", Integer.toString(width))
                .replace("{{height}}", Integer.toString(height))
                .replace("{{fps}}", Integer.toString(fps))
                .replace("{{frames}}", Integer.toString(frames))
                .replace("{{seed}}", Long.toString(seed))
                .replace("{{prefix}}", json(prefix))
                .replace("{{initialImage}}", json(initialImage));
    }

    static String flux(String prompt, int width, int height, long seed, int batchSize, String prefix,
                       String model, String vae, String clipL, String t5) {
        return "{"
                + "\"1\":{\"class_type\":\"UNETLoader\",\"inputs\":{\"unet_name\":\"" + json(model) + "\",\"weight_dtype\":\"fp8_e4m3fn\"}},"
                + "\"2\":{\"class_type\":\"DualCLIPLoader\",\"inputs\":{\"clip_name1\":\"" + json(clipL) + "\",\"clip_name2\":\"" + json(t5) + "\",\"type\":\"flux\",\"device\":\"cpu\"}},"
                + "\"3\":{\"class_type\":\"VAELoader\",\"inputs\":{\"vae_name\":\"" + json(vae) + "\"}},"
                + "\"4\":{\"class_type\":\"CLIPTextEncodeFlux\",\"inputs\":{\"clip\":[\"2\",0],\"clip_l\":\"" + json(prompt) + "\",\"t5xxl\":\"" + json(prompt) + "\",\"guidance\":3.5}},"
                + "\"5\":{\"class_type\":\"ModelSamplingFlux\",\"inputs\":{\"model\":[\"1\",0],\"max_shift\":1.15,\"base_shift\":0.5,\"width\":" + width + ",\"height\":" + height + "}},"
                + "\"6\":{\"class_type\":\"EmptySD3LatentImage\",\"inputs\":{\"width\":" + width + ",\"height\":" + height + ",\"batch_size\":" + batchSize + "}},"
                + "\"7\":{\"class_type\":\"RandomNoise\",\"inputs\":{\"noise_seed\":" + seed + "}},"
                + "\"8\":{\"class_type\":\"KSamplerSelect\",\"inputs\":{\"sampler_name\":\"euler\"}},"
                + "\"9\":{\"class_type\":\"BasicScheduler\",\"inputs\":{\"model\":[\"5\",0],\"scheduler\":\"simple\",\"steps\":28,\"denoise\":1}},"
                + "\"10\":{\"class_type\":\"BasicGuider\",\"inputs\":{\"model\":[\"5\",0],\"conditioning\":[\"4\",0]}},"
                + "\"11\":{\"class_type\":\"SamplerCustomAdvanced\",\"inputs\":{\"noise\":[\"7\",0],\"guider\":[\"10\",0],\"sampler\":[\"8\",0],\"sigmas\":[\"9\",0],\"latent_image\":[\"6\",0]}},"
                + "\"12\":{\"class_type\":\"VAEDecode\",\"inputs\":{\"samples\":[\"11\",0],\"vae\":[\"3\",0]}},"
                + "\"13\":{\"class_type\":\"SaveImage\",\"inputs\":{\"filename_prefix\":\"" + json(prefix) + "\",\"images\":[\"12\",0]}}"
                + "}";
    }

    static String superResolution(String uploadedImage, String modelName, String prefix) {
        return "{"
                + "\"1\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + json(uploadedImage) + "\"}},"
                + "\"2\":{\"class_type\":\"UpscaleModelLoader\",\"inputs\":{\"model_name\":\"" + json(modelName) + "\"}},"
                + "\"3\":{\"class_type\":\"ImageUpscaleWithModel\",\"inputs\":{\"upscale_model\":[\"2\",0],\"image\":[\"1\",0]}},"
                + "\"4\":{\"class_type\":\"SaveImage\",\"inputs\":{\"filename_prefix\":\"" + json(prefix)
                + "\",\"images\":[\"3\",0]}}"
                + "}";
    }

    static String refinement(
            String uploadedImage,
            String checkpoint,
            String controlNet,
            String prompt,
            String negativePrompt,
            String prefix,
            long seed,
            int steps,
            double denoise,
            double controlStrength,
            double cfg) {
        return "{"
                + "\"1\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + json(uploadedImage) + "\"}},"
                + "\"2\":{\"class_type\":\"CheckpointLoaderSimple\",\"inputs\":{\"ckpt_name\":\""
                + json(checkpoint) + "\"}},"
                + "\"3\":{\"class_type\":\"CLIPTextEncode\",\"inputs\":{\"text\":\"" + json(prompt)
                + "\",\"clip\":[\"2\",1]}},"
                + "\"4\":{\"class_type\":\"CLIPTextEncode\",\"inputs\":{\"text\":\""
                + json(negativePrompt) + "\",\"clip\":[\"2\",1]}},"
                + "\"5\":{\"class_type\":\"ControlNetLoader\",\"inputs\":{\"control_net_name\":\""
                + json(controlNet) + "\"}},"
                + "\"6\":{\"class_type\":\"ControlNetApplyAdvanced\",\"inputs\":{\"positive\":[\"3\",0],"
                + "\"negative\":[\"4\",0],\"control_net\":[\"5\",0],\"image\":[\"1\",0],"
                + "\"strength\":" + decimal(controlStrength) + ",\"start_percent\":0.0,\"end_percent\":1.0}},"
                + "\"7\":{\"class_type\":\"VAEEncode\",\"inputs\":{\"pixels\":[\"1\",0],\"vae\":[\"2\",2]}},"
                + "\"8\":{\"class_type\":\"KSampler\",\"inputs\":{\"seed\":" + seed + ",\"steps\":" + steps
                + ",\"cfg\":" + decimal(cfg) + ",\"sampler_name\":\"dpmpp_2m\",\"scheduler\":\"karras\","
                + "\"denoise\":" + decimal(denoise) + ",\"model\":[\"2\",0],\"positive\":[\"6\",0],"
                + "\"negative\":[\"6\",1],\"latent_image\":[\"7\",0]}},"
                + "\"9\":{\"class_type\":\"VAEDecode\",\"inputs\":{\"samples\":[\"8\",0],\"vae\":[\"2\",2]}},"
                + "\"10\":{\"class_type\":\"SaveImage\",\"inputs\":{\"filename_prefix\":\""
                + json(prefix) + "\",\"images\":[\"9\",0]}}"
                + "}";
    }

    private static String read(Path template) throws IOException {
        if (template == null || !Files.isRegularFile(template)) {
            throw new IOException("Falta el workflow del preset: " + template);
        }
        return Files.readString(template, StandardCharsets.UTF_8);
    }

    private static String replaceNumber(String text, String field, long value) {
        return Pattern.compile(NUMBER_FIELD.pattern().formatted(Pattern.quote(field)))
                .matcher(text).replaceAll("$1" + value);
    }

    private static String json(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String decimal(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
