package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.ArcaneWraithModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.ArcaneWraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ArcaneWraithRenderer extends MobRenderer<ArcaneWraithEntity, ArcaneWraithModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/entity/arcane_wraith.png");

    public ArcaneWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new ArcaneWraithModel(context.bakeLayer(ArcaneWraithModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneWraithEntity entity) {
        return TEXTURE;
    }
}
