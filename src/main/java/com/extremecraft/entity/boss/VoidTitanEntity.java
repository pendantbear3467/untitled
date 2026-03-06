package com.extremecraft.entity.boss;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import com.extremecraft.entity.ModEntities;
import com.extremecraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VoidTitanEntity extends AbstractECBoss {
    public VoidTitanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    @Override
    protected void performPhaseTick(int phase) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        int teleportCadence = phase == 1 ? 120 : (phase == 2 ? 90 : 60);
        if (this.tickCount % teleportCadence == 0) {
            teleportAttack(target);
        }

        int burstCadence = phase == 1 ? 130 : (phase == 2 ? 85 : 55);
        if (this.tickCount % burstCadence == 0) {
            projectileBurst(target, phase >= 3 ? 5 : 3);
        }

        if (phase == 3 && this.tickCount % 80 == 0) {
            // Rage mode below 30% HP.
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2));
            this.pulseParticles(ParticleTypes.DRAGON_BREATH, 28, 1.0D, 0.04D);
        }
    }

    @Override
    protected void summonBossMinions(int phase) {
        int count = phase >= 3 ? 3 : (phase == 2 ? 2 : 1);
        spawnMinions(ModEntities.VOID_STALKER.get(), count, 9.0D);
    }

    @Override
    protected SoundEvent getAttackSoundEvent() {
        return ModSounds.VOID_TITAN_ATTACK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.VOID_TITAN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VOID_TITAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VOID_TITAN_DEATH.get();
    }

    @Override
    protected SoundEvent getIntroSound() {
        return ModSounds.VOID_TITAN_INTRO.get();
    }

    @Override
    protected SoundEvent getPhaseTransitionSound() {
        return ModSounds.VOID_TITAN_PHASE.get();
    }

    @Override
    protected SoundEvent getBossDeathRoar() {
        return ModSounds.VOID_TITAN_DEATH_ROAR.get();
    }

    private void teleportAttack(LivingEntity target) {
        Vec3 backward = target.getLookAngle().normalize().scale(-2.8D);
        double x = target.getX() + backward.x + (this.random.nextDouble() - 0.5D) * 1.6D;
        double z = target.getZ() + backward.z + (this.random.nextDouble() - 0.5D) * 1.6D;
        double y = target.getY();
        if (this.randomTeleport(x, y, z, true)) {
            this.pulseParticles(ParticleTypes.PORTAL, 34, 0.45D, 0.08D);
            CombatEngine.applyDamage(DamageContext.builder()
                    .attacker(this)
                    .target(target)
                    .damageAmount((float) (this.attackDamage() * 0.85D + 2.0D))
                    .damageType(DamageType.VOID)
                    .abilitySource("boss:void_teleport_strike")
                    .weaponSource(ItemStack.EMPTY)
                    .armorValue(target.getArmorValue())
                    .build());
        }
    }

    private void projectileBurst(LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition())
                    .add((this.random.nextDouble() - 0.5D) * 0.35D, (this.random.nextDouble() - 0.5D) * 0.25D, (this.random.nextDouble() - 0.5D) * 0.35D)
                    .normalize()
                    .scale(0.9D);

            WitherSkull skull = new WitherSkull(this.level(), this, toTarget.x, toTarget.y, toTarget.z);
            skull.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(skull);
        }

        this.pulseParticles(ParticleTypes.SCULK_SOUL, 20, 0.55D, 0.03D);
    }
}
