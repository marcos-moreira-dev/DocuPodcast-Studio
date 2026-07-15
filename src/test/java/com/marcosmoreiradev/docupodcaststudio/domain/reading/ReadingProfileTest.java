package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReadingProfileTest {
    @Test
    void defaultProfileIsWordFirst() {
        ReadingProfile profile = ReadingProfile.academicDefaults();
        assertTrue(profile.name().contains("Word"));
        assertEquals(ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT, profile.imagePolicy());
        assertTrue(profile.headingRules().matchesHeadingStyle("Heading 1"));
        assertTrue(profile.headingRules().matchesSubheadingStyle("Título 2"));
    }
}
