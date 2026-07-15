package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.Random;
import java.io.IOException;
import java.nio.file.Path;

/** Produces API-format ComfyUI workflows without coupling callers to node graphs. */
public final class ComfyUiWorkflowPayloadFactory {
    public String create(VisualEngineRequest request, ComfyUiWorkflowSpec workflow) throws IOException {
        ComfyUiWorkflowSpec current = workflow == null ? ComfyUiWorkflowSpec.sd15() : workflow;
        Path conditionedTemplate = new VisualConditioningWorkflowSupport().requireTemplate(current, request);
        if (conditionedTemplate != null) {
            return new ConditionedWorkflowTemplateRenderer().render(conditionedTemplate, request, current);
        }
        return current.kind() == ComfyUiWorkflowKind.FLUX1_DEV_COMPONENTS
                ? flux(request, current)
                : sd15(request);
    }

    private static String sd15(VisualEngineRequest request) {
        long seed = positiveSeed();
        String negative = request.negativePrompt().isBlank()
                ? "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark"
                : request.negativePrompt();
        return "{\"prompt\":{" 
                + "\"1\":{\"class_type\":\"CheckpointLoaderSimple\",\"inputs\":{\"ckpt_name\":\"" + esc(request.checkpointName()) + "\"}},"
                + "\"2\":{\"class_type\":\"CLIPTextEncode\",\"inputs\":{\"text\":\"" + esc(request.prompt()) + "\",\"clip\":[\"1\",1]}},"
                + "\"3\":{\"class_type\":\"CLIPTextEncode\",\"inputs\":{\"text\":\"" + esc(negative) + "\",\"clip\":[\"1\",1]}},"
                + "\"4\":{\"class_type\":\"EmptyLatentImage\",\"inputs\":{\"width\":" + request.generationWidth()
                + ",\"height\":" + request.generationHeight() + ",\"batch_size\":" + request.batchSize() + "}},"
                + "\"5\":{\"class_type\":\"KSampler\",\"inputs\":{\"seed\":" + seed
                + ",\"steps\":" + request.steps() + ",\"cfg\":" + request.cfg()
                + ",\"sampler_name\":\"euler\",\"scheduler\":\"normal\",\"denoise\":1,\"model\":[\"1\",0],\"positive\":[\"2\",0],\"negative\":[\"3\",0],\"latent_image\":[\"4\",0]}},"
                + "\"6\":{\"class_type\":\"VAEDecode\",\"inputs\":{\"samples\":[\"5\",0],\"vae\":[\"1\",2]}},"
                + "\"7\":{\"class_type\":\"SaveImage\",\"inputs\":{\"filename_prefix\":\"" + esc(request.filenamePrefix()) + "\",\"images\":[\"6\",0]}}"
                + "}}";
    }

    private static String flux(VisualEngineRequest request, ComfyUiWorkflowSpec workflow) {
        int width = workflow.generationWidth(request);
        int height = workflow.generationHeight(request);
        long seed = positiveSeed();
        return "{\"prompt\":{" 
                + "\"1\":{\"class_type\":\"UNETLoader\",\"inputs\":{\"unet_name\":\"" + esc(workflow.modelName()) + "\",\"weight_dtype\":\"" + esc(workflow.modelWeightDtype()) + "\"}},"
                + "\"2\":{\"class_type\":\"DualCLIPLoader\",\"inputs\":{\"clip_name1\":\"" + esc(workflow.clipLName()) + "\",\"clip_name2\":\"" + esc(workflow.t5Name()) + "\",\"type\":\"flux\",\"device\":\"cpu\"}},"
                + "\"3\":{\"class_type\":\"VAELoader\",\"inputs\":{\"vae_name\":\"" + esc(workflow.vaeName()) + "\"}},"
                + "\"4\":{\"class_type\":\"CLIPTextEncodeFlux\",\"inputs\":{\"clip\":[\"2\",0],\"clip_l\":\"" + esc(request.prompt()) + "\",\"t5xxl\":\"" + esc(request.prompt()) + "\",\"guidance\":" + workflow.guidance() + "}},"
                + "\"5\":{\"class_type\":\"ModelSamplingFlux\",\"inputs\":{\"model\":[\"1\",0],\"max_shift\":1.15,\"base_shift\":0.5,\"width\":" + width + ",\"height\":" + height + "}},"
                + "\"6\":{\"class_type\":\"EmptySD3LatentImage\",\"inputs\":{\"width\":" + width + ",\"height\":" + height + ",\"batch_size\":" + request.batchSize() + "}},"
                + "\"7\":{\"class_type\":\"RandomNoise\",\"inputs\":{\"noise_seed\":" + seed + "}},"
                + "\"8\":{\"class_type\":\"KSamplerSelect\",\"inputs\":{\"sampler_name\":\"euler\"}},"
                + "\"9\":{\"class_type\":\"BasicScheduler\",\"inputs\":{\"model\":[\"5\",0],\"scheduler\":\"simple\",\"steps\":" + request.steps() + ",\"denoise\":1}},"
                + "\"10\":{\"class_type\":\"BasicGuider\",\"inputs\":{\"model\":[\"5\",0],\"conditioning\":[\"4\",0]}},"
                + "\"11\":{\"class_type\":\"SamplerCustomAdvanced\",\"inputs\":{\"noise\":[\"7\",0],\"guider\":[\"10\",0],\"sampler\":[\"8\",0],\"sigmas\":[\"9\",0],\"latent_image\":[\"6\",0]}},"
                + "\"12\":{\"class_type\":\"VAEDecode\",\"inputs\":{\"samples\":[\"11\",0],\"vae\":[\"3\",0]}},"
                + "\"13\":{\"class_type\":\"SaveImage\",\"inputs\":{\"filename_prefix\":\"" + esc(request.filenamePrefix()) + "\",\"images\":[\"12\",0]}}"
                + "}}";
    }

    private static long positiveSeed() {
        return new Random().nextLong() & Long.MAX_VALUE;
    }

    private static String esc(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
