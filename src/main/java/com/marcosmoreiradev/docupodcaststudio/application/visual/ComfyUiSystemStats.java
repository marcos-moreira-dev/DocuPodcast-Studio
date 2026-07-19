package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal structured projection of ComfyUI /system_stats. */
public record ComfyUiSystemStats(
        String os,
        String pythonVersion,
        String pytorchVersion,
        List<Device> devices,
        String rawJson
) {
    private static final Pattern OBJECT = Pattern.compile("\\{([^{}]+)}");

    public ComfyUiSystemStats {
        os = clean(os);
        pythonVersion = clean(pythonVersion);
        pytorchVersion = clean(pytorchVersion);
        devices = devices == null ? List.of() : List.copyOf(devices);
        rawJson = clean(rawJson);
    }

    public static ComfyUiSystemStats parse(String json) {
        String body = json == null ? "" : json;
        ArrayList<Device> devices = new ArrayList<>();
        Matcher objects = OBJECT.matcher(arrayBody(body, "devices"));
        while (objects.find()) {
            String object = objects.group();
            String name = stringField(object, "name");
            String type = stringField(object, "type");
            int index = intField(object, "index", devices.size());
            if (!name.isBlank() || !type.isBlank()) {
                devices.add(new Device(name, type, index));
            }
        }
        return new ComfyUiSystemStats(
                stringField(body, "os"),
                stringField(body, "python_version"),
                stringField(body, "pytorch_version"),
                devices,
                body);
    }

    public record Device(String name, String type, int index) {
        public Device {
            name = clean(name);
            type = clean(type).toLowerCase(Locale.ROOT);
            index = Math.max(0, index);
        }
    }

    private static String arrayBody(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\\[(.*?)]",
                Pattern.DOTALL).matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String stringField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"", Pattern.DOTALL).matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : "";
    }

    private static int intField(String json, String field, int fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
