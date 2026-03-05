package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.cable.CableBlockEntity;
import com.extremecraft.machine.core.TechMachineBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ECConstants.MODID);

    public static final RegistryObject<BlockEntityType<TechMachineBlockEntity>> TECH_MACHINE = BLOCK_ENTITIES.register("tech_machine", () ->
            BlockEntityType.Builder.of(TechMachineBlockEntity::new, TechBlocks.machineBlocks().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null)
    );

    public static final RegistryObject<BlockEntityType<CableBlockEntity>> CABLE = BLOCK_ENTITIES.register("cable", () ->
            BlockEntityType.Builder.of(CableBlockEntity::new, TechBlocks.cableBlocks().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null)
    );

    private TechBlockEntities() {
    }
}
