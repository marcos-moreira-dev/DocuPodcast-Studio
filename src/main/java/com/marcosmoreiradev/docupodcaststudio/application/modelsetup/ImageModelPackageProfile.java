package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.util.Locale;

/** Catalog of explicit local image model packages. Heavy downloads never run implicitly. */
public enum ImageModelPackageProfile {
    TEST_4GB_SD15(
            "TEST_4GB_SD15",
            "Prueba 4GB",
            "SD 1.5 fp16/pruned, 512x512, bajo consumo. Sirve para validar flujo, no calidad final HD.",
            "Comfy-Org/stable-diffusion-v1-5-archive",
            "https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive",
            "v1-5-pruned-emaonly-fp16.safetensors",
            "https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive/resolve/main/v1-5-pruned-emaonly-fp16.safetensors",
            2_132_696_762L,
            ImageModelPackageAccessPolicy.PUBLIC,
            ImageModelPackageInstallType.AUTOMATIC_DOWNLOAD,
            false,
            false),
    SD15_DREAMSHAPER(
            "SD15_DREAMSHAPER",
            "Estetico SD 1.5",
            "Perfil SD 1.5 estetico compatible con equipos modestos. Requiere descarga o importacion explicita.",
            "Lykon/DreamShaper",
            "https://huggingface.co/Lykon/DreamShaper",
            "DreamShaper_8_pruned.safetensors",
            "https://huggingface.co/Lykon/DreamShaper/resolve/main/DreamShaper_8_pruned.safetensors",
            2_132_625_894L,
            ImageModelPackageAccessPolicy.PUBLIC,
            ImageModelPackageInstallType.AUTOMATIC_DOWNLOAD,
            false,
            false),
    HIGH_QUALITY_FLUX(
            "HIGH_QUALITY_FLUX",
            "Alta calidad / Flux",
            "Perfil para equipo potente. No es default y requiere confirmacion fuerte o importacion manual.",
            "black-forest-labs/FLUX.1-dev",
            "https://huggingface.co/black-forest-labs/FLUX.1-dev",
            "flux1-dev.safetensors",
            "https://huggingface.co/black-forest-labs/FLUX.1-dev/resolve/main/flux1-dev.safetensors",
            24_000_000_000L,
            ImageModelPackageAccessPolicy.GATED_OR_TOKEN,
            ImageModelPackageInstallType.GATED_DOWNLOAD,
            true,
            false),
    CUSTOM_COMFY_WORKFLOW(
            "CUSTOM_COMFY_WORKFLOW",
            "Workflow personalizado",
            "Usa un workflow local importado por el usuario. Puede soportar intermedios si el workflow los define.",
            "",
            "",
            "",
            "",
            0L,
            ImageModelPackageAccessPolicy.MANUAL_IMPORT,
            ImageModelPackageInstallType.CUSTOM_WORKFLOW,
            false,
            true);

    private final String presetId;
    private final String displayName;
    private final String description;
    private final String recommendedRepository;
    private final String providerUrl;
    private final String checkpointName;
    private final String downloadUrl;
    private final long approximateBytes;
    private final ImageModelPackageAccessPolicy accessPolicy;
    private final ImageModelPackageInstallType installType;
    private final boolean highEnd;
    private final boolean supportsInterpolation;

    ImageModelPackageProfile(String presetId,
                             String displayName,
                             String description,
                             String recommendedRepository,
                             String providerUrl,
                             String checkpointName,
                             String downloadUrl,
                             long approximateBytes,
                             ImageModelPackageAccessPolicy accessPolicy,
                             ImageModelPackageInstallType installType,
                             boolean highEnd,
                             boolean supportsInterpolation) {
        this.presetId = presetId;
        this.displayName = displayName;
        this.description = description;
        this.recommendedRepository = recommendedRepository;
        this.providerUrl = providerUrl;
        this.checkpointName = checkpointName;
        this.downloadUrl = downloadUrl;
        this.approximateBytes = approximateBytes;
        this.accessPolicy = accessPolicy == null ? ImageModelPackageAccessPolicy.MANUAL_IMPORT : accessPolicy;
        this.installType = installType == null ? ImageModelPackageInstallType.MANUAL_IMPORT : installType;
        this.highEnd = highEnd;
        this.supportsInterpolation = supportsInterpolation;
    }

    public String presetId() {
        return presetId;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String recommendedRepository() {
        return recommendedRepository;
    }

    public String providerUrl() {
        return providerUrl;
    }

    public String checkpointName() {
        return checkpointName;
    }

    public String downloadUrl() {
        return downloadUrl;
    }

    public long approximateBytes() {
        return approximateBytes;
    }

    public ImageModelPackageAccessPolicy accessPolicy() {
        return accessPolicy;
    }

    public ImageModelPackageInstallType installType() {
        return installType;
    }

    public boolean downloadable() {
        return !downloadUrl.isBlank() && accessPolicy != ImageModelPackageAccessPolicy.MANUAL_IMPORT;
    }

    public boolean requiresAuthentication() {
        return accessPolicy == ImageModelPackageAccessPolicy.GATED_OR_TOKEN;
    }

    public boolean highEnd() {
        return highEnd;
    }

    public boolean supportsInterpolation() {
        return supportsInterpolation;
    }

    public String manualSetupInstructions() {
        if (this != HIGH_QUALITY_FLUX) {
            return "";
        }
        return "Antes de descargar FLUX.1-dev debes abrir el repositorio del proveedor y aceptar sus terminos. "
                + "Luego descarga manualmente: flux1-dev.safetensors, ae.safetensors, clip_l.safetensors "
                + "y un encoder T5XXL. DocuPodcast acepta T5 FP8/FP16/BF16 o la carpeta oficial sharded text_encoder_2, "
                + "que consolida por streaming sin alterar el original. Ubicaciones esperadas: models/image/flux1-dev.safetensors, "
                + "models/image/vae/ae.safetensors y models/image/text_encoders/. El workflow FLUX viene integrado.";
    }

    public static ImageModelPackageProfile fromPreset(String preset) {
        String normalized = preset == null ? "" : preset.strip().toUpperCase(Locale.ROOT);
        for (ImageModelPackageProfile profile : values()) {
            if (profile.presetId.equalsIgnoreCase(normalized) || profile.name().equalsIgnoreCase(normalized)) {
                return profile;
            }
        }
        return TEST_4GB_SD15;
    }
}
