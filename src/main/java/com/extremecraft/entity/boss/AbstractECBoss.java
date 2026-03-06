package com.extremecraft.entity.boss;

import com.extremecraft.entity.mob.AbstractECMonster;
import com.extremecraft.entity.mob.ArcaneWraithEntity;
import com.extremecraft.entity.mob.EnergyParasiteEntity;
import com.extremecraft.entity.mob.VoidStalkerEntity;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class AbstractECBoss extends AbstractECMonster {
    private final ServerBossEvent bossEvent;
    private boolean shieldPhase;

    protected AbstractECBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
        this.xpReward = 200;
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

        if (!shieldPhase && this.getHealth() <= this.getMaxHealth() * 0.45F) {
            shieldPhase = true;
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 3));
            summonWave();
        }

        if (this.tickCount % 240 == 0) {
            summonWave();
        }
    }

    private void summonWave() {
        if (level() == null || level().isClientSide) {
            return;
        }
        spawnMinion(EntityType.ZOMBIE);
        spawnMinion(EntityType.SKELETON);
        if (this.random.nextBoolean()) {
            spawnMinion(EntityType.SPIDER);
        }
    }

    private void spawnMinion(EntityType<? extends Monster> type) {
        Monster mob = type.create(level());
        if (mob == null) {
            return;
        }
        mob.moveTo(getX() + (random.nextDouble() - 0.5D) * 6.0D, getY(), getZ() + (random.nextDouble() - 0.5D) * 6.0D, random.nextFloat() * 360.0F, 0.0F);
        level().addFreshEntity(mob);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (shieldPhase && this.tickCount % 20 < 8) {
            amount *= 0.4F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
