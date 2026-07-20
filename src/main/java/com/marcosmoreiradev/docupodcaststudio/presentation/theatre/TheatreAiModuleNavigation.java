package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.admin.AdminModuleNavigation;
import javafx.beans.property.ObjectProperty;

import java.util.List;

final class TheatreAiModuleNavigation extends AdminModuleNavigation<TheatreAiModuleId> {
    TheatreAiModuleNavigation(List<TheatreAiModuleDescriptor> descriptors,
                              ObjectProperty<TheatreAiModuleId> selectedModule) {
        super(descriptors, selectedModule);
    }
}
