package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;

import java.util.ArrayList;
import java.util.List;

/** Conservative production policy for visual/video jobs on current and future hardware. */
public final class ProductionHardwareProfilePolicy {
    public ProductionHardwareProfileAdvice advise(long detectedVramMb,
                                                  SimpleVideoResolutionPreset requestedResolution,
                                                  int requestedBatchSize) {
        SimpleVideoResolutionPreset resolution = requestedResolution == null
                ? SimpleVideoResolutionPreset.defaultPreset()
                : requestedResolution;
        int batch = Math.max(1, requestedBatchSize);
        ArrayList<String> warnings = new ArrayList<>();
        if (detectedVramMb > 0 && detectedVramMb <= 4096) {
            if (resolution.width() > SimpleVideoResolutionPreset.HD_720.width()
                    || resolution.height() > SimpleVideoResolutionPreset.HD_720.height()) {
                warnings.add("Hardware actual cercano a 4 GB VRAM: usa 720p para evitar fallos por memoria.");
            }
            if (batch > 1) {
                warnings.add("Hardware actual cercano a 4 GB VRAM: usa lotes de 1 candidato visual por vez.");
            }
            return new ProductionHardwareProfileAdvice("4 GB VRAM conservador", detectedVramMb,
                    SimpleVideoResolutionPreset.HD_720, 1, true, false, warnings);
        }
        if (detectedVramMb >= 24576) {
            return new ProductionHardwareProfileAdvice("24 GB VRAM produccion amplia", detectedVramMb,
                    resolution, Math.max(batch, 4), false, true, warnings);
        }
        if (detectedVramMb == 0) {
            warnings.add("No se conoce la VRAM disponible; usa perfil conservador hasta confirmar hardware.");
        }
        return new ProductionHardwareProfileAdvice("Perfil balanceado", detectedVramMb,
                resolution.width() > SimpleVideoResolutionPreset.FULL_HD_1080.width()
                        ? SimpleVideoResolutionPreset.FULL_HD_1080
                        : resolution,
                Math.min(batch, 2),
                detectedVramMb == 0,
                false,
                List.copyOf(warnings));
    }
}
