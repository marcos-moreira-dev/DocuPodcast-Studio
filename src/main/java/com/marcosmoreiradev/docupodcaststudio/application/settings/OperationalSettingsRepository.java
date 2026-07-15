package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.io.IOException;

/** Persistence port for desktop operational settings. */
public interface OperationalSettingsRepository {
    OperationalSettings load() throws IOException;
    void save(OperationalSettings settings) throws IOException;
}
