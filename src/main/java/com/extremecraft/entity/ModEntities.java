package com.extremecraft.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.AncientCoreGuardianEntity;
import com.extremecraft.entity.boss.OverchargedMachineGodEntity;
import com.extremecraft.entity.boss.VoidTitanEntity;
import com.extremecraft.entity.mob.AncientSentinelEntity;
import com.extremecraft.entity.mob.ArcaneWraithEntity;
import com.extremecraft.entity.mob.EnergyParasiteEntity;
import com.extremecraft.entity.mob.RunicGolemEntity;
import com.extremecraft.entity.mob.TechConstructEntity;
import com.extremecraft.entity.mob.VoidStalkerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ECConstants.MODID);

    public static final RegistryObject<EntityType<TechConstructEntity>> TECH_CONSTRUCT = registerMonster("tech_construct", TechConstructEntity::new, 0.9F, 2.1F);
    public static final RegistryObject<EntityType<ArcaneWraithEntity>> ARCANE_WRAITH = registerMonster("arcane_wraith", ArcaneWraithEntity::new, 0.7F, 2.0F);
    public static final RegistryObject<EntityType<VoidStalkerEntity>> VOID_STALKER = registerMonster("void_stalker", VoidStalkerEntity::new, 0.8F, 1.95F);
    public static final RegistryObject<EntityType<AncientSentinelEntity>> ANCIENT_SENTINEL = registerMonster("ancient_sentinel", AncientSentinelEntity::new, 1.0F, 2.6F);
    public static final RegistryObject<EntityType<EnergyParasiteEntity>> ENERGY_PARASITE = registerMonster("energy_parasite", EnergyParasiteEntity::new, 0.45F, 0.9F);
    public static final RegistryObject<EntityType<RunicGolemEntity>> RUNIC_GOLEM = registerMonster("runic_golem", RunicGolemEntity::new, 1.2F, 2.9F);

    public static final RegistryObject<EntityType<AncientCoreGuardianEntity>> ANCIENT_CORE_GUARDIAN = registerMonster("ancient_core_guardian", AncientCoreGuardianEntity::new, 1.4F, 3.2F);
    public static final RegistryObject<EntityType<VoidTitanEntity>> VOID_TITAN = registerMonster("void_titan", VoidTitanEntity::new, 1.8F, 4.3F);
    public static final RegistryObject<EntityType<OverchargedMachineGodEntity>> OVERCHARGED_MACHINE_GOD = registerMonster("overcharged_machine_god", OverchargedMachineGodEntity::new, 1.6F, 3.8F);

    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<EntityType<T>> registerMonster(String id, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).build(id));
    }

    private ModEntities() {
    }
}
