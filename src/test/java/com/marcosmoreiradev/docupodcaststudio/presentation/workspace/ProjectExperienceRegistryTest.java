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
        assertTrue(narrative.requires(ProductRequirements.MEDIA_CANDIDATE_REVIEW));
        assertTrue(narrative.requires(ProductRequirements.RECOVERABLE_JOBS));
    }

    @Test
    void infrastructureNeedsComeFromProductComplexity() {
        ProjectExperienceRegistry registry = ProjectExperienceRegistry.official();
        ProjectExperience documentary = registry.require(ProjectMode.DOCUMENTARY_STUDIO);
        ProjectExperience theatre = registry.require(ProjectMode.THEATRE_PRODUCTION);

        assertTrue(documentary.requires(ProductRequirements.DOCUMENT_PAGED));
        assertTrue(documentary.requires(ProductRequirements.INK_EDITING));
        assertTrue(theatre.requires(ProductRequirements.PRODUCTION_BOARD));
        assertTrue(theatre.requires(ProductRequirements.MEDIA_CANDIDATE_REVIEW));
    }
}
