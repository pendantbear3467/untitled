package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.RunicGolemEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class RunicGolemModel extends ECBipedEntityModel<RunicGolemEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "runic_golem"), "main");

    public RunicGolemModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        CubeDeformation deformation = new CubeDeformation(0.35F);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.5F, -8.5F, -4.0F, 9.0F, 9.0F, 8.0F, deformation)
                .texOffs(36, 0)
                .addBox(-1.5F, -11.5F, -1.5F, 3.0F, 3.0F, 3.0F),
            PartPose.offset(0.0F, 6.5F, 0.0F));

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(-6.0F, -14.0F, -3.5F, 12.0F, 14.0F, 7.0F, deformation)
                .texOffs(38, 18)
                .addBox(-8.5F, -12.0F, -3.0F, 3.0F, 5.0F, 6.0F)
                .addBox(5.5F, -12.0F, -3.0F, 3.0F, 5.0F, 6.0F),
            PartPose.offset(0.0F, 21.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 40)
                .addBox(-4.0F, -10.0F, -3.0F, 6.0F, 15.0F, 6.0F, deformation)
                .texOffs(24, 40)
                .addBox(-4.5F, 5.0F, -3.5F, 7.0F, 5.0F, 7.0F),
            PartPose.offset(-8.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 40)
                .mirror()
                .addBox(-2.0F, -10.0F, -3.0F, 6.0F, 15.0F, 6.0F, deformation)
                .mirror(false)
                .texOffs(24, 40)
                .mirror()
                .addBox(-2.5F, 5.0F, -3.5F, 7.0F, 5.0F, 7.0F)
                .mirror(false),
            PartPose.offset(8.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(52, 38)
                .addBox(-3.0F, -12.0F, -3.0F, 5.0F, 12.0F, 6.0F, deformation),
            PartPose.offset(-3.5F, 24.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(52, 38)
                .mirror()
                .addBox(-2.0F, -12.0F, -3.0F, 5.0F, 12.0F, 6.0F, deformation)
                .mirror(false),
            PartPose.offset(3.5F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 96, 96);
    }
}
