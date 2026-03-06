package com.extremecraft.api.module;

import com.extremecraft.api.registration.ExtremeCraftApiProvider;

public interface ExtremeCraftModule {
    String moduleId();

    String moduleName();

    int apiVersion();

    int protocolVersion();

    default boolean requiredOnClient() {
        return false;
    }

    default boolean requiredOnServer() {
        return true;
    }

    void register(ExtremeCraftApiProvider api);
}
