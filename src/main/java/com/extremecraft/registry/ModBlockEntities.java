package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machines.pulverizer.PulverizerBlockEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ECConstants.MODID);

    public static final RegistryObject<BlockEntityType<PulverizerBlockEntity>> PULVERIZER_BE = BLOCK_ENTITIES.register("pulverizer", () ->
            BlockEntityType.Builder.of(PulverizerBlockEntity::new, ModBlocks.PULVERIZER.get()).build(null)
    );

    private ModBlockEntities() {}
}
