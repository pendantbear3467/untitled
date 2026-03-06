package com.extremecraft.magic;

import com.extremecraft.ability.AbilityCooldownManager;
import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.progression.BuffStackingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpellCastingSystem {
    private static final Map<UUID, ActiveChannel> ACTIVE_CHANNELS = new LinkedHashMap<>();

    private SpellCastingSystem() {
    }

    public static boolean tryCast(ServerPlayer player, String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return false;
        }

        SpellDefinition spell = SpellRegistry.get(spellId);
        if (spell == null) {
            return false;
        }

        if (!ClassAbilityBindings.canUseSpell(player, spell.id())) {
            return false;
        }

        String cooldownKey = "spell:" + spell.id();
        if (!AbilityCooldownManager.isReady(player, cooldownKey)) {
            return false;
        }

        if (!ManaService.tryConsume(player, spell.manaCost())) {
            return false;
        }

        boolean cast = switch (spell.type()) {
            case INSTANT -> castInstant(player, spell);
            case AREA -> castArea(player, spell);
            case PROJECTILE -> castProjectile(player, spell);
            case CHANNEL -> castChannel(player, spell);
            case SUMMON -> castSummon(player, spell);
        };

        if (cast) {
            AbilityCooldownManager.startCooldown(player, cooldownKey, spell.cooldownTicks());
        }

        return cast;
    }

    public static void tickChanneling(ServerPlayer player) {
        ActiveChannel channel = ACTIVE_CHANNELS.get(player.getUUID());
        if (channel == null) {
            return;
        }

        SpellDefinition spell = SpellRegistry.get(channel.spellId);
        if (spell == null) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        if (channel.endTick <= player.level().getGameTime()) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        if (player.level().getGameTime() < channel.nextPulseTick) {
            return;
        }

        if (!ManaApi.get(player).map(mana -> mana.consume(Math.max(1.0D, spell.manaCost() * 0.2D))).orElse(false)) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        castArea(player, spell);
        channel.nextPulseTick = player.level().getGameTime() + 10;
        ManaService.sync(player);
    }

    private static boolean castInstant(ServerPlayer player, SpellDefinition spell) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(4.0D),
                entity -> entity.isAlive() && entity != player
        );

        if (targets.isEmpty()) {
            return false;
        }

        applyEffects(player, spell.effects(), targets);
        return true;
    }

    private static boolean castArea(ServerPlayer player, SpellDefinition spell) {
        double radius = Math.max(1.0D, spell.radius() <= 0.0D ? 5.0D : spell.radius());
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(radius),
                entity -> entity.isAlive() && entity != player
        );

        if (targets.isEmpty()) {
            return false;
        }

        applyEffects(player, spell.effects(), targets);
        return true;
    }

    private static boolean castProjectile(ServerPlayer player, SpellDefinition spell) {
        return new SpellProjectile(player, spell.speed(), 24.0D)
                .cast()
                .map(target -> {
                    applyEffects(player, spell.effects(), List.of(target));
                    return true;
                })
                .orElse(false);
    }

    private static boolean castChannel(ServerPlayer player, SpellDefinition spell) {
        ACTIVE_CHANNELS.put(player.getUUID(), new ActiveChannel(
                spell.id(),
                player.level().getGameTime() + Math.max(20, spell.channelTicks()),
                player.level().getGameTime() + 1
        ));
        return true;
    }

    private static boolean castSummon(ServerPlayer player, SpellDefinition spell) {
        ResourceLocation summonId = ResourceLocation.tryParse(spell.summonEntity());
        if (summonId == null) {
            return false;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(summonId);
        if (type == null || type == EntityType.PIG) {
            return false;
        }

        if (type.create(player.level()) instanceof Mob mob) {
            mob.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.level().addFreshEntity(mob);
            return true;
        }

        return false;
    }

    private static void applyEffects(ServerPlayer caster, List<SpellEffect> effects, List<LivingEntity> targets) {
        for (SpellEffect effect : effects) {
            switch (effect.type()) {
                case "damage" -> {
                    float amount = (float) Math.max(0.0D, effect.value());
                    for (LivingEntity target : targets) {
                        target.hurt(caster.damageSources().playerAttack(caster), amount);
                    }
                }
                case "ignite" -> {
                    int seconds = Math.max(1, effect.duration());
                    for (LivingEntity target : targets) {
                        target.setSecondsOnFire(seconds);
                    }
                }
                case "heal" -> {
                    float amount = (float) Math.max(0.0D, effect.value());
                    for (LivingEntity target : targets) {
                        target.heal(amount);
                    }
                }
                case "buff", "debuff" -> {
                    ResourceLocation effectId = ResourceLocation.tryParse(effect.id());
                    if (effectId == null) {
                        continue;
                    }

                    MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                    if (mobEffect == null) {
                        continue;
                    }

                    int durationTicks = Math.max(20, effect.duration() * 20);
                    MobEffectInstance instance = new MobEffectInstance(mobEffect, durationTicks, effect.amplifier());
                    for (LivingEntity target : targets) {
                        target.addEffect(instance);
                        BuffStackingSystem.track(target, effect.type() + ":" + effect.id(), durationTicks, effect.amplifier());
                    }
                }
                default -> {
                }
            }
        }
    }

    private static final class ActiveChannel {
        private final String spellId;
        private final long endTick;
        private long nextPulseTick;

        private ActiveChannel(String spellId, long endTick, long nextPulseTick) {
            this.spellId = spellId;
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
        }
    }
}
