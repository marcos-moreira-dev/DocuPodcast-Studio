package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import java.util.List;
import java.util.Objects;

/**
 * Persisted, deterministic delta of the stage at one theatre intervention.
 * Blank scalar values mean "not overridden"; explicit reset is represented by
 * {@link InheritanceMode#RESET}. Assets remain optional and are referenced by id.
 */
public record TheatreInterventionState(
        String interventionId,
        InheritanceMode inheritanceMode,
        String inheritsFromInterventionId,
        List<CharacterState> characters,
        List<ObjectState> objects,
        List<StageEvent> events,
        String microexpression,
        String emoji,
        String tone
) {
    public TheatreInterventionState {
        interventionId = required(interventionId, "interventionId");
        inheritanceMode = inheritanceMode == null ? InheritanceMode.PREVIOUS : inheritanceMode;
        inheritsFromInterventionId = normalized(inheritsFromInterventionId);
        if (inheritanceMode != InheritanceMode.EXPLICIT && !inheritsFromInterventionId.isBlank()) {
            throw new IllegalArgumentException("inheritsFromInterventionId requires EXPLICIT inheritance");
        }
        if (inheritanceMode == InheritanceMode.EXPLICIT && inheritsFromInterventionId.isBlank()) {
            throw new IllegalArgumentException("EXPLICIT inheritance requires inheritsFromInterventionId");
        }
        characters = copy(characters);
        objects = copy(objects);
        events = copy(events);
        microexpression = normalized(microexpression);
        emoji = normalized(emoji);
        tone = normalized(tone);
    }

    public TheatreInterventionState(String interventionId, InheritanceMode inheritanceMode,
                                    String inheritsFromInterventionId, List<CharacterState> characters,
                                    List<ObjectState> objects, List<StageEvent> events,
                                    String microexpression, String emoji) {
        this(interventionId, inheritanceMode, inheritsFromInterventionId, characters, objects, events,
                microexpression, emoji, "");
    }

    public enum InheritanceMode { PREVIOUS, EXPLICIT, RESET }
    public enum Presence { INHERIT, PRESENT, ABSENT }
    public enum EventType { ENTER, EXIT, MOVE, TAKE, CARRY, DROP, GIVE, INTERACT }

    public record CharacterState(
            String characterId,
            Presence presence,
            String position,
            String orientation,
            String gazeTarget,
            String visualVariantId,
            String costume
    ) {
        public CharacterState {
            characterId = required(characterId, "characterId");
            presence = presence == null ? Presence.INHERIT : presence;
            position = normalized(position);
            orientation = normalized(orientation);
            gazeTarget = normalized(gazeTarget);
            visualVariantId = normalized(visualVariantId);
            costume = normalized(costume);
            if (presence == Presence.ABSENT && !position.isBlank()) {
                throw new IllegalArgumentException("An absent character cannot have a stage position");
            }
        }
    }

    public record ObjectState(
            String objectId,
            Presence presence,
            String position,
            String holderCharacterId,
            String manipulation
    ) {
        public ObjectState {
            objectId = required(objectId, "objectId");
            presence = presence == null ? Presence.INHERIT : presence;
            position = normalized(position);
            holderCharacterId = normalized(holderCharacterId);
            manipulation = normalized(manipulation);
            if (presence == Presence.ABSENT && (!position.isBlank() || !holderCharacterId.isBlank())) {
                throw new IllegalArgumentException("An absent object cannot have a position or holder");
            }
        }
    }

    public record StageEvent(
            EventType type,
            String characterId,
            String objectId,
            String targetCharacterId,
            String fromPosition,
            String toPosition
    ) {
        public StageEvent {
            type = Objects.requireNonNull(type, "type");
            characterId = normalized(characterId);
            objectId = normalized(objectId);
            targetCharacterId = normalized(targetCharacterId);
            fromPosition = normalized(fromPosition);
            toPosition = normalized(toPosition);
        }
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String required(String value, String field) {
        String normalized = normalized(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip();
    }
}
