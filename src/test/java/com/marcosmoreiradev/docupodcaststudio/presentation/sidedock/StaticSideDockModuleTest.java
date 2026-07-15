package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import javafx.scene.Group;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class StaticSideDockModuleTest {
    @Test
    void keepsTheSameViewWhenTheUserAlternatesModules() {
        AtomicInteger creations = new AtomicInteger();
        StaticSideDockModule module = StaticSideDockModule.of(
                SideDockModuleId.DOCUMENT_IMAGE,
                "Imagen",
                "Imagen",
                "I",
                () -> {
                    creations.incrementAndGet();
                    return new Group();
                });

        Parent first = module.createView(null);
        Parent second = module.createView(null);

        assertSame(first, second);
        assertEquals(1, creations.get());
    }
}
