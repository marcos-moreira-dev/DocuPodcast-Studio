package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StudyRibbonT93SourceTest {
    @Test
    void documentaryStudyTabOnlyShowsTechnicalProblemCommands() {
        RibbonTabDefinition study = RibbonDefinitionCatalog.officialTabs(ProjectMode.DOCUMENTARY_STUDIO).stream()
                .filter(tab -> tab.id().equals("estudio"))
                .findFirst()
                .orElseThrow();

        List<AppCommandId> commands = study.groups().stream()
                .flatMap(group -> group.commands().stream())
                .map(RibbonCommandDefinition::commandId)
                .toList();

        assertEquals(List.of(AppCommandId.PREPARE_TECHNICAL_PROBLEM, AppCommandId.TOGGLE_DOCUMENT_PLAYBAR_DOCK), commands);
    }
}
