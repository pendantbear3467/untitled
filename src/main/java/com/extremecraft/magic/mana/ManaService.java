package com.extremecraft.magic.mana;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.sync.SyncManaStateS2CPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class ManaService {
    private ManaService() {
    }

    public static boolean tryConsume(ServerPlayer player, double amount) {
        return ManaApi.get(player).map(mana -> {
            boolean consumed = mana.consume(amount);
            if (consumed) {
                sync(player, mana);
            }
            return consumed;
        }).orElse(false);
    }

    public static void setMana(ServerPlayer player, double value) {
        ManaApi.get(player).ifPresent(mana -> {
            mana.setCurrentMana(value);
            sync(player, mana);
        });
    }

    public static void refreshFromPlayerData(ServerPlayer player) {
        ManaApi.get(player).ifPresent(mana -> {
            double nextMaxMana = mana.maxMana();
            double nextRegen = mana.manaRegen();
            double nextSpellPower = mana.spellPower();

            PlayerStatsCapability stats = PlayerStatsApi.get(player).orElse(null);
            if (stats != null) {
                nextMaxMana = Math.max(nextMaxMana, stats.maxMana());
                nextRegen = Math.max(0.03D, (stats.intelligence() * 0.03D) + stats.spellPowerBonus());
                nextSpellPower = Math.max(1.0D, stats.magicPower() * (1.0D + stats.spellPowerBonus()));
            }

            mana.applyDerivedStats(nextMaxMana, nextRegen, nextSpellPower);
        });
    }

    public static void tick(ServerPlayer player) {
        if ((player.tickCount % 20) == 0) {
            refreshFromPlayerData(player);
        }

        ManaApi.get(player).ifPresent(mana -> {
            boolean changed = mana.regenerateTick();
            if (changed && (player.tickCount % 5) == 0) {
                sync(player, mana);
            }
        });
    }

    public static void sync(ServerPlayer player) {
        ManaApi.get(player).ifPresent(mana -> sync(player, mana));
    }

    public static void sync(ServerPlayer player, ManaCapability mana) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaStateS2CPacket(mana.serializeNBT()));
    }
}
