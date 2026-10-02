package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class TheatreSceneryCompositionTest {
    @TempDir Path root;
    @Test void transparentAndSemitransparentPixelsRevealTheStage() throws Exception {
        var background = new BufferedImage(1000,600,BufferedImage.TYPE_INT_RGB);
        var g=background.createGraphics(); g.setColor(Color.BLUE); g.fillRect(0,0,1000,600); g.dispose();
        Path bg=root.resolve("stage.png"); ImageIO.write(background,"png",bg.toFile());
        var figure=new BufferedImage(240,240,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<240;y++) for(int x=120;x<240;x++) figure.setRGB(x,y,0x80ff0000);
        Path person=root.resolve("hero.png"); ImageIO.write(figure,"png",person.toFile());
        var scene=new TheatreSceneryComposition.Composition(bg,List.of(new TheatreSceneryComposition.Figure("hero","Hero",true,person,true)));
        var output=new TheatreSceneryComposition().render(scene,1000,600);
        assertEquals(Color.BLUE.getRGB(),output.getRGB(410,400));
        var mixed=new Color(output.getRGB(550,400));
        assertTrue(mixed.getRed()>=127 && mixed.getRed()<=129);
        assertTrue(mixed.getBlue()>=126 && mixed.getBlue()<=128);
        assertEquals(0,mixed.getGreen());
    }
    @Test void missingFigureHasBlackPlaceholder() {
        var scene=new TheatreSceneryComposition.Composition(null,List.of(new TheatreSceneryComposition.Figure("hero","Hero",true,null,false)));
        var output=new TheatreSceneryComposition().render(scene,1000,600);
        assertEquals(Color.BLACK.getRGB(),output.getRGB(400,350));
    }
    @Test void solidDirectionArrowConnectsSpeakerToConcreteRecipient() {
        var figures = List.of(
                new TheatreSceneryComposition.Figure("speaker", "Habla", true, null, false, false),
                new TheatreSceneryComposition.Figure("listener", "Escucha", false, null, false, false));
        var output = new TheatreSceneryComposition().render(
                new TheatreSceneryComposition.Composition(null, figures), 1000, 600);
        assertTrue(countCreamPixels(output, 70, 145) > 100,
                "La flecha rellena debe ser claramente visible entre ambos personajes");
    }
    @Test void publicRecipientDoesNotDrawDirectionArrow() {
        var figures = List.of(
                new TheatreSceneryComposition.Figure("speaker", "Habla", true, null, false, false),
                new TheatreSceneryComposition.Figure("public", "Público", false, null, false, true));
        var output = new TheatreSceneryComposition().render(
                new TheatreSceneryComposition.Composition(null, figures), 1000, 600);
        assertEquals(0, countCreamPixels(output, 70, 145));
    }
    @Test void moreThanFourRecipientsBecomeOneCollectiveMarker() {
        var figures = List.of(
                new TheatreSceneryComposition.Figure("speaker", "Concha", true, null, false, false),
                new TheatreSceneryComposition.Figure("one", "Uno", false, null, false, false),
                new TheatreSceneryComposition.Figure("two", "Dos", false, null, false, false),
                new TheatreSceneryComposition.Figure("three", "Tres", false, null, false, false),
                new TheatreSceneryComposition.Figure("four", "Cuatro", false, null, false, false),
                new TheatreSceneryComposition.Figure("five", "Cinco", false, null, false, false));
        var compact = TheatreSceneryComposition.compactRecipients(figures, "Todos los personajes restantes");
        assertEquals(2, compact.size());
        assertTrue(compact.get(1).collective());
        assertEquals("Todos los personajes restantes", compact.get(1).name());
    }
    @Test void upToFourRecipientsRemainVisibleIndividually() {
        var figures = List.of(
                new TheatreSceneryComposition.Figure("speaker", "Concha", true, null, false, false),
                new TheatreSceneryComposition.Figure("one", "Uno", false, null, false, false),
                new TheatreSceneryComposition.Figure("two", "Dos", false, null, false, false),
                new TheatreSceneryComposition.Figure("three", "Tres", false, null, false, false),
                new TheatreSceneryComposition.Figure("four", "Cuatro", false, null, false, false));
        assertEquals(figures, TheatreSceneryComposition.compactRecipients(figures, "Uno, Dos, Tres, Cuatro"));
    }
    @Test void offstageLocationsAreRecognizedWithoutDependingOnOnePlay() {
        assertTrue(TheatreSceneryComposition.isOffstage("fuera_escena"));
        assertTrue(TheatreSceneryComposition.isOffstage("Fuera de escena"));
        assertTrue(TheatreSceneryComposition.isOffstage("offstage"));
        assertFalse(TheatreSceneryComposition.isOffstage("centro"));
    }
    @Test void longCaptionIsPaginatedWithoutLosingWords() {
        String text="El escenario cambia: seguimos hablando con claridad. ".repeat(60).strip();
        var pages=BuildTheatreSpatialVideoPlanUseCase.sceneryCaptionPages(text,1280,720);
        assertTrue(pages.size()>1);
        assertEquals(text,String.join(" ",pages));
    }

    private static int countCreamPixels(BufferedImage image, int top, int bottom) {
        int count = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y));
                if (pixel.getRed() > 235 && pixel.getGreen() > 215 && pixel.getBlue() > 175) count++;
            }
        }
        return count;
    }
}
