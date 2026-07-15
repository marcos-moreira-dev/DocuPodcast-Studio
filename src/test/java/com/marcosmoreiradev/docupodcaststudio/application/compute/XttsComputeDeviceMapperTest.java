package com.marcosmoreiradev.docupodcaststudio.application.compute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XttsComputeDeviceMapperTest {
    @Test
    void preservesManualDeviceIntentForVoiceWrappers() {
        assertEquals("cpu", XttsComputeDeviceMapper.toXttsDeviceArgument(ComputeDevicePolicy.CPU_ONLY, "cpu"));
        assertEquals("gpu-nvidia-0", XttsComputeDeviceMapper.toXttsDeviceArgument(ComputeDevicePolicy.SPECIFIC_DEVICE, "gpu-nvidia-0"));
        assertEquals("gpu-nvidia-2", XttsComputeDeviceMapper.toXttsDeviceArgument(ComputeDevicePolicy.SPECIFIC_DEVICE, "gpu-nvidia-2"));
    }

    @Test
    void preservesAmdAndIntelManualIntentInsteadOfForcingCpu() {
        assertEquals("gpu-amd-0", XttsComputeDeviceMapper.toXttsDeviceArgument(ComputeDevicePolicy.SPECIFIC_DEVICE, "gpu-amd-0"));
        assertEquals("gpu-intel-0", XttsComputeDeviceMapper.toXttsDeviceArgument(ComputeDevicePolicy.SPECIFIC_DEVICE, "gpu-intel-0"));
    }

    @Test
    void mapsNvidiaToCudaOnlyForCudaSmokeProbe() {
        assertEquals("cuda:0", ComputeDeviceArgumentMapper.toCudaDeviceArgument("gpu-nvidia-0"));
        assertEquals("cuda:2", ComputeDeviceArgumentMapper.toCudaDeviceArgument("gpu-nvidia-2"));
    }
}
