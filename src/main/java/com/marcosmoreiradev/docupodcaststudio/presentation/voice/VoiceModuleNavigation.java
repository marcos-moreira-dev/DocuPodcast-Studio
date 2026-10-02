package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.admin.AdminModuleNavigation;
import javafx.beans.property.ObjectProperty;

import java.util.List;

/** Voices contribution to the transversal administrative navigation. */
public final class VoiceModuleNavigation extends AdminModuleNavigation<VoiceModuleId> {
    public VoiceModuleNavigation(List<VoiceModuleDescriptor> descriptors,
                                 ObjectProperty<VoiceModuleId> selectedModule) {
        super(descriptors, selectedModule);
    }
}
