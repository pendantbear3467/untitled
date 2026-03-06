package com.extremecraft.item.tool;

import com.extremecraft.config.Config;
import com.extremecraft.item.module.ItemModuleStorage;
import com.extremecraft.item.module.ModuleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ModularDrillItem extends PickaxeItem {
    public ModularDrillItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        float miningBonus = ModuleRegistry.sumToolEffect(stack, "mining_speed");
        if (miningBonus <= 0.0F) {
            return base;
        }

        return base * (1.0F + miningBonus);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        if (!Config.COMMON.tools.enableDrillTeleport.get()) {
            return super.use(level, player, hand);
        }

        int teleportLevel = ItemModuleStorage.levelOf(stack, "teleport_module");
        if (teleportLevel <= 0) {
            return super.use(level, player, hand);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        double baseDistance = Math.max(1.0D, Config.COMMON.tools.drillTeleportBaseDistance.get());
        double perLevel = Math.max(0.0D, Config.COMMON.tools.drillTeleportDistancePerLevel.get());
        double distance = baseDistance + (perLevel * teleportLevel);

        Vec3 look = serverPlayer.getLookAngle();
        Vec3 target = serverPlayer.position().add(look.scale(distance));

        if (!level.noCollision(serverPlayer, serverPlayer.getBoundingBox().move(target.x - serverPlayer.getX(), 0, target.z - serverPlayer.getZ()))) {
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.teleportTo(target.x, serverPlayer.getY(), target.z);

        int baseCooldown = Math.max(0, Config.COMMON.tools.drillTeleportCooldownBaseTicks.get());
        int reduction = Math.max(0, Config.COMMON.tools.drillTeleportCooldownReductionPerLevel.get()) * teleportLevel;
        int minimum = Math.max(0, Config.COMMON.tools.drillTeleportMinCooldownTicks.get());
        int cooldown = Math.max(minimum, baseCooldown - reduction);
        if (cooldown > 0) {
            serverPlayer.getCooldowns().addCooldown(this, cooldown);
        }

        stack.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(hand));
        level.playSound(null, target.x, target.y, target.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        List<ItemModuleStorage.InstalledModule> modules = ItemModuleStorage.getModules(stack);
        if (modules.isEmpty()) {
            tooltip.add(Component.literal("No modules installed").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.literal("Installed Modules:").withStyle(ChatFormatting.AQUA));
        for (ItemModuleStorage.InstalledModule module : modules) {
            tooltip.add(Component.literal("- " + module.id() + " Lv." + module.level()).withStyle(ChatFormatting.GRAY));
        }
    }
}
