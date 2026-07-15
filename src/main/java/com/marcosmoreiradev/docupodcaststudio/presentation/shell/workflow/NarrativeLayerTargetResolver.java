package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves concrete project targets for narrative layers.
 *
 * <p>T74 removes the old placeholder targets from productive layer assignment.
 * A visible action may only create a layer when it can point to a real voice, style,
 * image or audio asset already known by the project. If the required target is not
 * available, the coordinator returns an actionable message and leaves the project
 * unchanged.</p>
 */
public final class NarrativeLayerTargetResolver {
    public TargetResolution resolve(DocuPodcastProject project, NarrativeLayerKind kind, String preferredTargetId) {
        Objects.requireNonNull(project, "project");
        NarrativeLayerKind normalizedKind = kind == null ? NarrativeLayerKind.NOTE : kind;
        String preferred = preferredTargetId == null ? "" : preferredTargetId.strip();
        return switch (normalizedKind) {
            case VOICE -> resolveVoice(project, preferred);
            case HUMAN_AUDIO -> resolveAudio(project, preferred, "audio principal");
            case EMOTION -> resolveStyle(project, preferred);
            case IMAGE, BRIDGE_IMAGE -> resolveImage(project, preferred);
            case AMBIENT_AUDIO -> resolveAudio(project, preferred, "audio ambiente");
            case NOTE -> TargetResolution.resolved("", "Nota de producción", "Nota interna del proyecto DocuPodcast.");
        };
    }

    private static TargetResolution resolveVoice(DocuPodcastProject project, String preferred) {
        Optional<VoiceProfile> preferredVoice = preferred.isBlank()
                ? Optional.empty()
                : project.voiceLibrary().voiceById(preferred);
        Optional<VoiceProfile> voice = preferredVoice.or(() -> project.voiceLibrary().voiceById("VOC-NARRATOR"))
                .or(() -> project.voiceLibrary().voices().stream().min(Comparator.comparing(VoiceProfile::id)));
        return voice.map(profile -> TargetResolution.resolved(
                        profile.id(),
                        profile.displayName(),
                        "Voz real de la biblioteca del proyecto. Configura el motor en Configuración."))
                .orElseGet(() -> TargetResolution.missing(
                        "No hay una voz disponible en la biblioteca. Crea o importa una voz antes de asignar voz principal."));
    }

    private static TargetResolution resolveStyle(DocuPodcastProject project, String preferred) {
        Optional<VoiceReferenceTone> requestedTone = VoiceReferenceTone.fromLayerTargetId(preferred);
        if (requestedTone.isPresent()) {
            VoiceReferenceTone tone = requestedTone.get();
            return TargetResolution.resolved(
                    tone.layerTargetId(),
                    "Tono " + tone.displayName(),
                    "Tono de referencia elegido en Documento. Si falta la muestra exacta, la generación puede usar neutral como fallback.");
        }
        Optional<PerformanceStyle> preferredStyle = preferred.isBlank()
                ? Optional.empty()
                : project.voiceLibrary().styleById(preferred);
        Optional<PerformanceStyle> style = preferredStyle.or(() -> project.voiceLibrary().styleById("STY-NEUTRAL"))
                .or(() -> project.voiceLibrary().styles().stream().min(Comparator.comparing(PerformanceStyle::id)));
        return style.map(profile -> TargetResolution.resolved(
                        profile.id(),
                        profile.displayName(),
                        "Intención narrativa; el motor de voz puede ignorarla si no soporta estilos."))
                .orElseGet(() -> TargetResolution.missing(
                        "No hay estilos narrativos disponibles. Restaura la biblioteca de voces antes de asignar emoción/intención."));
    }

    private static TargetResolution resolveImage(DocuPodcastProject project, String preferred) {
        Optional<ProjectAssetReference> preferredImage = preferred.isBlank()
                ? Optional.empty()
                : project.assets().byId(preferred).filter(ProjectAssetReference::isImage);
        Optional<ProjectAssetReference> image = preferredImage.or(() -> firstAsset(project, ProjectAssetKind.IMAGE, ProjectAssetKind.THUMBNAIL));
        return image.map(asset -> TargetResolution.resolved(
                        asset.id(),
                        asset.displayName(),
                        "Imagen real del proyecto asociada al texto. No modifica el documento fuente."))
                .orElseGet(() -> TargetResolution.missing(
                        "Importa o selecciona una imagen real antes de asociarla a este texto."));
    }

    private static TargetResolution resolveAudio(DocuPodcastProject project, String preferred, String label) {
        Optional<ProjectAssetReference> preferredAudio = preferred.isBlank()
                ? Optional.empty()
                : project.assets().byId(preferred).filter(ProjectAssetReference::isAudio);
        Optional<ProjectAssetReference> audio = preferredAudio.or(() -> firstAsset(project, ProjectAssetKind.AUDIO_CLIP, ProjectAssetKind.AUDIO_FINAL));
        return audio.map(asset -> TargetResolution.resolved(
                        asset.id(),
                        asset.displayName(),
                        "Asset real de " + label + " asociado al texto. No modifica el documento fuente."))
                .orElseGet(() -> TargetResolution.missing(
                        "Importa o graba un asset de " + label + " antes de asignarlo a este texto."));
    }

    private static Optional<ProjectAssetReference> firstAsset(DocuPodcastProject project, ProjectAssetKind primary, ProjectAssetKind secondary) {
        List<ProjectAssetReference> candidates = project.assets().references().stream()
                .filter(asset -> asset.kind() == primary || asset.kind() == secondary)
                .toList();
        return candidates.stream().findFirst();
    }

    public record TargetResolution(boolean resolved, String targetId, String displayName, String notes, String message) {
        public static TargetResolution resolved(String targetId, String displayName, String notes) {
            return new TargetResolution(true,
                    targetId == null ? "" : targetId.strip(),
                    displayName == null ? "" : displayName.strip(),
                    notes == null ? "" : notes.strip(),
                    "");
        }

        public static TargetResolution missing(String message) {
            return new TargetResolution(false, "", "", "", message == null ? "" : message.strip());
        }
    }
}
