package com.marcosmoreiradev.docupodcaststudio.application.visual;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/** Validates the portable guarantees shared by every visual consumer. */
public final class BasicVisualFileValidator implements VisualGenerationValidator {
    @Override
    public VisualGenerationValidationReport validate(VisualGenerationResult result) {
        ArrayList<String> issues = new ArrayList<>();
        if (result == null || result.outputPath() == null || !Files.isRegularFile(result.outputPath())) {
            issues.add("La salida visual no existe.");
            return new VisualGenerationValidationReport(false, issues);
        }
        try {
            BufferedImage image = ImageIO.read(result.outputPath().toFile());
            if (image == null) {
                issues.add("La salida no es una imagen valida.");
            } else if (image.getWidth() < 64 || image.getHeight() < 64) {
                issues.add("La resolucion generada es demasiado pequena.");
            }
        } catch (IOException ex) {
            issues.add("No se pudo leer la salida visual: " + ex.getMessage());
        }
        return new VisualGenerationValidationReport(issues.isEmpty(), issues);
    }
}
