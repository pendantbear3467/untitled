package com.extremecraft.dev.validation;

import com.extremecraft.ability.AbilityRegistry;
import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.magic.SpellRegistry;
import com.extremecraft.machine.MachineRegistry;
import com.extremecraft.machine.core.MachineCatalog;
import com.extremecraft.modules.registry.ModuleAbilityRegistry;
import com.extremecraft.platform.data.registry.MachineDataRegistry;
import com.extremecraft.platform.data.registry.EndgameCoreDataRegistry;
import com.extremecraft.platform.data.registry.ReactorPartDataRegistry;
import com.extremecraft.platform.data.registry.RadiationSourceDataRegistry;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;
import com.extremecraft.quest.QuestManager;

public final class ECRegistryAuditService {
    private ECRegistryAuditService() {
    }

    public static String summary() {
        return "registry_audit: tech_machines=" + MachineCatalog.DEFINITIONS.size()
                + ", machine_processing_metadata=" + MachineDataRegistry.registry().size()
                + ", legacy_machine_defs=" + MachineRegistry.machines().size()
                + ", abilities=" + AbilityRegistry.size()
                + ", module_trigger_defs=" + ModuleAbilityRegistry.size()
                + ", spells=" + SpellRegistry.size()
                + ", canonical_classes=" + ClassDefinitions.all().size()
                + ", class_adapter=" + ClassRegistry.size()
                + ", quests=" + QuestManager.all().size()
                + ", radiation_sources=" + RadiationSourceDataRegistry.registry().size()
                + ", reactor_parts=" + ReactorPartDataRegistry.registry().size()
                + ", endgame_cores=" + EndgameCoreDataRegistry.registry().size()
                + " | " + ECRuntimeOwnershipAudit.summary();
    }
}
