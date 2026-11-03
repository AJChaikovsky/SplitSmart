package com.splitsmart.app.ui;

/** Handle for background work started by UiUtils; cancelling drops its result. */
public final class UiTask {
    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }
}
