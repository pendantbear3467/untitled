package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.EnergyParasiteModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.EnergyParasiteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class EnergyParasiteRenderer extends MobRenderer<EnergyParasiteEntity, EnergyParasiteModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/energy_parasite.png");

    public EnergyParasiteRenderer(EntityRendererProvider.Context context) {
        super(context, new EnergyParasiteModel(context.bakeLayer(EnergyParasiteModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyParasiteEntity entity) {
        return TEXTURE;
    }
}
