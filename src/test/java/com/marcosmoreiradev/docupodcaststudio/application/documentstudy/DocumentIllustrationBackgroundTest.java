package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class DocumentIllustrationBackgroundTest {
    @TempDir Path root;
    @Test void generatedBackgroundOverridesExistingImageAndAppliesOpacity() throws Exception {
        Path old = solid("old.png", Color.RED);
        Path generated = solid("generated.png", Color.BLUE);
        var options = DocumentTextVideoOptions.defaults().withBackgroundImage(old.toString(), 1.0);
        var config = DocumentStudyVideoConfiguration.empty().withAiIllustrationsEnabled(true)
                .withAiIllustrationAppearance(new DocumentAiIllustrationAppearance(true, .5, "COVER"));
        var block = DocumentBlock.of("B1", DocumentBlockType.PARAGRAPH, "Texto visible", "");
        var visual = DocumentParagraphVisualAssignment.empty("B1");
        var compositor = new DocumentStudySlideCompositor();
        var project = DocuPodcastProject.createNew("Test");
        Path frame = root.resolve("frame.png");
        compositor.composeIllustratedParagraph(frame, block, block.text(), visual, config, project, root, options, generated);
        BufferedImage image = ImageIO.read(frame.toFile());
        Color corner = new Color(image.getRGB(2, 2));
        assertEquals(255, corner.getBlue());
        assertEquals(127, corner.getRed(), 2);
        assertEquals(corner.getRed(), corner.getGreen());
        boolean darkText = false;
        for (int y = 40; y < image.getHeight()/2 && !darkText; y++)
            for (int x = 40; x < image.getWidth()/2; x++) {
                Color c = new Color(image.getRGB(x,y));
                if (c.getRed()<70 && c.getGreen()<70 && c.getBlue()<80) { darkText = true; break; }
            }
        assertTrue(darkText, "Text must remain opaque above the background");
        compositor.composeNarratedParagraph(frame, block, visual, config, project, root, options);
        assertEquals(Color.RED.getRGB(), ImageIO.read(frame.toFile()).getRGB(2,2),
                "Slides without an AI illustration keep their configured background");
    }
    @Test void changingOtherSettingsPreservesAppearance() {
        var appearance = new DocumentAiIllustrationAppearance(true, .65, "BLUR_AND_CONTAIN");
        var config = DocumentStudyVideoConfiguration.empty().withAiIllustrationAppearance(appearance)
                .withTitle("Changed").withAiIllustrationsEnabled(true).withDefaultTableDuration(8)
                .withMusicTracks(java.util.List.of()).withBlockEnabled("B1", false);
        assertEquals(appearance, config.aiIllustrationAppearance());
    }
    private Path solid(String name, Color color) throws Exception {
        BufferedImage image = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics(); g.setColor(color); g.fillRect(0,0,32,32); g.dispose();
        Path file=root.resolve(name); ImageIO.write(image,"png",file.toFile()); return file;
    }
}
