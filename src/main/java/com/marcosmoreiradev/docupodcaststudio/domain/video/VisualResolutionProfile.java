package com.marcosmoreiradev.docupodcaststudio.domain.video;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shared visual resolution catalog for image generation, AI super-resolution and video delivery.
 *
 * <p>The public dimensions are exact delivery dimensions. Generative runtimes may use the
 * corresponding multiple-of-eight working dimensions and normalize the final artifact afterwards.</p>
 */
public enum VisualResolutionProfile {
    P540("540p", 960, 540),
    P720("720p", 1280, 720),
    P1080("1080p", 1920, 1080),
    QHD_2K("2K/QHD", 2560, 1440),
    UHD_4K("4K/UHD", 3840, 2160);

    private final String displayName;
    private final int landscapeWidth;
    private final int landscapeHeight;

    VisualResolutionProfile(String displayName, int landscapeWidth, int landscapeHeight) {
        this.displayName = displayName;
        this.landscapeWidth = landscapeWidth;
        this.landscapeHeight = landscapeHeight;
    }

    public String displayName() {
        return displayName;
    }

    public int landscapeWidth() {
        return landscapeWidth;
    }

    public int landscapeHeight() {
        return landscapeHeight;
    }

    public Dimensions deliveryDimensions(int widthRatio, int heightRatio) {
        int safeWidthRatio = Math.max(1, widthRatio);
        int safeHeightRatio = Math.max(1, heightRatio);
        if (safeWidthRatio >= safeHeightRatio) {
            return new Dimensions(
                    Math.max(1, (int) Math.round(landscapeHeight * (safeWidthRatio / (double) safeHeightRatio))),
                    landscapeHeight);
        }
        return new Dimensions(
                landscapeHeight,
                Math.max(1, (int) Math.round(landscapeHeight * (safeHeightRatio / (double) safeWidthRatio))));
    }

    public Dimensions workingDimensions(int widthRatio, int heightRatio) {
        Dimensions delivery = deliveryDimensions(widthRatio, heightRatio);
        return new Dimensions(multipleOfEight(delivery.width()), multipleOfEight(delivery.height()));
    }

    public static Dimensions workingDimensionsForExact(int width, int height) {
        return new Dimensions(multipleOfEight(Math.max(1, width)), multipleOfEight(Math.max(1, height)));
    }

    public boolean higherThan(VisualResolutionProfile source) {
        return source != null && landscapeHeight > source.landscapeHeight;
    }

    public static List<VisualResolutionProfile> higherThanProfile(VisualResolutionProfile source) {
        VisualResolutionProfile current = source == null ? P1080 : source;
        return Arrays.stream(values()).filter(profile -> profile.higherThan(current)).toList();
    }

    public static VisualResolutionProfile from(String value, VisualResolutionProfile fallback) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace('/', '_');
        for (VisualResolutionProfile profile : values()) {
            if (profile.name().equals(normalized)
                    || profile.displayName.toUpperCase(Locale.ROOT).replace('/', '_').equals(normalized)) {
                return profile;
            }
        }
        return switch (normalized) {
            case "540P", "LOW_540", "SD_540" -> P540;
            case "720P", "HD", "HD_720" -> P720;
            case "1080P", "FHD", "FHD_1080", "FULL_HD_1080" -> P1080;
            case "2K", "QHD", "QHD2K" -> QHD_2K;
            case "4K", "UHD", "UHD4K" -> UHD_4K;
            default -> fallback == null ? P1080 : fallback;
        };
    }

    private static int multipleOfEight(int value) {
        return Math.max(8, ((value + 7) / 8) * 8);
    }

    public record Dimensions(int width, int height) {
        public Dimensions {
            if (width < 1 || height < 1) {
                throw new IllegalArgumentException("visual dimensions must be positive");
            }
        }

        public boolean isStrictlyLargerThan(int sourceWidth, int sourceHeight) {
            return width > sourceWidth && height > sourceHeight;
        }
    }
}
