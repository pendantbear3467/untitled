package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.AncientCoreGuardianModel;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.boss.AncientCoreGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class AncientCoreGuardianRenderer extends MobRenderer<AncientCoreGuardianEntity, AncientCoreGuardianModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ECConstants.MODID, "textures/entity/ancient_core_guardian.png");

    public AncientCoreGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new AncientCoreGuardianModel(context.bakeLayer(AncientCoreGuardianModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(AncientCoreGuardianEntity entity) {
        return TEXTURE;
    }
}
