package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;

import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Map;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class DocumentIllustrationCompositionTest {
    @TempDir Path root;
    @Test void illustrationOccupiesItsOwnCenteredRegionBelowReadableText() throws Exception {
        var fixture = new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
        var g = fixture.createGraphics();
        g.setColor(Color.MAGENTA); g.fillRect(0,0,512,512); g.dispose();
        Path asset = root.resolve("motif.png");
        ImageIO.write(fixture,"png",asset.toFile());
        Path frame = root.resolve("frame.png");
        String text = "Los árboles capturan carbono y ayudan a regular el clima. Sus raíces protegen el suelo.";
        var options = DocumentTextVideoOptions.defaults();
        new DocumentStudySlideCompositor().composeIllustratedParagraph(frame,
                DocumentBlock.of("B1",DocumentBlockType.PARAGRAPH,text,"Word",Map.of()), text,
                DocumentParagraphVisualAssignment.empty("B1"), DocumentStudyVideoConfiguration.empty().withTitle("Bosques"),
                DocuPodcastProject.createNew("Bosques"),root,options,asset);
        var rendered = ImageIO.read(frame.toFile());
        int minX=rendered.getWidth(), maxX=0, minY=rendered.getHeight(), maxY=0;
        int textPixels=0;
        for(int y=0; y<rendered.getHeight(); y++) for(int x=0; x<rendered.getWidth(); x++) {
            int rgb=rendered.getRGB(x,y)&0xffffff;
            if(rgb==0xff00ff) { minX=Math.min(minX,x); maxX=Math.max(maxX,x); minY=Math.min(minY,y); maxY=Math.max(maxY,y); }
            if(x<rendered.getWidth()/2 && rgb!=0xffffff) textPixels++;
        }
        assertTrue(maxX>minX);
        assertEquals(rendered.getWidth()/2.0, (minX+maxX)/2.0, 1, "Center the illustration horizontally");
        assertTrue(minY>rendered.getHeight()/3, "Image must occupy the lower region");
        assertTrue(maxX-minX<512, "Do not upscale a small illustration");
        assertTrue(textPixels>100, "Text must remain visible");
        assertEquals(maxX-minX,maxY-minY,1, "Preserve the motif aspect ratio");
        Path preview=Path.of("target","document-illustration-composition.png");
        Files.createDirectories(preview.getParent());
        Files.copy(frame,preview,StandardCopyOption.REPLACE_EXISTING);
    }
}
