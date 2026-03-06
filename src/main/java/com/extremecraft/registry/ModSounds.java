package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ECConstants.MODID);

    public static final RegistryObject<SoundEvent> TECH_CONSTRUCT_AMBIENT = register("entity.tech_construct.ambient");
    public static final RegistryObject<SoundEvent> TECH_CONSTRUCT_HURT = register("entity.tech_construct.hurt");
    public static final RegistryObject<SoundEvent> TECH_CONSTRUCT_DEATH = register("entity.tech_construct.death");
    public static final RegistryObject<SoundEvent> TECH_CONSTRUCT_ATTACK = register("entity.tech_construct.attack");

    public static final RegistryObject<SoundEvent> ARCANE_WRAITH_AMBIENT = register("entity.arcane_wraith.ambient");
    public static final RegistryObject<SoundEvent> ARCANE_WRAITH_HURT = register("entity.arcane_wraith.hurt");
    public static final RegistryObject<SoundEvent> ARCANE_WRAITH_DEATH = register("entity.arcane_wraith.death");
    public static final RegistryObject<SoundEvent> ARCANE_WRAITH_ATTACK = register("entity.arcane_wraith.attack");

    public static final RegistryObject<SoundEvent> VOID_STALKER_AMBIENT = register("entity.void_stalker.ambient");
    public static final RegistryObject<SoundEvent> VOID_STALKER_HURT = register("entity.void_stalker.hurt");
    public static final RegistryObject<SoundEvent> VOID_STALKER_DEATH = register("entity.void_stalker.death");
    public static final RegistryObject<SoundEvent> VOID_STALKER_ATTACK = register("entity.void_stalker.attack");

    public static final RegistryObject<SoundEvent> ANCIENT_SENTINEL_AMBIENT = register("entity.ancient_sentinel.ambient");
    public static final RegistryObject<SoundEvent> ANCIENT_SENTINEL_HURT = register("entity.ancient_sentinel.hurt");
    public static final RegistryObject<SoundEvent> ANCIENT_SENTINEL_DEATH = register("entity.ancient_sentinel.death");
    public static final RegistryObject<SoundEvent> ANCIENT_SENTINEL_ATTACK = register("entity.ancient_sentinel.attack");

    public static final RegistryObject<SoundEvent> ENERGY_PARASITE_AMBIENT = register("entity.energy_parasite.ambient");
    public static final RegistryObject<SoundEvent> ENERGY_PARASITE_HURT = register("entity.energy_parasite.hurt");
    public static final RegistryObject<SoundEvent> ENERGY_PARASITE_DEATH = register("entity.energy_parasite.death");
    public static final RegistryObject<SoundEvent> ENERGY_PARASITE_ATTACK = register("entity.energy_parasite.attack");

    public static final RegistryObject<SoundEvent> RUNIC_GOLEM_AMBIENT = register("entity.runic_golem.ambient");
    public static final RegistryObject<SoundEvent> RUNIC_GOLEM_HURT = register("entity.runic_golem.hurt");
    public static final RegistryObject<SoundEvent> RUNIC_GOLEM_DEATH = register("entity.runic_golem.death");
    public static final RegistryObject<SoundEvent> RUNIC_GOLEM_ATTACK = register("entity.runic_golem.attack");

    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_AMBIENT = register("entity.ancient_core_guardian.ambient");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_HURT = register("entity.ancient_core_guardian.hurt");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_DEATH = register("entity.ancient_core_guardian.death");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_ATTACK = register("entity.ancient_core_guardian.attack");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_INTRO = register("entity.ancient_core_guardian.intro");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_PHASE = register("entity.ancient_core_guardian.phase_transition");
    public static final RegistryObject<SoundEvent> ANCIENT_CORE_GUARDIAN_DEATH_ROAR = register("entity.ancient_core_guardian.death_roar");

    public static final RegistryObject<SoundEvent> VOID_TITAN_AMBIENT = register("entity.void_titan.ambient");
    public static final RegistryObject<SoundEvent> VOID_TITAN_HURT = register("entity.void_titan.hurt");
    public static final RegistryObject<SoundEvent> VOID_TITAN_DEATH = register("entity.void_titan.death");
    public static final RegistryObject<SoundEvent> VOID_TITAN_ATTACK = register("entity.void_titan.attack");
    public static final RegistryObject<SoundEvent> VOID_TITAN_INTRO = register("entity.void_titan.intro");
    public static final RegistryObject<SoundEvent> VOID_TITAN_PHASE = register("entity.void_titan.phase_transition");
    public static final RegistryObject<SoundEvent> VOID_TITAN_DEATH_ROAR = register("entity.void_titan.death_roar");

    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_AMBIENT = register("entity.overcharged_machine_god.ambient");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_HURT = register("entity.overcharged_machine_god.hurt");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_DEATH = register("entity.overcharged_machine_god.death");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_ATTACK = register("entity.overcharged_machine_god.attack");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_INTRO = register("entity.overcharged_machine_god.intro");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_PHASE = register("entity.overcharged_machine_god.phase_transition");
    public static final RegistryObject<SoundEvent> OVERCHARGED_MACHINE_GOD_DEATH_ROAR = register("entity.overcharged_machine_god.death_roar");

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ECConstants.MODID, id)));
    }

    private ModSounds() {
    }
}
