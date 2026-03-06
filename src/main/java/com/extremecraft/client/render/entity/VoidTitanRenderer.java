package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.VoidTitanModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.VoidTitanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class VoidTitanRenderer extends MobRenderer<VoidTitanEntity, VoidTitanModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/void_titan.png");

    public VoidTitanRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidTitanModel(context.bakeLayer(VoidTitanModel.LAYER_LOCATION)), 1.05F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTitanEntity entity) {
        return TEXTURE;
    }
}
