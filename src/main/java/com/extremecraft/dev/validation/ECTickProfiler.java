package com.extremecraft.dev.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class ECTickProfiler {
    private static final Map<String, Sample> SAMPLES = new LinkedHashMap<>();

    private ECTickProfiler() {
    }

    public static void record(String section, long nanos) {
        if (section == null || section.isBlank() || nanos < 0L) {
            return;
        }

        synchronized (SAMPLES) {
            SAMPLES.computeIfAbsent(section, ignored -> new Sample()).record(nanos);
        }
    }

    public static String summary() {
        synchronized (SAMPLES) {
            if (SAMPLES.isEmpty()) {
                return "tick_profiler: idle";
            }

            StringBuilder builder = new StringBuilder("tick_profiler: ");
            boolean first = true;
            for (Map.Entry<String, Sample> entry : SAMPLES.entrySet()) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append(entry.getKey())
                        .append(" avg=")
                        .append(entry.getValue().averageMicros())
                        .append("us")
                        .append(" count=")
                        .append(entry.getValue().count());
            }
            return builder.toString();
        }
    }

    private static final class Sample {
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder count = new LongAdder();

        private void record(long nanos) {
            totalNanos.add(nanos);
            count.increment();
        }

        private long averageMicros() {
            long currentCount = count.longValue();
            if (currentCount <= 0L) {
                return 0L;
            }
            return (totalNanos.longValue() / currentCount) / 1_000L;
        }

        private long count() {
            return count.longValue();
        }
    }
}
