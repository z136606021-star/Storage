package com.storage.common.web;

public final class RequestContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(State state) {
        HOLDER.set(state);
    }

    public static State get() {
        return HOLDER.get();
    }

    public static String getRequestId() {
        State state = HOLDER.get();
        return state == null ? null : state.requestId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record State(
            String requestId,
            String method,
            String path,
            long startedAtMillis
    ) {
    }
}
