package com.marcosmoreiradev.docupodcaststudio.application.process;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Single local AI-model admission guard for audio and image generation jobs. */
public final class AiModelResourceGuard {
    private static final AiModelResourceGuard GLOBAL = new AiModelResourceGuard();
    private final AtomicReference<Lease> active = new AtomicReference<>();

    public static AiModelResourceGuard global() {
        return GLOBAL;
    }

    public Lease acquire(AiModelResourceKind kind, String label) throws AiModelResourceBusyException {
        Lease lease = new Lease(this, kind == null ? AiModelResourceKind.UNKNOWN : kind, normalize(label));
        if (active.compareAndSet(null, lease)) {
            return lease;
        }
        Lease current = active.get();
        throw new AiModelResourceBusyException(busyMessage(lease, current));
    }

    public Optional<Lease> activeLease() {
        return Optional.ofNullable(active.get());
    }

    void release(Lease lease) {
        active.compareAndSet(lease, null);
    }

    private static String busyMessage(Lease requested, Lease current) {
        String currentLabel = current == null ? "otro proceso de IA" : current.displayLabel();
        String requestedLabel = requested == null ? "esta generacion" : requested.displayLabel();
        return "No se puede cargar mas de un modelo de IA a la vez. "
                + currentLabel + " ya esta usando el recurso local; espera a que termine o cancelalo antes de iniciar "
                + requestedLabel + ". La verificacion fina de VRAM libre aun no esta confirmada para permitir concurrencia segura.";
    }

    private static String normalize(String value) {
        String text = Objects.toString(value, "").strip();
        return text.isBlank() ? "generacion IA" : text;
    }

    public enum AiModelResourceKind {
        AUDIO,
        IMAGE,
        UNKNOWN
    }

    public static final class Lease implements AutoCloseable {
        private final AiModelResourceGuard owner;
        private final AiModelResourceKind kind;
        private final String label;

        private Lease(AiModelResourceGuard owner, AiModelResourceKind kind, String label) {
            this.owner = owner;
            this.kind = kind;
            this.label = label;
        }

        public AiModelResourceKind kind() {
            return kind;
        }

        public String label() {
            return label;
        }

        public String displayLabel() {
            return (kind == AiModelResourceKind.AUDIO ? "Generacion de audio"
                    : kind == AiModelResourceKind.IMAGE ? "Generacion de imagenes"
                    : "Generacion IA") + " (" + label + ")";
        }

        @Override
        public void close() {
            owner.release(this);
        }
    }
}
