package com.marcosmoreiradev.docupodcaststudio.architecture;

import com.marcosmoreiradev.docupodcaststudio.DocuPodcastStudioApp;
import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModuleSurfaceTest {

    @Test
    void onlyCompositionEntryPointsAreExported() {
        ModuleDescriptor descriptor = DocuPodcastStudioApp.class.getModule().getDescriptor();
        Set<String> exportedPackages = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "com.marcosmoreiradev.docupodcaststudio",
                "com.marcosmoreiradev.docupodcaststudio.bootstrap"
        ), exportedPackages);
    }
}
