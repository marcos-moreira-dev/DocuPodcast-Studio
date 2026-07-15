package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;

/** Result of importing a voice sample as a portable project asset. */
public record VoiceSampleImportResult(
        DocuPodcastProject project,
        VoiceLibrary voiceLibrary,
        VoiceProfile updatedVoiceProfile,
        ProjectAssetReference sampleAsset,
        VoiceReferenceSample referenceSample
) {
}
