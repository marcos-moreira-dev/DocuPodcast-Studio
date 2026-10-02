package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

final class OllamaRuntimeOwnershipTest {
    @TempDir Path root;

    @Test void managedRuntimeIsKilledButExternalRuntimeIsNeverKilled() throws Exception {
        assertRecoveryPolicy(RuntimeOwnership.MANAGED_PRIVATE_RUNTIME, true);
        assertRecoveryPolicy(RuntimeOwnership.EXTERNAL_RUNTIME, false);
    }

    private void assertRecoveryPolicy(RuntimeOwnership ownership,
                                      boolean expectedKilled) throws Exception {
        Path executable = root.resolve(ownership.name() + ".exe");
        Files.writeString(executable, "fixture");
        List<Process> children = new ArrayList<>();
        ManagedOllamaProcess runtime = new ManagedOllamaProcess(
                executable, root.resolve("models-" + ownership),
                root.resolve("logs-" + ownership), () -> 19555,
                ignored -> {
                    Process value = new ProcessBuilder("powershell", "-NoProfile",
                            "-Command", "Start-Sleep -Seconds 300").start();
                    children.add(value); return value;
                }, new ReadyEndpoint(), System::nanoTime, ownership);
        try {
            runtime.ensureReady(ExecutionContext.defaults("ownership"));
            long generationA = runtime.lifecycleSnapshot().runtimeGeneration();
            Process generationAProcess = children.getFirst();
            runtime.recoverUnresponsive(ExecutionContext.defaults("recover"), "test");
            if (expectedKilled) {
                generationAProcess.waitFor(2, TimeUnit.SECONDS);
                assertFalse(generationAProcess.isAlive());
                runtime.ensureReady(ExecutionContext.defaults("generation-b"));
                assertEquals(2, children.size());
                assertNotEquals(generationAProcess.pid(), children.getLast().pid());
                assertTrue(children.getLast().isAlive());
            } else {
                assertTrue(generationAProcess.isAlive());
                runtime.close();
                assertTrue(generationAProcess.isAlive(),
                        "closing the adapter must not terminate an external runtime");
                generationAProcess.destroyForcibly();
                generationAProcess.waitFor(2, TimeUnit.SECONDS);
            }
            assertTrue(runtime.lifecycleSnapshot().runtimeGeneration() > generationA);
        } finally {
            for (Process child : children) {
                if (child.isAlive()) {
                    child.destroyForcibly();
                    child.waitFor(2, TimeUnit.SECONDS);
                }
            }
            runtime.close();
        }
    }

    private static final class ReadyEndpoint implements ManagedOllamaProcess.EndpointClient {
        @Override public boolean ready(java.net.URI endpoint) { return true; }
        @Override public ManagedOllamaProcess.EndpointResponse post(java.net.URI endpoint,
                String body, Duration timeout) { return new ManagedOllamaProcess.EndpointResponse(200, "{}"); }
        @Override public ManagedOllamaProcess.EndpointResponse get(java.net.URI endpoint,
                Duration timeout) { return new ManagedOllamaProcess.EndpointResponse(200, "{}"); }
    }
}
