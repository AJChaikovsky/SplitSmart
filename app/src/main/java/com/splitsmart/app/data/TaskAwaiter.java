package com.splitsmart.app.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.ExecutionException;

/**
 * Blocks on a Firebase Task and rethrows its failure with the original message.
 * Must be called off the main thread (UI code goes through UiUtils.launchIo).
 */
final class TaskAwaiter {
    private TaskAwaiter() {
    }

    static <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Firebase", e);
        }
    }
}
