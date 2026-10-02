package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Invalidatable identity preventing callbacks from a closed PDF project session. */
public final class PdfProjectSessionToken {
    private final String id = UUID.randomUUID().toString();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public String id() {
        return id;
    }

    public boolean active() {
        return active.get();
    }

    public void invalidate() {
        active.set(false);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PdfProjectSessionToken token && id.equals(token.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
