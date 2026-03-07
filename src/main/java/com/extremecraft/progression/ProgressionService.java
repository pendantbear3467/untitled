package com.extremecraft.progression;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncProgressPacket;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public final class ProgressionService {
    private static final UUID LEVEL_HEALTH_MOD = UUID.fromString("b6ab1ed2-1f7e-4965-9f53-19d8b237db0b");
    private static final UUID LEVEL_ATTACK_MOD = UUID.fromString("3a2d5b50-6b16-469f-8e0a-df018dd7707c");
    private static final UUID CLASS_HEALTH_MOD = UUID.fromString("ca6b0177-d863-4be1-a2f6-dbbd7c79e18f");
    private static final UUID CLASS_ATTACK_MOD = UUID.fromString("2f86f095-69a8-4d48-8116-770f9d4f3f70");
    private static final UUID CLASS_SPEED_MOD = UUID.fromString("f0d635c2-a0c4-40f8-a52f-c031adff12c7");
    private static final UUID CLASS_LUCK_MOD = UUID.fromString("730244df-6e1a-42c9-98f7-fbe44f0fd5cb");

    private ProgressionService() {}

    public static void addXp(ServerPlayer player, int amount) {
        ProgressApi.get(player).ifPresent(data -> data.addXp(amount));
        flushDirty(player);
    }

    public static void setLevel(ServerPlayer player, int level) {
        ProgressApi.get(player).ifPresent(data -> data.setLevel(level));
        flushDirty(player);
    }
    public static void addPlayerSkillPoints(ServerPlayer player, int amount) {
        ProgressApi.get(player).ifPresent(data -> data.addPlayerSkillPoints(amount));
        flushDirty(player);
    }

    public static void addClassSkillPoints(ServerPlayer player, int amount) {
        ProgressApi.get(player).ifPresent(data -> data.addClassSkillPoints(amount));
        flushDirty(player);
    }

    public static void unlockClass(ServerPlayer player, String classId) {
        ProgressApi.get(player).ifPresent(data -> data.unlockClass(classId));
        flushDirty(player);
    }

    public static boolean switchClass(ServerPlayer player, String classId) {
        return ProgressApi.get(player).map(data -> {
            if (!data.unlockedClasses().contains(classId)) return false;
            data.setCurrentClass(classId);
            flushDirty(player);
            return true;
        }).orElse(false);
    }

    public static void flushDirty(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> {
            if (data.consumeAttributesDirty()) {
                applyAttributes(player);
            }

            if (data.consumeSyncDirty()) {
                sync(player);
            }
        });
    }

    public static void applyAttributes(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> {
            int level = data.level();
            PlayerClass klass = PlayerClass.byId(data.currentClass()).orElse(PlayerClass.WARRIOR);

            double levelHealth = (level - 1) * 0.4D;
            double levelAttack = (level - 1) * 0.15D;

            applyAdd(player.getAttribute(Attributes.MAX_HEALTH), LEVEL_HEALTH_MOD, "ec_level_health", levelHealth);
            applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), LEVEL_ATTACK_MOD, "ec_level_attack", levelAttack);

            double classHealth = Math.max(0.0D, player.getAttributeValue(Attributes.MAX_HEALTH) * klass.healthBonusPct());
            applyAdd(player.getAttribute(Attributes.MAX_HEALTH), CLASS_HEALTH_MOD, "ec_class_health", classHealth);
            applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), CLASS_ATTACK_MOD, "ec_class_attack", klass.flatAttackBonus());
            applyAdd(player.getAttribute(Attributes.MOVEMENT_SPEED), CLASS_SPEED_MOD, "ec_class_speed", klass.moveSpeedBonus());
            applyAdd(player.getAttribute(Attributes.LUCK), CLASS_LUCK_MOD, "ec_class_luck", klass.luckBonus());

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        });
    }

    public static void sync(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(data -> ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncProgressPacket(data.serializeNBT())
        ));
    }

    private static void applyAdd(AttributeInstance attr, UUID id, String name, double amount) {
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(id);
        if (existing != null) attr.removeModifier(existing);
        if (Math.abs(amount) < 0.00001D) return;
        attr.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}

