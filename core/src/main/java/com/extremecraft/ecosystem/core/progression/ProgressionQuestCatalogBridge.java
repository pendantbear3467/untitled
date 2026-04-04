package com.extremecraft.ecosystem.core.progression;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public final class ProgressionQuestCatalogBridge {
    private static volatile Supplier<Collection<ProgressionQuestDescriptor>> provider = List::of;

    private ProgressionQuestCatalogBridge() {
    }

    public static void setProvider(Supplier<Collection<ProgressionQuestDescriptor>> questProvider) {
        provider = questProvider == null ? List::of : questProvider;
    }

    public static Collection<ProgressionQuestDescriptor> allQuestDescriptors() {
        return provider.get();
    }

    public static Optional<ProgressionQuestDescriptor> questDescriptor(String questId) {
        if (questId == null || questId.isBlank()) {
            return Optional.empty();
        }

        String normalized = questId.trim().toLowerCase(Locale.ROOT);
        return provider.get().stream()
                .filter(descriptor -> normalized.equals(descriptor.id()))
                .findFirst();
    }
}
