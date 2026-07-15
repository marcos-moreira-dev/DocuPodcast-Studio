package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Port for discovering local CPU/GPU devices without tying application rules to one OS command. */
public interface ComputeDeviceDiscoveryGateway {
    List<ComputeDeviceDescriptor> discover(Properties properties, Map<String, String> environment, int availableProcessors);
}
