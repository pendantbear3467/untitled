package com.extremecraft.progression.skilltree;

import com.extremecraft.net.DwNetwork;
import com.extremecraft.progression.ProgressionService;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class SkillTreeService {
    private SkillTreeService() {}

    public static boolean tryUnlock(ServerPlayer player, String treeId, String nodeId) {
        SkillTree tree = SkillTreeRegistry.get(treeId);
        if (tree == null) {
            return false;
        }

        SkillNode node = tree.getNode(nodeId);
        if (node == null) {
            return false;
        }

        if (node.cost() <= 0) {
            return false;
        }

        return PlayerSkillTreeApi.get(player).map(data -> {
            if (data.isUnlocked(treeId, nodeId)) {
                return false;
            }

            for (String required : node.requiredNodes()) {
                if (!data.isUnlocked(treeId, required)) {
                    return false;
                }
            }

            boolean consumed = ProgressApi.get(player).map(progress -> progress.consumePlayerSkillPoints(node.cost())).orElse(false);
            if (!consumed) {
                return false;
            }

            boolean unlocked = data.unlock(treeId, nodeId);
            if (!unlocked) {
                return false;
            }

            applySkillModifiers(player, data);
            ProgressionService.flushDirty(player);
            sync(player, data);
            return true;
        }).orElse(false);
    }

    public static void flushDirty(ServerPlayer player) {
        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            if (data.consumeDirty()) {
                applySkillModifiers(player, data);
                sync(player, data);
            }
        });
    }

    public static void sync(ServerPlayer player, PlayerSkillData data) {
        DwNetwork.CH.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillTreeDataS2C(data.serializeNBT()));
    }

    private static void applySkillModifiers(ServerPlayer player, PlayerSkillData data) {
        clearSkillModifiers(player);

        for (String unlocked : data.unlockedNodes()) {
            String[] split = unlocked.split(":", 2);
            if (split.length != 2) {
                continue;
            }

            SkillTree tree = SkillTreeRegistry.get(split[0]);
            if (tree == null) {
                continue;
            }

            SkillNode node = tree.getNode(split[1]);
            if (node == null) {
                continue;
            }

            for (Map.Entry<String, Double> modifier : node.statModifiers().entrySet()) {
                Attribute attribute = resolveAttribute(modifier.getKey());
                if (attribute == null) {
                    continue;
                }

                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }

                UUID id = uuidForNodeModifier(unlocked, modifier.getKey());
                AttributeModifier existing = instance.getModifier(id);
                if (existing != null) {
                    instance.removeModifier(existing);
                }

                double amount = modifier.getValue();
                if (Math.abs(amount) > 0.00001D) {
                    instance.addTransientModifier(new AttributeModifier(id, "ec_skill_" + modifier.getKey(), amount, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    private static void clearSkillModifiers(ServerPlayer player) {
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            instance.getModifiers().stream()
                    .filter(modifier -> modifier.getName().startsWith("ec_skill_"))
                    .map(AttributeModifier::getId)
                    .toList()
                    .forEach(instance::removeModifier);
        }
    }

    private static UUID uuidForNodeModifier(String nodeKey, String statKey) {
        return UUID.nameUUIDFromBytes((nodeKey + "|" + statKey).getBytes(StandardCharsets.UTF_8));
    }

    private static Attribute resolveAttribute(String key) {
        return switch (key) {
            case "attack_damage", "offhand_damage" -> Attributes.ATTACK_DAMAGE;
            case "max_health" -> Attributes.MAX_HEALTH;
            case "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "luck" -> Attributes.LUCK;
            default -> null;
        };
    }
}
