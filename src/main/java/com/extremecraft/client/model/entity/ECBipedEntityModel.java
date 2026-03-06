package com.extremecraft.client.model.entity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Shared cube-based biped model with built-in idle, walk, and attack animation.
 */
public abstract class ECBipedEntityModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;

    protected ECBipedEntityModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    protected static LayerDefinition createBodyLayer(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -10.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(-5.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(-1.0F, -10.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation)
                        .mirror(false),
                PartPose.offset(5.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation),
                PartPose.offset(-1.9F, 24.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation)
                        .mirror(false),
                PartPose.offset(1.9F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);

        float headYaw = netHeadYaw * Mth.DEG_TO_RAD;
        float headXRot = headPitch * Mth.DEG_TO_RAD;
        this.head.yRot = headYaw;
        this.head.xRot = headXRot;

        // Walk cycle.
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount;
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.9F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 0.9F * limbSwingAmount;

        // Idle sway.
        this.body.yRot = Mth.sin(ageInTicks * 0.04F) * 0.04F;
        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.04F + 0.03F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.04F + 0.03F;

        // Attack swing.
        float attackAnim = entity.getAttackAnim(0.0F);
        if (attackAnim > 0.0F) {
            float swing = Mth.sin(Mth.sqrt(attackAnim) * (Mth.PI * 2.0F)) * 0.2F;
            this.body.yRot += swing;
            this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
            this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
            this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
            this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;

            float attackProgress = 1.0F - attackAnim;
            attackProgress *= attackProgress;
            attackProgress *= attackProgress;
            attackProgress = 1.0F - attackProgress;

            float attackRot = Mth.sin(attackProgress * Mth.PI);
            float headAssist = Mth.sin(attackAnim * Mth.PI) * -(this.head.xRot - 0.7F) * 0.75F;
            this.rightArm.xRot -= attackRot * 1.6F + headAssist;
            this.rightArm.yRot += this.body.yRot * 1.4F;
            this.rightArm.zRot += Mth.sin(attackAnim * Mth.PI) * -0.4F;
        }
    }
}

