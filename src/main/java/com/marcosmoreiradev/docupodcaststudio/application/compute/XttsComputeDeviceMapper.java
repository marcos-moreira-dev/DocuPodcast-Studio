package com.marcosmoreiradev.docupodcaststudio.application.compute;

/** Backward-compatible facade for voice-device argument mapping. */
public final class XttsComputeDeviceMapper {
    private XttsComputeDeviceMapper() {
    }

    public static String toXttsDeviceArgument(ComputeDevicePolicy policy, String selectedDeviceId) {
        return ComputeDeviceArgumentMapper.toProcessDeviceArgument(policy, selectedDeviceId);
    }

    public static String gpuIndex(String selectedDeviceId) {
        return ComputeDeviceArgumentMapper.gpuIndex(selectedDeviceId);
    }
}
