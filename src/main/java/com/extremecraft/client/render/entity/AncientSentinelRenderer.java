package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.AncientSentinelModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.AncientSentinelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class AncientSentinelRenderer extends MobRenderer<AncientSentinelEntity, AncientSentinelModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/ancient_sentinel.png");

    public AncientSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new AncientSentinelModel(context.bakeLayer(AncientSentinelModel.LAYER_LOCATION)), 0.65F);
    }

    @Override
    public ResourceLocation getTextureLocation(AncientSentinelEntity entity) {
        return TEXTURE;
    }
}
