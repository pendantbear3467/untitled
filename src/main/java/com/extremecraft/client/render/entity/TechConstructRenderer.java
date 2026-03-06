package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.TechConstructModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.TechConstructEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class TechConstructRenderer extends MobRenderer<TechConstructEntity, TechConstructModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/tech_construct.png");

    public TechConstructRenderer(EntityRendererProvider.Context context) {
        super(context, new TechConstructModel(context.bakeLayer(TechConstructModel.LAYER_LOCATION)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(TechConstructEntity entity) {
        return TEXTURE;
    }
}
