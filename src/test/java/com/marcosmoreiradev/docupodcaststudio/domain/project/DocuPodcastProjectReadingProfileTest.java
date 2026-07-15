package com.marcosmoreiradev.docupodcaststudio.domain.project;

import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocuPodcastProjectReadingProfileTest {
    @Test
    void projectStoresActiveReadingProfile() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Notas");
        ReadingProfile custom = new ReadingProfile(
                project.readingProfile().id(),
                "Perfil personalizado",
                "Persistible",
                project.readingProfile().headingRules(),
                ImageNarrationPolicy.IGNORE_IMAGES,
                project.readingProfile().tablePolicy()
        );

        DocuPodcastProject updated = project.withReadingProfile(custom);

        assertEquals("Perfil personalizado", updated.readingProfile().name());
        assertEquals(ImageNarrationPolicy.IGNORE_IMAGES, updated.readingProfile().imagePolicy());
    }
}
