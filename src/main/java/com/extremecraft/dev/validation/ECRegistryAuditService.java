package com.extremecraft.dev.validation;

import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.magic.SpellRegistry;
import com.extremecraft.machine.MachineRegistry;
import com.extremecraft.platform.data.registry.EndgameCoreDataRegistry;
import com.extremecraft.platform.data.registry.ReactorPartDataRegistry;
import com.extremecraft.platform.data.registry.RadiationSourceDataRegistry;
import com.extremecraft.quest.QuestManager;

public final class ECRegistryAuditService {
    private ECRegistryAuditService() {
    }

    public static String summary() {
        return "registry_audit: machines=" + MachineRegistry.machines().size()
                + ", spells=" + SpellRegistry.size()
                + ", classes=" + ClassRegistry.size()
                + ", quests=" + QuestManager.all().size()
                + ", radiation_sources=" + RadiationSourceDataRegistry.registry().size()
                + ", reactor_parts=" + ReactorPartDataRegistry.registry().size()
                + ", endgame_cores=" + EndgameCoreDataRegistry.registry().size();
    }
}
