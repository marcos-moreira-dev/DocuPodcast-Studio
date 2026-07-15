package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSpatialRoleIcon;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

final class TheatreSpatialIconSet {
    private static final Map<TheatreSpatialRoleIcon, Image> CACHE = new EnumMap<>(TheatreSpatialRoleIcon.class);

    private TheatreSpatialIconSet() {
    }

    static Image image(TheatreSpatialRoleIcon role) {
        TheatreSpatialRoleIcon resolved = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(resolved, TheatreSpatialIconSet::load);
        }
    }

    private static Image load(TheatreSpatialRoleIcon role) {
        var stream = TheatreSpatialIconSet.class.getResourceAsStream(role.resourcePath());
        if (stream == null) {
            return null;
        }
        Image image = new Image(stream);
        return image.isError() ? null : image;
    }
}
