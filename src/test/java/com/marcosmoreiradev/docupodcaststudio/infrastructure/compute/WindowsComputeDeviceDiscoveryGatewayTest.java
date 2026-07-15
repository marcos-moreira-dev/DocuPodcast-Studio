package com.marcosmoreiradev.docupodcaststudio.infrastructure.compute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WindowsComputeDeviceDiscoveryGatewayTest {
    @Test
    void parsesWindowsVideoControllerNamesByVendor() {
        WindowsComputeDeviceDiscoveryGateway gateway = new WindowsComputeDeviceDiscoveryGateway();
        var devices = gateway.parseWindowsVideoControllerOutput("NVIDIA GeForce RTX 4060\nAMD Radeon Graphics\nIntel Iris Xe Graphics");

        assertEquals(3, devices.size());
        assertTrue(devices.stream().anyMatch(device -> device.id().equals("gpu-nvidia-0")));
        assertTrue(devices.stream().anyMatch(device -> device.id().equals("gpu-amd-0")));
        assertTrue(devices.stream().anyMatch(device -> device.id().equals("gpu-intel-0")));
    }
}
