package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.OverchargedMachineGodEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public final class OverchargedMachineGodModel extends ECBipedEntityModel<OverchargedMachineGodEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ECConstants.MODID, "overcharged_machine_god"), "main");

    public OverchargedMachineGodModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(new CubeDeformation(0.5F));
    }
}
