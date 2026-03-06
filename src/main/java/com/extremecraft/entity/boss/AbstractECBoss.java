package com.extremecraft.entity.boss;

import com.extremecraft.entity.mob.AbstractECMonster;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class AbstractECBoss extends AbstractECMonster {
    private final ServerBossEvent bossEvent;
    private int phase;
    private boolean introSoundPlayed;

    protected AbstractECBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
        this.phase = 1;
        this.xpReward = 240;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (!introSoundPlayed) {
            introSoundPlayed = true;
            this.playSound(getIntroSound(), 1.6F, 0.95F);
        }

        int nextPhase = resolvePhase();
        if (nextPhase != this.phase) {
            this.phase = nextPhase;
            onPhaseChanged(nextPhase);
        }

        int summonCadence = switch (this.phase) {
            case 1 -> 220;
            case 2 -> 170;
            default -> 110;
        };

        if (this.tickCount % summonCadence == 0) {
            summonBossMinions(this.phase);
        }

        performPhaseTick(this.phase);
    }

    protected int currentPhase() {
        return this.phase;
    }

    protected int resolvePhase() {
        float healthRatio = this.getHealth() / this.getMaxHealth();
        if (healthRatio <= 0.30F) {
            return 3;
        }
        if (healthRatio <= 0.60F) {
            return 2;
        }
        return 1;
    }

    protected void onPhaseChanged(int newPhase) {
        this.playSound(getPhaseTransitionSound(), 1.3F, 0.9F + this.random.nextFloat() * 0.2F);
        if (newPhase == 2) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
        } else if (newPhase == 3) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        }
    }

    protected abstract void performPhaseTick(int phase);

    protected abstract void summonBossMinions(int phase);

    protected void spawnMinions(EntityType<? extends Monster> type, int count, double spread) {
        if (this.level().isClientSide) {
            return;
        }

        for (int i = 0; i < count; i++) {
            Monster mob = type.create(this.level());
            if (mob == null) {
                continue;
            }

            double x = this.getX() + (this.random.nextDouble() - 0.5D) * spread;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * spread;
            mob.moveTo(x, this.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
            mob.setTarget(this.getTarget());
            this.level().addFreshEntity(mob);
        }

        this.playSound(getBossAttackSound(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    protected void pulseAreaDamage(double radius, float damage, double yKnockback) {
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive());

        for (LivingEntity entity : nearby) {
            entity.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 push = entity.position().subtract(this.position());
            Vec3 flat = new Vec3(push.x, 0.0D, push.z);
            if (flat.lengthSqr() < 1.0E-4D) {
                flat = new Vec3(0.0D, 0.0D, 1.0D);
            }
            Vec3 normalized = flat.normalize().scale(0.8D);
            entity.push(normalized.x, yKnockback, normalized.z);
        }
    }

    protected void pulseParticles(ParticleOptions particle, int count, double spread, double speed) {
        emitAbilityParticles(particle, count, spread, speed);
    }

    protected SoundEvent getIntroSound() {
        return SoundEvents.WITHER_SPAWN;
    }

    protected SoundEvent getPhaseTransitionSound() {
        return SoundEvents.BEACON_POWER_SELECT;
    }

    protected SoundEvent getBossAttackSound() {
        return SoundEvents.IRON_GOLEM_ATTACK;
    }

    protected float incomingDamageMultiplier() {
        return this.phase == 2 ? 0.70F : 1.0F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount * incomingDamageMultiplier());
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION || spawnType == MobSpawnType.SPAWNER) {
            return false;
        }
        return super.checkSpawnRules(level, spawnType);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
