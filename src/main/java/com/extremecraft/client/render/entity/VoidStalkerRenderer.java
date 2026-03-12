package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.VoidStalkerModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.VoidStalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class VoidStalkerRenderer extends MobRenderer<VoidStalkerEntity, VoidStalkerModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/entity/void_stalker.png");

    public VoidStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidStalkerModel(context.bakeLayer(VoidStalkerModel.LAYER_LOCATION)), 0.52F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidStalkerEntity entity) {
        return TEXTURE;
    }
}
