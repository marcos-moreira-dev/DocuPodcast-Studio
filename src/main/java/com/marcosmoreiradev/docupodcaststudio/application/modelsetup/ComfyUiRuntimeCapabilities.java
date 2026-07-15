package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Best-effort description of supported ComfyUI command-line arguments. */
public record ComfyUiRuntimeCapabilities(Set<String> supportedFlags, boolean probed, String diagnostic) {
    public ComfyUiRuntimeCapabilities {
        supportedFlags = supportedFlags == null ? Set.of() : Set.copyOf(supportedFlags);
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
    }

    public boolean supports(String flag) {
        return flag != null && supportedFlags.contains(flag.strip());
    }

    public static ComfyUiRuntimeCapabilities unknown(String diagnostic) {
        return new ComfyUiRuntimeCapabilities(Set.of(), false, diagnostic);
    }

    public static ComfyUiRuntimeCapabilities fromHelpText(String text) {
        String help = text == null ? "" : text;
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        for (String token : help.split("[\\s,;=\\[\\](){}]+")) {
            String normalized = token == null ? "" : token.strip().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("--") && normalized.length() > 2) {
                flags.add(normalized);
            }
        }
        return new ComfyUiRuntimeCapabilities(flags, true,
                flags.isEmpty() ? "ComfyUI help no expuso flags reconocibles." : "flags=" + String.join(",", flags));
    }
}
