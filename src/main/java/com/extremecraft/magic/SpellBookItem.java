package com.extremecraft.magic;

import com.extremecraft.magic.SpellCastContext.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class SpellBookItem extends Item {
    private static final ResourceLocation SPELL_BOOK_ID = ResourceLocation.fromNamespaceAndPath("extremecraft", "spell_book");

    public SpellBookItem(Properties properties) {
        super(properties);
    }

    public static boolean hasSpellBookAccess(Player player) {
        if (player == null) {
            return false;
        }

        if (isSpellBook(player.getMainHandItem()) || isSpellBook(player.getOffhandItem())) {
            return true;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (isSpellBook(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpellBook(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof SpellBookItem) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return SPELL_BOOK_ID.equals(id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        boolean cast = SpellExecutor.tryCastFromStack(serverPlayer, stack, CastSource.SPELL_BOOK);
        return cast ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }
}
