package com.extremecraft.client.model.entity;

import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.mob.ArcaneWraithEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ArcaneWraithModel extends ECBipedEntityModel<ArcaneWraithEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "arcane_wraith"), "main");

    public ArcaneWraithModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        CubeDeformation deformation = new CubeDeformation(0.1F);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation)
                .texOffs(32, 0)
                .addBox(-5.0F, -9.5F, -1.0F, 10.0F, 2.0F, 2.0F),
            PartPose.offset(0.0F, 8.0F, 0.0F));
        head.addOrReplaceChild("veil", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-4.5F, -1.0F, 3.5F, 9.0F, 10.0F, 0.0F),
            PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(20, 16)
                .addBox(-3.5F, -12.0F, -2.0F, 7.0F, 10.0F, 4.0F, deformation)
                .texOffs(0, 26)
                .addBox(-6.0F, -2.0F, -2.5F, 12.0F, 10.0F, 5.0F, new CubeDeformation(0.35F)),
            PartPose.offset(0.0F, 20.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(34, 30)
                .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 14.0F, 3.0F, deformation)
                .texOffs(46, 30)
                .addBox(-2.5F, 4.0F, -0.5F, 2.0F, 8.0F, 1.0F),
            PartPose.offset(-6.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(34, 30)
                .mirror()
                .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 14.0F, 3.0F, deformation)
                .mirror(false)
                .texOffs(46, 30)
                .mirror()
                .addBox(0.5F, 4.0F, -0.5F, 2.0F, 8.0F, 1.0F)
                .mirror(false),
            PartPose.offset(6.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(48, 16)
                .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 10.0F, 3.0F, deformation),
            PartPose.offset(-2.0F, 24.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(48, 16)
                .mirror()
                .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 10.0F, 3.0F, deformation)
                .mirror(false),
            PartPose.offset(2.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
