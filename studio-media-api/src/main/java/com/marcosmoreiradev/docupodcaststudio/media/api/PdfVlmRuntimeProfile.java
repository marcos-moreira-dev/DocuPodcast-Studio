package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Runtime-only PDF visual model profile; it never participates in PDF domain identity. */
public record PdfVlmRuntimeProfile(
        String model,
        int contextTokens,
        int maxOutputTokens,
        int batchSize,
        String kvCacheType,
        boolean flashAttention,
        long modelHostMib,
        long modelVramMib,
        long marginalHostMib,
        long marginalVramMib) {
    public static final String MODEL_4B_Q8 = "qwen3-vl:4b-instruct-q8_0";
    public static final String MODEL_8B_Q8 = "qwen3-vl:8b-instruct-q8_0";

    public PdfVlmRuntimeProfile {
        model = require(model, "model");
        contextTokens = positive(contextTokens, "contextTokens");
        maxOutputTokens = positive(maxOutputTokens, "maxOutputTokens");
        batchSize = positive(batchSize, "batchSize");
        kvCacheType = require(kvCacheType, "kvCacheType").toLowerCase();
        modelHostMib = positive(modelHostMib, "modelHostMib");
        modelVramMib = nonNegative(modelVramMib, "modelVramMib");
        marginalHostMib = positive(marginalHostMib, "marginalHostMib");
        marginalVramMib = nonNegative(marginalVramMib, "marginalVramMib");
    }

    public static PdfVlmRuntimeProfile defaults() {
        return forModel(MODEL_4B_Q8);
    }

    public static PdfVlmRuntimeProfile forModel(String model) {
        boolean eightB = Objects.toString(model, "").contains(":8b-");
        return new PdfVlmRuntimeProfile(model, 8192, 4096, 512, "q8_0", true,
                // Scheduler host residency excludes the portion kept in VRAM;
                // total process working set is measured separately by telemetry.
                eightB ? 7_475 : 4_000,
                eightB ? 2_300 : 1_800,
                eightB ? 640 : 512,
                eightB ? 512 : 384);
    }

    public static PdfVlmRuntimeProfile fromSystem() {
        return from(System.getProperties(), System.getenv());
    }

    public static PdfVlmRuntimeProfile from(Properties properties,
                                             Map<String, String> environment) {
        Properties props = properties == null ? new Properties() : properties;
        Map<String, String> env = environment == null ? Map.of() : environment;
        String model = configured(props, env, "docupodcast.pdfVlm.model",
                "PDF_VLM_MODEL", MODEL_4B_Q8);
        PdfVlmRuntimeProfile base = forModel(model);
        return new PdfVlmRuntimeProfile(model,
                integer(props, env, "docupodcast.pdfVlm.context", "PDF_VLM_CONTEXT",
                        base.contextTokens()),
                integer(props, env, "docupodcast.pdfVlm.numPredict", "PDF_VLM_NUM_PREDICT",
                        base.maxOutputTokens()),
                integer(props, env, "docupodcast.pdfVlm.batch", "PDF_VLM_BATCH",
                        base.batchSize()),
                configured(props, env, "docupodcast.pdfVlm.kvCache", "PDF_VLM_KV_CACHE",
                        base.kvCacheType()),
                bool(props, env, "docupodcast.pdfVlm.flashAttention",
                        "PDF_VLM_FLASH_ATTENTION", base.flashAttention()),
                number(props, env, "docupodcast.pdfVlm.modelHostMib",
                        "PDF_VLM_MODEL_HOST_MIB", base.modelHostMib()),
                number(props, env, "docupodcast.pdfVlm.modelVramMib",
                        "PDF_VLM_MODEL_VRAM_MIB", base.modelVramMib()),
                number(props, env, "docupodcast.pdfVlm.marginalHostMib",
                        "PDF_VLM_MARGINAL_HOST_MIB", base.marginalHostMib()),
                number(props, env, "docupodcast.pdfVlm.marginalVramMib",
                        "PDF_VLM_MARGINAL_VRAM_MIB", base.marginalVramMib()));
    }

    public Map<String, String> requestOptions() {
        return Map.of("model", model,
                "contextWindowTokens", Integer.toString(contextTokens),
                "maxOutputTokens", Integer.toString(maxOutputTokens),
                "batchSize", Integer.toString(batchSize),
                "kvCacheType", kvCacheType,
                "flashAttention", Boolean.toString(flashAttention),
                "modelHostMib", Long.toString(modelHostMib),
                "modelVramMib", Long.toString(modelVramMib),
                "marginalHostMib", Long.toString(marginalHostMib),
                "marginalVramMib", Long.toString(marginalVramMib));
    }

    private static String configured(Properties props, Map<String, String> env,
                                     String property, String variable, String fallback) {
        String value = props.getProperty(property, "").strip();
        if (value.isBlank()) value = Objects.toString(env.get(variable), "").strip();
        return value.isBlank() ? fallback : value;
    }

    private static int integer(Properties props, Map<String, String> env,
                               String property, String variable, int fallback) {
        return Math.toIntExact(number(props, env, property, variable, fallback));
    }

    private static long number(Properties props, Map<String, String> env,
                               String property, String variable, long fallback) {
        String value = configured(props, env, property, variable, Long.toString(fallback));
        try { return Long.parseLong(value); }
        catch (NumberFormatException failure) {
            throw new IllegalArgumentException(property + " must be an integer", failure);
        }
    }

    private static boolean bool(Properties props, Map<String, String> env,
                                String property, String variable, boolean fallback) {
        String value = configured(props, env, property, variable, Boolean.toString(fallback));
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(property + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static String require(String value, String name) {
        String normalized = Objects.toString(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " is required");
        return normalized;
    }

    private static int positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static long positive(long value, String name) {
        if (value <= 0L) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static long nonNegative(long value, String name) {
        if (value < 0L) throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }
}
