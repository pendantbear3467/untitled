package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.VoidTitanEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public final class VoidTitanModel extends ECBipedEntityModel<VoidTitanEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ECConstants.MODID, "void_titan"), "main");

    public VoidTitanModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(new CubeDeformation(0.62F));
    }
}
