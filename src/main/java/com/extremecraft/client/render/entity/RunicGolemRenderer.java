package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.RunicGolemModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.RunicGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RunicGolemRenderer extends MobRenderer<RunicGolemEntity, RunicGolemModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/entity/runic_golem.png");

    public RunicGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new RunicGolemModel(context.bakeLayer(RunicGolemModel.LAYER_LOCATION)), 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(RunicGolemEntity entity) {
        return TEXTURE;
    }
}
