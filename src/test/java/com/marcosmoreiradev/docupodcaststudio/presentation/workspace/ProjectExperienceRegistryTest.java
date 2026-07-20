package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectExperienceRegistryTest {
    @Test
    void everyOfficialProjectModeHasOneComposition() {
        ProjectExperienceRegistry registry = ProjectExperienceRegistry.official();
        assertEquals(ProjectMode.officialModes().size(), registry.experiences().size());
        ProjectMode.officialModes().forEach(registry::require);
    }

    @Test
    void narrativeExplicitlyDelegatesToDocumentWhileProductIsEvolving() {
        ProjectExperience narrative = ProjectExperienceRegistry.official().require(ProjectMode.NARRATIVE_VIDEO);
        assertEquals(ProductMaturity.EVOLVING, narrative.maturity());
        assertEquals(WorkspaceKind.DOCUMENT_READER, narrative.primaryWorkspace());
        assertTrue(narrative.workspaces().contains(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION));
    }
}
