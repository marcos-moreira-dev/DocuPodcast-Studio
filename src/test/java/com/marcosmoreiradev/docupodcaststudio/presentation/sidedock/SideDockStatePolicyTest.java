package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SideDockStatePolicyTest {
    @Test
    void keepsPreviousModuleWhenStillAvailable() {
        SideDockModule audio = module(SideDockModuleId.DOCUMENT_AUDIO_NARRATION);
        SideDockModule details = module(SideDockModuleId.DOCUMENT_CONTEXT_DETAILS);

        assertEquals(SideDockModuleId.DOCUMENT_AUDIO_NARRATION,
                new SideDockStatePolicy().choose(SideDockModuleId.DOCUMENT_AUDIO_NARRATION, List.of(details, audio)));
    }

    @Test
    void choosesDetailsAsDefaultContextModule() {
        SideDockModule audio = module(SideDockModuleId.DOCUMENT_AUDIO_NARRATION);
        SideDockModule details = module(SideDockModuleId.DOCUMENT_CONTEXT_DETAILS);

        assertEquals(SideDockModuleId.DOCUMENT_CONTEXT_DETAILS,
                new SideDockStatePolicy().choose(null, List.of(audio, details)));
    }

    private static SideDockModule module(SideDockModuleId id) {
        return new SideDockModule() {
            @Override public SideDockModuleId id() { return id; }
            @Override public String title() { return id.displayName(); }
            @Override public String tooltip() { return id.displayName(); }
            @Override public String iconText() { return id.displayName().substring(0, 1); }
            @Override public boolean supports(SideDockContext context) { return true; }
            @Override public Parent createView(SideDockContext context) { return new VBox(); }
        };
    }
}
