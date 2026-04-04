package com.extremecraft.platform.service;

import com.extremecraft.ecosystem.core.service.CoreServices;

@Deprecated(forRemoval = false, since = "1.2.0")
public final class ExtremeCraftServices {
    private ExtremeCraftServices() {
    }

    public static SkillService skillService() {
        return CoreServices.skillService()::refresh;
    }

    public static MachineService machineService() {
        return CoreServices.machineService()::refresh;
    }

    public static ResearchService researchService() {
        return CoreServices.researchService()::refresh;
    }

    public static ModuleService moduleService() {
        return CoreServices.moduleService()::refresh;
    }

    public static EnergyNetworkService energyNetworkService() {
        return CoreServices.energyNetworkService()::refresh;
    }

    public static void setSkillService(SkillService service) {
        CoreServices.setSkillService(service == null ? null : service::refresh);
    }

    public static void setMachineService(MachineService service) {
        CoreServices.setMachineService(service == null ? null : service::onMachineTick);
    }

    public static void setResearchService(ResearchService service) {
        CoreServices.setResearchService(service == null ? null : service::refresh);
    }

    public static void setModuleService(ModuleService service) {
        CoreServices.setModuleService(service == null ? null : service::refresh);
    }

    public static void setEnergyNetworkService(EnergyNetworkService service) {
        CoreServices.setEnergyNetworkService(service == null ? null : service::sync);
    }
}
