package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.AncientSentinelEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public final class AncientSentinelModel extends ECBipedEntityModel<AncientSentinelEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ECConstants.MODID, "ancient_sentinel"), "main");

    public AncientSentinelModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(new CubeDeformation(0.18F));
    }
}
