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

    private ComfyWorkflowTemplate() { }

    static String image(Path template, String prompt, String negativePrompt, int width, int height,
                        long seed, int batchSize, String prefix) throws IOException {
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
}
