package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationMemoryProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;

import java.util.ArrayList;
import java.util.List;

/** Builds ComfyUI launch arguments from user intent plus probed runtime capabilities. */
public final class ComfyUiLaunchArgumentPlanner {
    public List<String> memoryArguments(ImageGenerationSettings settings, ComfyUiRuntimeCapabilities capabilities) {
        ImageGenerationSettings current = settings == null ? ImageGenerationSettings.defaults() : settings;
        ImageGenerationMemoryProfile profile = current.memoryProfileValue();
        ComfyUiRuntimeCapabilities support = capabilities == null
                ? ComfyUiRuntimeCapabilities.unknown("sin probe")
                : capabilities;
        ArrayList<String> args = new ArrayList<>();
        if (profile == ImageGenerationMemoryProfile.SAFE_LOW_VRAM) {
            addIfSupported(args, support, "--lowvram", true);
        } else if (profile == ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD) {
            addIfSupported(args, support, "--novram", true);
            addIfSupported(args, support, "--cpu-vae", false);
            addIfSupported(args, support, "--disable-smart-memory", false);
            addIfSupported(args, support, "--fp8_e4m3fn-unet", false);
            addIfSupported(args, support, "--fp8_e4m3fn-text-enc", false);
        } else if (profile == ImageGenerationMemoryProfile.HIGH_MEMORY) {
            addIfSupported(args, support, "--highvram", false);
        }
        return List.copyOf(args);
    }

    public String diagnostic(ImageGenerationSettings settings, ComfyUiRuntimeCapabilities capabilities) {
        ImageGenerationSettings current = settings == null ? ImageGenerationSettings.defaults() : settings;
        ComfyUiRuntimeCapabilities support = capabilities == null
                ? ComfyUiRuntimeCapabilities.unknown("sin probe")
                : capabilities;
        return "memoryProfile=" + current.memoryProfile()
                + "\nmemoryProfileLabel=" + current.memoryProfileValue().displayName()
                + "\nlowVramLegacy=" + current.lowVram()
                + "\ncomfyHelpProbed=" + support.probed()
                + (support.diagnostic().isBlank() ? "" : "\n" + support.diagnostic());
    }

    private static void addIfSupported(List<String> args, ComfyUiRuntimeCapabilities support, String flag, boolean fallbackWhenUnknown) {
        if (support.supports(flag) || (!support.probed() && fallbackWhenUnknown)) {
            args.add(flag);
        }
    }
}
