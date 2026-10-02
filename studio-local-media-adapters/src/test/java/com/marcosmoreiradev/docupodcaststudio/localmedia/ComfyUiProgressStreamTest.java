package com.marcosmoreiradev.docupodcaststudio.localmedia;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ComfyUiProgressStreamTest {
    @Test void reportsOnlyStepsFromItsOwnJob() {
        var stream = new ComfyUiProgressStream("mine");
        stream.accept("""
            {"type":"progress","data":{"prompt_id":"other","value":14,"max":28}}
            """);
        assertFalse(stream.detail().contains("14"));
        stream.accept("""
            {"type":"progress","data":{"prompt_id":"mine","value":14,"max":28}}
            """);
        assertTrue(stream.detail().contains("paso 14 de 28"));
    }
    @Test void malformedAndMissingProgressDoNotInventPercentage() {
        var stream = new ComfyUiProgressStream("mine");
        String initial = stream.detail();
        stream.accept("""
            {"type":"progress","data":{"prompt_id":"mine","value":99,"max":28}}
            """);
        stream.accept("invalid");
        assertEquals(initial, stream.detail());
    }
}
