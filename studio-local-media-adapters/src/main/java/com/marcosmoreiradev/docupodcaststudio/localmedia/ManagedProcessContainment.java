package com.marcosmoreiradev.docupodcaststudio.localmedia;

/** Owns an operating-system process container whose close terminates its members. */
interface ManagedProcessContainment extends AutoCloseable {
    boolean attach(Process process);

    String diagnostics();

    @Override
    void close();

    static ManagedProcessContainment create() {
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            return WindowsJobObjectContainment.open();
        }
        return NoopProcessContainment.INSTANCE;
    }

    enum NoopProcessContainment implements ManagedProcessContainment {
        INSTANCE;

        @Override public boolean attach(Process process) { return false; }
        @Override public String diagnostics() { return "process-handle-fallback"; }
        @Override public void close() { }
    }
}
