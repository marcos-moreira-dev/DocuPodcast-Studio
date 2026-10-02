package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VoiceReferenceSamplePathResolverTest {
    @TempDir
    Path temp;

    @Test
    void resolvesOfficialSamplesFromInstallationEvenWhenRuntimeIsElsewhere() throws Exception {
        Path installation = temp.resolve("installation");
        Path runtime = temp.resolve("runtime");
        Path project = temp.resolve("project");
        Path official = installation.resolve("samples/voices/advanced-presets/actor/neutral.wav");
        Files.createDirectories(official.getParent());
        Files.write(official, new byte[]{1, 2, 3});

        VoiceReferenceSample sample = new VoiceReferenceSample(
                "OFFICIAL", "VOICE", VoiceReferenceTone.NEUTRAL,
                "samples/voices/advanced-presets/actor/neutral.wav",
                VoiceSampleOrigin.APP_DEFAULT, VoiceFileOwnership.APP_RESOURCE,
                3L, Instant.EPOCH, "official");

        Path resolved = new VoiceReferenceSamplePathResolver(installation, runtime)
                .resolve(project, sample, "muestra oficial");

        assertEquals(official.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void resolvesManagedLibraryFromRuntimeAndProjectAssetsFromProject() throws Exception {
        Path installation = temp.resolve("installation");
        Path runtime = temp.resolve("runtime");
        Path project = temp.resolve("project");
        Path managed = runtime.resolve("voice-library/samples/actor.wav");
        Path projectSample = project.resolve("voices/actor.wav");
        Files.createDirectories(managed.getParent());
        Files.createDirectories(projectSample.getParent());
        Files.write(managed, new byte[]{1});
        Files.write(projectSample, new byte[]{2});
        VoiceReferenceSamplePathResolver resolver =
                new VoiceReferenceSamplePathResolver(installation, runtime);

        VoiceReferenceSample managedSample = new VoiceReferenceSample(
                "MANAGED", "VOICE", VoiceReferenceTone.NEUTRAL,
                "voice-library/samples/actor.wav",
                VoiceSampleOrigin.IMPORTED_FILE, VoiceFileOwnership.USER_APPDATA,
                1L, Instant.EPOCH, "managed");
        VoiceReferenceSample projectAsset = new VoiceReferenceSample(
                "PROJECT", "VOICE", VoiceReferenceTone.NEUTRAL,
                "voices/actor.wav",
                VoiceSampleOrigin.IMPORTED_FILE, VoiceFileOwnership.PROJECT_ASSET,
                1L, Instant.EPOCH, "project");

        assertEquals(managed.toAbsolutePath().normalize(),
                resolver.resolve(project, managedSample, "muestra administrada"));
        assertEquals(projectSample.toAbsolutePath().normalize(),
                resolver.resolve(project, projectAsset, "muestra del proyecto"));
    }
}
