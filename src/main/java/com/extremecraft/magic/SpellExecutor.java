package com.extremecraft.magic;

import com.extremecraft.ability.AbilityCooldownManager;
import com.extremecraft.ability.AbilityDefinition;
import com.extremecraft.ability.AbilityEffect;
import com.extremecraft.ability.AbilityExecutor;
import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.SpellCastContext.CastSource;
import com.extremecraft.magic.Spell.SpellType;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.modules.item.ArcaneStaffItem;
import com.extremecraft.network.sync.RuntimeSyncService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellExecutor {
    public static final String SPELL_TAG = "ec_spell_id";

    private static final Map<UUID, ActiveChannel> ACTIVE_CHANNELS = new ConcurrentHashMap<>();

    private SpellExecutor() {
    }

    public static boolean tryCast(ServerPlayer player, String spellId) {
        return tryCast(player, spellId, CastSource.SYSTEM);
    }

    public static boolean tryCast(ServerPlayer player, String spellId, CastSource source) {
        if (player == null || player.isSpectator() || spellId == null || spellId.isBlank()) {
            return false;
        }

        Spell spell = SpellRegistry.get(spellId);
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

        SpellCastContext context = SpellCastContext.create(player, spell, source);
        if (!ManaService.tryConsume(player, context.scaledManaCost())) {
            return false;
        }

        boolean cast = executeSpell(context, false);
        if (!cast) {
            return false;
        }

        AbilityCooldownManager.startCooldown(player, cooldownKey, spell.cooldownTicks());
        RuntimeSyncService.syncAbilities(player);
        triggerCastFeedback(context, false);
        return true;
    }

    public static boolean tryCastFromEquipped(ServerPlayer player, CastSource source) {
        if (player == null) {
            return false;
        }

        if (tryCastFromStack(player, player.getMainHandItem(), source)) {
            return true;
        }

        return tryCastFromStack(player, player.getOffhandItem(), source);
    }

    public static boolean tryCastFromStack(ServerPlayer player, ItemStack stack, CastSource source) {
        if (stack == null || stack.isEmpty() || !isSpellCasterItem(stack)) {
            return false;
        }

        String spellId = resolveSpellId(stack);
        if (spellId.isBlank()) {
            return false;
        }

        return tryCast(player, spellId, source);
    }

    public static void tickChanneling(ServerPlayer player) {
        ActiveChannel active = ACTIVE_CHANNELS.get(player.getUUID());
        if (active == null) {
            return;
        }

        Spell spell = SpellRegistry.get(active.spellId());
        if (spell == null || spell.type() != SpellType.CHANNEL) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        if (now >= active.endTick()) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        if (now < active.nextPulseTick()) {
            return;
        }

        SpellCastContext context = SpellCastContext.create(player, spell, active.source());
        int pulseManaCost = Math.max(1, context.scaledManaCost() / 5);
        if (!ManaService.tryConsume(player, pulseManaCost)) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        if (executeSpell(context, true)) {
            triggerCastFeedback(context, true);
        }

        ACTIVE_CHANNELS.put(player.getUUID(), new ActiveChannel(active.spellId(), active.endTick(), now + 10L, active.source()));
        RuntimeSyncService.syncAbilities(player);
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            clearPlayer(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            ACTIVE_CHANNELS.remove(playerId);
        }
    }

    public static String resolveSpellId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        String bound = stack.hasTag() ? stack.getTag().getString(SPELL_TAG).trim().toLowerCase() : "";
        if (!bound.isBlank() && SpellRegistry.get(bound) != null) {
            return bound;
        }

        return isSpellCasterItem(stack) ? SpellRegistry.firstSpellId() : "";
    }

    public static void bindSpell(ItemStack stack, String spellId) {
        if (stack == null || stack.isEmpty() || spellId == null || spellId.isBlank()) {
            return;
        }

        String normalized = spellId.trim().toLowerCase();
        if (SpellRegistry.get(normalized) == null) {
            return;
        }

        stack.getOrCreateTag().putString(SPELL_TAG, normalized);
    }

    public static boolean isSpellCasterItem(ItemStack stack) {
        return stack.getItem() instanceof ArcaneStaffItem || stack.getItem() instanceof SpellBookItem;
    }

    private static boolean executeSpell(SpellCastContext context, boolean channelPulse) {
        if (context.spell().type() == SpellType.CHANNEL && !channelPulse) {
            long now = context.caster().level().getGameTime();
            long duration = Math.max(20, context.spell().channelTicks());
            ACTIVE_CHANNELS.put(context.caster().getUUID(), new ActiveChannel(context.spell().id(), now + duration, now + 1L, context.source()));
            return true;
        }

        AbilityDefinition definition = toAbilityDefinition(context, channelPulse);
        boolean cast = AbilityExecutor.executeSpellAbility(context.caster(), definition);

        if (cast) {
            context.caster().swing(InteractionHand.MAIN_HAND, true);
        }

        return cast;
    }

    private static AbilityDefinition toAbilityDefinition(SpellCastContext context, boolean channelPulse) {
        Spell spell = context.spell();
        AbilityDefinition.TargetType targetType = targetTypeFor(spell, channelPulse);
        double radius = context.scaledRadius(spell.radius() <= 0.0D ? 4.0D : spell.radius());
        double range = Math.max(1.0D, spell.range());
        List<AbilityEffect> effects = toAbilityEffects(context, channelPulse);

        return new AbilityDefinition(
                "spell:" + spell.id(),
                0,
                0,
                targetType,
                radius,
                range,
                "",
                effects
        );
    }

    private static AbilityDefinition.TargetType targetTypeFor(Spell spell, boolean channelPulse) {
        if (channelPulse) {
            return AbilityDefinition.TargetType.AREA;
        }

        return switch (spell.type()) {
            case PROJECTILE -> AbilityDefinition.TargetType.PROJECTILE;
            case AREA -> AbilityDefinition.TargetType.AREA;
            case BUFF -> AbilityDefinition.TargetType.SELF;
            case DEBUFF -> spell.radius() > 1.0D ? AbilityDefinition.TargetType.AREA : AbilityDefinition.TargetType.ENTITY;
            case SUMMON -> AbilityDefinition.TargetType.NONE;
            case BEAM, INSTANT -> AbilityDefinition.TargetType.ENTITY;
            case CHANNEL -> AbilityDefinition.TargetType.AREA;
        };
    }

    private static List<AbilityEffect> toAbilityEffects(SpellCastContext context, boolean channelPulse) {
        Spell spell = context.spell();
        List<AbilityEffect> effects = new LinkedList<>();

        for (SpellEffect effect : spell.effects()) {
            String type = effect.type();
            int seconds = effect.duration();
            int fallbackSeconds = Math.max(1, context.scaledDurationTicks(spell.durationTicks()) / 20);
            int durationSeconds = Math.max(seconds, fallbackSeconds);

            double value = effect.value();
            if ("damage".equals(type) || "heal".equals(type)) {
                value = context.scaledDamage(value);
            }

            effects.add(new AbilityEffect(type, value, durationSeconds, effect.amplifier(), effect.id(), Map.of()));
        }

        if (effects.isEmpty()) {
            double baseDamage = applySpellModifiers(spell, "damage", spell.damage());
            if (baseDamage > 0.0D) {
                effects.add(new AbilityEffect("damage", context.scaledDamage(baseDamage), 0, 0, "", Map.of()));
            }

            if (spell.type() == SpellType.SUMMON && !spell.summonEntity().isBlank()) {
                effects.add(new AbilityEffect("summon", 0.0D, 0, 0, spell.summonEntity(), Map.of()));
            }

            if (spell.type() == SpellType.BUFF) {
                int seconds = Math.max(2, context.scaledDurationTicks(spell.durationTicks()) / 20);
                effects.add(new AbilityEffect("buff", 0.0D, seconds, 0, "minecraft:damage_resistance", Map.of()));
            }

            if (spell.type() == SpellType.DEBUFF) {
                int seconds = Math.max(2, context.scaledDurationTicks(spell.durationTicks()) / 20);
                effects.add(new AbilityEffect("debuff", 0.0D, seconds, 0, "minecraft:weakness", Map.of()));
            }
        }

        if (spell.type() == SpellType.CHANNEL || channelPulse) {
            if (effects.stream().noneMatch(effect -> "damage".equals(effect.type()) || "heal".equals(effect.type()))) {
                effects.add(new AbilityEffect("damage", context.scaledDamage(Math.max(1.0D, spell.damage() * 0.5D)), 0, 0, "", Map.of()));
            }
        }

        if (spell.id().contains("blink") && effects.stream().noneMatch(effect -> "teleport".equals(effect.type()))) {
            double distance = Math.max(3.0D, applySpellModifiers(spell, "teleport_distance", 6.0D));
            effects.add(new AbilityEffect("teleport", 0.0D, 0, 0, "", Map.of("distance", distance)));
        }

        return List.copyOf(effects);
    }

    private static double applySpellModifiers(Spell spell, String modifierId, double base) {
        double value = base;
        for (SpellModifier modifier : spell.modifiers()) {
            if (!modifier.id().equalsIgnoreCase(modifierId)) {
                continue;
            }

            value = switch (modifier.operation()) {
                case ADD -> value + modifier.value();
                case MULTIPLY -> value * modifier.value();
                case PERCENT -> value * (1.0D + modifier.value());
            };
        }
        return value;
    }

    private static void triggerCastFeedback(SpellCastContext context, boolean pulse) {
        ServerPlayer player = context.caster();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        SimpleParticleType particle = resolveParticleType(context.spell());
        int count = pulse ? 8 : 18;
        double speed = pulse ? 0.01D : 0.03D;
        serverLevel.sendParticles(particle, player.getX(), player.getEyeY() - 0.2D, player.getZ(), count, 0.3D, 0.45D, 0.3D, speed);

        SoundEvent sound = resolveSound(context.spell());
        float volume = pulse ? 0.55F : 0.9F;
        float pitch = pulse ? 1.2F : 1.0F;
        serverLevel.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static SimpleParticleType resolveParticleType(Spell spell) {
        String particleId = spell.particle();
        if (!particleId.isBlank()) {
            ResourceLocation location = withDefaultNamespace(particleId);
            if (location != null) {
                var particle = BuiltInRegistries.PARTICLE_TYPE.get(location);
                if (particle instanceof SimpleParticleType simple) {
                    return simple;
                }
            }
        }

        return switch (spell.element()) {
            case "fire" -> ParticleTypes.FLAME;
            case "lightning", "storm", "electric" -> ParticleTypes.ELECTRIC_SPARK;
            case "healing", "holy" -> ParticleTypes.HEART;
            case "arcane" -> ParticleTypes.ENCHANT;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static SoundEvent resolveSound(Spell spell) {
        String soundId = spell.sound();
        if (!soundId.isBlank()) {
            ResourceLocation location = withDefaultNamespace(soundId);
            if (location != null) {
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(location);
                if (sound != null) {
                    return sound;
                }
            }
        }

        return switch (spell.element()) {
            case "fire" -> SoundEvents.BLAZE_SHOOT;
            case "lightning", "storm", "electric" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case "healing", "holy" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "arcane" -> SoundEvents.EVOKER_CAST_SPELL;
            default -> SoundEvents.ENCHANTMENT_TABLE_USE;
        };
    }

    private static ResourceLocation withDefaultNamespace(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase();
        if (normalized.isBlank()) {
            return null;
        }

        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        return ResourceLocation.tryParse(normalized);
    }

    private record ActiveChannel(String spellId, long endTick, long nextPulseTick, CastSource source) {
    }
}







