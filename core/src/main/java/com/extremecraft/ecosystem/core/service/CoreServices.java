package com.extremecraft.ecosystem.core.service;

/**
 * Shared cross-module service registry owned by ExtremeCraft Core.
 */
public final class CoreServices {
    private static CoreSkillService skillService = player -> { };
    private static CoreMachineService machineService = player -> { };
    private static CoreResearchService researchService = player -> { };
    private static CoreModuleService moduleService = player -> { };
    private static CoreEnergyNetworkService energyNetworkService = player -> { };

    private CoreServices() {
    }

    public static CoreSkillService skillService() {
        return skillService;
    }

    public static CoreMachineService machineService() {
        return machineService;
    }

    public static CoreResearchService researchService() {
        return researchService;
    }

    public static CoreModuleService moduleService() {
        return moduleService;
    }

    public static CoreEnergyNetworkService energyNetworkService() {
        return energyNetworkService;
    }

    public static void setSkillService(CoreSkillService service) {
        skillService = service == null ? player -> { } : service;
    }

    public static void setMachineService(CoreMachineService service) {
        machineService = service == null ? player -> { } : service;
    }

    public static void setResearchService(CoreResearchService service) {
        researchService = service == null ? player -> { } : service;
    }

    public static void setModuleService(CoreModuleService service) {
        moduleService = service == null ? player -> { } : service;
    }

    public static void setEnergyNetworkService(CoreEnergyNetworkService service) {
        energyNetworkService = service == null ? player -> { } : service;
    }
}
