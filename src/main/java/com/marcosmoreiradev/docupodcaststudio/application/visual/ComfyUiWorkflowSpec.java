package com.marcosmoreiradev.docupodcaststudio.application.visual;

/** Assets and native generation geometry required by a built-in ComfyUI workflow. */
public record ComfyUiWorkflowSpec(
        ComfyUiWorkflowKind kind,
        String modelName,
        String vaeName,
        String clipLName,
        String t5Name,
        String modelWeightDtype,
        double guidance,
        int nativeWidth,
        int nativeHeight
) {
    public ComfyUiWorkflowSpec {
        kind = kind == null ? ComfyUiWorkflowKind.SD15_CHECKPOINT : kind;
        modelName = clean(modelName);
        vaeName = clean(vaeName);
        clipLName = clean(clipLName);
        t5Name = clean(t5Name);
        modelWeightDtype = clean(modelWeightDtype).isBlank() ? "default" : clean(modelWeightDtype);
        guidance = guidance <= 0 ? 3.5 : guidance;
        nativeWidth = roundToSixteen(nativeWidth);
        nativeHeight = roundToSixteen(nativeHeight);
    }

    public static ComfyUiWorkflowSpec sd15() {
        return new ComfyUiWorkflowSpec(ComfyUiWorkflowKind.SD15_CHECKPOINT,
                "", "", "", "", "default", 3.5, 0, 0);
    }

    public static ComfyUiWorkflowSpec flux(String modelName, String vaeName, String clipLName, String t5Name,
                                           int nativeWidth, int nativeHeight) {
        return new ComfyUiWorkflowSpec(ComfyUiWorkflowKind.FLUX1_DEV_COMPONENTS,
                modelName, vaeName, clipLName, t5Name, "fp8_e4m3fn", 3.5,
                nativeWidth, nativeHeight);
    }

    public static ComfyUiWorkflowSpec fluxForTarget(String modelName, String vaeName, String clipLName, String t5Name,
                                                    int targetWidth, int targetHeight) {
        int[] nativeSize = fluxNativeSize(targetWidth, targetHeight);
        return flux(modelName, vaeName, clipLName, t5Name, nativeSize[0], nativeSize[1]);
    }

    public int generationWidth(VisualEngineRequest request) {
        return nativeWidth > 0 ? nativeWidth : request.generationWidth();
    }

    public int generationHeight(VisualEngineRequest request) {
        return nativeHeight > 0 ? nativeHeight : request.generationHeight();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static int roundToSixteen(int value) {
        return value <= 0 ? 0 : Math.max(16, Math.round(value / 16.0f) * 16);
    }

    private static int[] fluxNativeSize(int targetWidth, int targetHeight) {
        if (targetWidth == targetHeight) {
            return new int[] {1024, 1024};
        }
        double ratio = targetWidth / (double) targetHeight;
        if (Math.abs(ratio - 16.0 / 9.0) < 0.05) {
            return new int[] {1344, 768};
        }
        if (Math.abs(ratio - 9.0 / 16.0) < 0.05) {
            return new int[] {768, 1344};
        }
        int width = roundToSixteen((int) Math.round(Math.sqrt(1_048_576.0 * ratio)));
        int height = roundToSixteen((int) Math.round(width / ratio));
        return new int[] {width, height};
    }
}
