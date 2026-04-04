package com.extremecraft.platform.service;

import com.extremecraft.ecosystem.core.service.CoreServices;
import com.extremecraft.modules.runtime.ModuleRuntimeService;

@Deprecated(forRemoval = false, since = "1.2.0")
public final class ExtremeCraftServices {
    static {
        // Preserve previous default behavior while core service registry is extracted to module ownership.
        CoreServices.setModuleService(player -> ModuleRuntimeService.refreshPassiveModifiers((net.minecraft.server.level.ServerPlayer) player));
    }

    private ExtremeCraftServices() {
    }

    public static SkillService skillService() {
        return player -> CoreServices.skillService().refresh(player);
    }

    public static MachineService machineService() {
        return player -> CoreServices.machineService().refresh(player);
    }

    public static ResearchService researchService() {
        return player -> CoreServices.researchService().refresh(player);
    }

    public static ModuleService moduleService() {
        return player -> CoreServices.moduleService().refresh(player);
    }

    public static EnergyNetworkService energyNetworkService() {
        return player -> CoreServices.energyNetworkService().refresh(player);
    }

    public static void setSkillService(SkillService service) {
        CoreServices.setSkillService(service == null ? null : player -> service.refresh((net.minecraft.server.level.ServerPlayer) player));
    }

    public static void setMachineService(MachineService service) {
        CoreServices.setMachineService(service == null ? null : player -> service.onMachineTick((net.minecraft.server.level.ServerPlayer) player));
    }

    public static void setResearchService(ResearchService service) {
        CoreServices.setResearchService(service == null ? null : player -> service.refresh((net.minecraft.server.level.ServerPlayer) player));
    }

    public static void setModuleService(ModuleService service) {
        CoreServices.setModuleService(service == null ? null : player -> service.refresh((net.minecraft.server.level.ServerPlayer) player));
    }

    public static void setEnergyNetworkService(EnergyNetworkService service) {
        CoreServices.setEnergyNetworkService(service == null ? null : player -> service.sync((net.minecraft.server.level.ServerPlayer) player));
    }
}
