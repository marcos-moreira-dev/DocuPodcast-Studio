package com.marcosmoreiradev.docupodcaststudio.architecture;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RibbonIconCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.ribbon.RibbonDefinitionCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class RibbonProductIconCoverageTest {
    @Test
    void everyOfficialRibbonCommandHasAnExplicitProductIcon() {
        for (ProjectMode mode : ProjectMode.values()) {
            RibbonDefinitionCatalog.officialTabs(mode).stream()
                    .flatMap(tab -> tab.groups().stream())
                    .flatMap(group -> group.commands().stream())
                    .forEach(command -> assertNotEquals(
                            AppIcon.DEFAULT,
                            RibbonIconCatalog.iconFor(command.commandId()),
                            () -> "Missing product icon for " + command.commandId()));
        }
    }
}
