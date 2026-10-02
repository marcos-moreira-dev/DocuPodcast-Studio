package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceEngineResourceDemandTest {
    @Test
    void piperDeclaresItsOwnCpuDemand() {
        PiperVoiceEngine engine = new PiperVoiceEngine(
                new EngineConfiguration(PiperVoiceEngine.ID, Map.of()));

        ComputeResourceDemand demand = engine.resourceDemand(
                ComputePreference.preferGpu(true));

        assertEquals(1, demand.units().get(ResourceId.CPU_HEAVY));
        assertTrue(demand.device().cpu());
        assertNull(demand.modelResidency());
    }

    @Test
    void xttsDeclaresPlacementSpecificModelResidency() {
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, Map.of()));

        ComputeResourceDemand cpu = engine.resourceDemand(
                ComputePreference.cpuOnly(true));
        ComputeResourceDemand gpu = engine.resourceDemand(
                ComputePreference.specificDevice("GPU:NVIDIA:0", true));

        assertEquals("xtts-v2", cpu.modelResidency().key().modelIdentity());
        assertEquals(0L, cpu.modelResidency().vramBytes());
        assertEquals("xtts-v2", gpu.modelResidency().key().modelIdentity());
        assertTrue(gpu.modelResidency().vramBytes() > 0L);
        assertEquals(1, gpu.gpuComputeUnits());
        assertEquals(ComputeDeviceId.gpu("NVIDIA", 0), gpu.device());
    }

    @Test
    void unknownLocalEngineUsesConservativeGenericDemandNotXttsResidency() {
        ComputeResourceDemand demand = new TestVoiceEngine().resourceDemand(
                ComputePreference.automatic());

        assertEquals(1, demand.units().get(ResourceId.CPU_HEAVY));
        assertNull(demand.modelResidency());
        assertEquals(0L, demand.estimatedVramBytes());
    }
}
