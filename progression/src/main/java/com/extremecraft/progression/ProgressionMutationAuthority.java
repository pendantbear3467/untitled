package com.extremecraft.progression;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Tracks whether the current thread entered progression mutation through {@link ProgressionFacade}.
 *
 * <p>This is a lightweight runtime guard for the migration period: lower-level services still
 * execute the real writes, but they warn when callers bypass the facade-owned mutation boundary.</p>
 */
public final class ProgressionMutationAuthority {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadLocal<Integer> FACADE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private ProgressionMutationAuthority() {
    }

    static int runInt(String operation, IntSupplier action) {
        enter();
        try {
            return action.getAsInt();
        } finally {
            exit();
        }
    }

    static boolean runBoolean(String operation, BooleanSupplier action) {
        enter();
        try {
            return action.getAsBoolean();
        } finally {
            exit();
        }
    }

    static <T> T run(String operation, Supplier<T> action) {
        enter();
        try {
            return action.get();
        } finally {
            exit();
        }
    }

    static void runVoid(String operation, Runnable action) {
        enter();
        try {
            action.run();
        } finally {
            exit();
        }
    }

    public static void warnIfBypassed(String operation) {
        if (FACADE_DEPTH.get() > 0) {
            return;
        }

        LOGGER.warn("[ProgressionGuard] Mutation '{}' bypassed ProgressionFacade. Caller={}", operation, caller());
    }

    private static void enter() {
        FACADE_DEPTH.set(FACADE_DEPTH.get() + 1);
    }

    private static void exit() {
        int depth = Math.max(0, FACADE_DEPTH.get() - 1);
        if (depth == 0) {
            FACADE_DEPTH.remove();
            return;
        }
        FACADE_DEPTH.set(depth);
    }

    private static String caller() {
        String self = ProgressionMutationAuthority.class.getName();
        String facade = ProgressionFacade.class.getName();

        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String className = frame.getClassName();
            if (className.equals(self) || className.equals(facade) || className.startsWith("java.lang.Thread")) {
                continue;
            }
            return className + "#" + frame.getMethodName();
        }

        return "<unknown>";
    }
}
