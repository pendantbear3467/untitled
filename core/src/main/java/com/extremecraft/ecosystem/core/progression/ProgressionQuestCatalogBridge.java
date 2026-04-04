package com.extremecraft.ecosystem.core.progression;

import java.util.Collection;
import java.util.List;
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
}