package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.AncientCoreGuardianEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public final class AncientCoreGuardianModel extends ECBipedEntityModel<AncientCoreGuardianEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ECConstants.MODID, "ancient_core_guardian"), "main");

    public AncientCoreGuardianModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(new CubeDeformation(0.42F));
    }
}
