package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppCommandRegistryTest {
    @Test
    void officialCatalogAssignsOwnersAndPrimarySurfaces() {
        AppCommandRegistry registry = AppCommandRegistry.official();

        AppCommandDescriptor listen = registry.descriptor(AppCommandId.LISTEN_DOCUMENT);
        assertEquals(AppCommandOwner.READING, listen.owner());
        assertEquals(AppCommandSurface.WORKSPACE_PLAYBAR, listen.primarySurface());
        assertTrue(listen.allowedOn(AppCommandSurface.MENU_BAR));
        assertFalse(listen.allowedOn(AppCommandSurface.RIBBON));

        AppCommandDescriptor importAudio = registry.descriptor(AppCommandId.IMPORT_AUDIO_FOR_SELECTION);
        assertEquals(AppCommandOwner.MEDIA, importAudio.owner());
        assertEquals(AppCommandSurface.LEFT_SIDEBAR, importAudio.primarySurface());
        assertFalse(importAudio.allowedOn(AppCommandSurface.RIBBON));

        AppCommandDescriptor fullscreen = registry.descriptor(AppCommandId.TOGGLE_FULLSCREEN);
        assertEquals(AppCommandOwner.VIEW, fullscreen.owner());
        assertEquals(AppCommandSurface.MENU_BAR, fullscreen.primarySurface());
        assertFalse(fullscreen.allowedOn(AppCommandSurface.TOOLBAR));
    }

    @Test
    void visibleCommandsAreImplementedAndHaveUniqueIds() {
        AppCommandRegistry registry = AppCommandRegistry.official();
        EnumSet<AppCommandId> ids = EnumSet.noneOf(AppCommandId.class);
        for (AppCommandDescriptor command : registry.all()) {
            assertTrue(ids.add(command.id()), "Comando duplicado: " + command.id());
            if (command.visibleByDefault()) {
                assertTrue(command.implemented(), "Comando visible sin implementar: " + command.id());
            }
        }
        assertTrue(registry.all().size() >= 35, "El catalogo debe cubrir comandos reales de producto antes de rediseñar GUI.");
    }
}
