package com.extremecraft.platform.service;

import com.extremecraft.modules.runtime.ModuleRuntimeService;

public final class ExtremeCraftServices {
    private static SkillService skillService = player -> { };
    private static MachineService machineService = player -> { };
    private static ResearchService researchService = player -> { };
    private static ModuleService moduleService = ModuleRuntimeService::refreshPassiveModifiers;
    private static EnergyNetworkService energyNetworkService = player -> { };

    private ExtremeCraftServices() {
    }

    public static SkillService skillService() {
        return skillService;
    }

    public static MachineService machineService() {
        return machineService;
    }

    public static ResearchService researchService() {
        return researchService;
    }

    public static ModuleService moduleService() {
        return moduleService;
    }

    public static EnergyNetworkService energyNetworkService() {
        return energyNetworkService;
    }

    public static void setSkillService(SkillService service) {
        skillService = service;
    }

    public static void setMachineService(MachineService service) {
        machineService = service;
    }

    public static void setResearchService(ResearchService service) {
        researchService = service;
    }

    public static void setModuleService(ModuleService service) {
        moduleService = service;
    }

    public static void setEnergyNetworkService(EnergyNetworkService service) {
        energyNetworkService = service;
    }
}
