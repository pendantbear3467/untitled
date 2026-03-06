package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.OverchargedMachineGodModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.OverchargedMachineGodEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class OverchargedMachineGodRenderer extends MobRenderer<OverchargedMachineGodEntity, OverchargedMachineGodModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/overcharged_machine_god.png");

    public OverchargedMachineGodRenderer(EntityRendererProvider.Context context) {
        super(context, new OverchargedMachineGodModel(context.bakeLayer(OverchargedMachineGodModel.LAYER_LOCATION)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(OverchargedMachineGodEntity entity) {
        return TEXTURE;
    }
}
