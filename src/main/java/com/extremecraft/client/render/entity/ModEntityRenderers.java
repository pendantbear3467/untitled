package com.extremecraft.client.render.entity;

import com.extremecraft.client.model.entity.AncientCoreGuardianModel;
import com.extremecraft.client.model.entity.AncientSentinelModel;
import com.extremecraft.client.model.entity.ArcaneWraithModel;
import com.extremecraft.client.model.entity.EnergyParasiteModel;
import com.extremecraft.client.model.entity.OverchargedMachineGodModel;
import com.extremecraft.client.model.entity.RunicGolemModel;
import com.extremecraft.client.model.entity.TechConstructModel;
import com.extremecraft.client.model.entity.VoidStalkerModel;
import com.extremecraft.client.model.entity.VoidTitanModel;
import com.extremecraft.entity.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    /**
     * Registers the canonical runtime entity renderer path.
     *
     * <p>Java layer definitions and renderer classes are the live owner for entity visuals. The
     * JSON files under {@code assets/extremecraft/entities} and {@code assets/extremecraft/models/entity}
     * are handoff metadata only and are not consumed by Forge rendering at runtime.</p>
     */
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TECH_CONSTRUCT.get(), TechConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.ARCANE_WRAITH.get(), ArcaneWraithRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_STALKER.get(), VoidStalkerRenderer::new);
        event.registerEntityRenderer(ModEntities.ANCIENT_SENTINEL.get(), AncientSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.ENERGY_PARASITE.get(), EnergyParasiteRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNIC_GOLEM.get(), RunicGolemRenderer::new);

        event.registerEntityRenderer(ModEntities.ANCIENT_CORE_GUARDIAN.get(), AncientCoreGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TITAN.get(), VoidTitanRenderer::new);
        event.registerEntityRenderer(ModEntities.OVERCHARGED_MACHINE_GOD.get(), OverchargedMachineGodRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TechConstructModel.LAYER_LOCATION, TechConstructModel::createBodyLayer);
        event.registerLayerDefinition(ArcaneWraithModel.LAYER_LOCATION, ArcaneWraithModel::createBodyLayer);
        event.registerLayerDefinition(VoidStalkerModel.LAYER_LOCATION, VoidStalkerModel::createBodyLayer);
        event.registerLayerDefinition(AncientSentinelModel.LAYER_LOCATION, AncientSentinelModel::createBodyLayer);
        event.registerLayerDefinition(EnergyParasiteModel.LAYER_LOCATION, EnergyParasiteModel::createBodyLayer);
        event.registerLayerDefinition(RunicGolemModel.LAYER_LOCATION, RunicGolemModel::createBodyLayer);

        event.registerLayerDefinition(AncientCoreGuardianModel.LAYER_LOCATION, AncientCoreGuardianModel::createBodyLayer);
        event.registerLayerDefinition(VoidTitanModel.LAYER_LOCATION, VoidTitanModel::createBodyLayer);
        event.registerLayerDefinition(OverchargedMachineGodModel.LAYER_LOCATION, OverchargedMachineGodModel::createBodyLayer);
    }
}
