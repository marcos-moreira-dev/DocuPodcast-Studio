package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentStudySemanticSlidePolicyTest {
    @TempDir Path root;

    @Test
    void visualSemanticSlideShowsSourceOnlyEvenWhenSpeechIsHuge() throws Exception {
        BufferedImage source = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        var sg = source.createGraphics(); sg.setColor(new Color(20, 110, 210));
        sg.fillRect(0, 0, source.getWidth(), source.getHeight()); sg.dispose();
        Path roi = root.resolve("roi.png"); ImageIO.write(source, "png", roi.toFile());
        DocumentBlock block = DocumentBlock.of("REGION-1", DocumentBlockType.MATH_NOTICE,
                "SPEECH que no debe imprimirse ".repeat(2_000), "pdf", Map.of("contentKind", "EQUATION"));
        Path output = root.resolve("slide.png");

        new DocumentStudySlideCompositor().composeSemanticComponent(output, block, roi,
                DocumentParagraphVisualAssignment.empty(block.id()),
                DocumentStudyVideoConfiguration.empty(), DocuPodcastProject.createNew("test"),
                root, DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.LOW_540));

        BufferedImage slide = ImageIO.read(output.toFile());
        assertEquals(new Color(20, 110, 210),
                new Color(slide.getRGB(slide.getWidth() / 2, slide.getHeight() / 2)));
    }

    @Test
    void sourceCapturePrioritizesCanonicalRoiOverConfiguredIllustration() throws Exception {
        BufferedImage source = solid(600, 300, new Color(20, 110, 210));
        BufferedImage replacement = solid(600, 300, new Color(220, 80, 20));
        Path roi = root.resolve("roi-authority.png");
        Path imported = root.resolve("replacement.png");
        ImageIO.write(source, "png", roi.toFile());
        ImageIO.write(replacement, "png", imported.toFile());
        var asset = new com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference(
                "IMG-REPLACEMENT",
                com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind.IMAGE,
                "replacement", "replacement.png", "image/png", "test", "", "");
        DocuPodcastProject project = DocuPodcastProject.createNew("test").withAsset(asset);
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "REGION-1", "fp", "IMG-REPLACEMENT", "", "",
                com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource.IMPORTED,
                "", null);
        DocumentBlock block = DocumentBlock.of("REGION-1", DocumentBlockType.IMAGE_NOTICE,
                "Speech invisible", "pdf");
        Path output = root.resolve("source-authority.png");

        new DocumentStudySlideCompositor().composeSourceCapture(output, block, roi,
                visual, DocumentStudyVideoConfiguration.empty(), project, root,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.LOW_540));

        BufferedImage slide = ImageIO.read(output.toFile());
        assertEquals(new Color(20, 110, 210),
                new Color(slide.getRGB(slide.getWidth() / 2, slide.getHeight() / 2)));
    }

    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }
}
