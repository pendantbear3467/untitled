package com.extremecraft.progression.unlock;

import net.minecraft.world.entity.player.Player;

/**
 * Canonical access helper for player-facing unlockable content.
 *
 * <p>Callers can ask for base content access or action-specific access without inventing
 * one-off id conventions. The resolver checks an action-specific key first and then falls
 * back to the base content key.</p>
 */
public final class UnlockAccessService {
    public enum Action {
        VIEW("view"),
        CRAFT("craft"),
        EQUIP("equip"),
        USE("use"),
        CAST("cast"),
        ACTIVATE("activate");

        private final String id;

        Action(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private UnlockAccessService() {
    }

    public static boolean canAccess(Player player, String contentKey) {
        return UnlockRuleLoader.canUnlock(player, normalizeContentKey(contentKey));
    }

    public static boolean canAccess(Player player, String contentKey, Action action) {
        String normalized = normalizeContentKey(contentKey);
        if (normalized.isBlank()) {
            return true;
        }

        if (action == null) {
            return UnlockRuleLoader.canUnlock(player, normalized);
        }

        if (!UnlockRuleLoader.canUnlock(player, normalized + "#" + action.id())) {
            return false;
        }
        return UnlockRuleLoader.canUnlock(player, normalized);
    }

    public static boolean canViewItem(Player player, String itemId) {
        return canAccess(player, key("item", itemId), Action.VIEW);
    }

    public static boolean canEquipItem(Player player, String itemId) {
        return canAccess(player, key("item", itemId), Action.EQUIP);
    }

    public static boolean canUseItem(Player player, String itemId) {
        return canAccess(player, key("item", itemId), Action.USE);
    }

    public static boolean canCastSpell(Player player, String spellId) {
        return canAccess(player, key("spell", spellId), Action.CAST);
    }

    public static boolean canUseAbility(Player player, String abilityId) {
        return canAccess(player, key("ability", abilityId), Action.ACTIVATE);
    }

    public static boolean canUseClassAbility(Player player, String abilityId) {
        return canAccess(player, key("class_ability", abilityId), Action.ACTIVATE);
    }

    public static boolean canUseLoadoutSlot(Player player, String slotId) {
        return canAccess(player, key("loadout_slot", slotId), Action.EQUIP);
    }

    private static String key(String domain, String contentId) {
        String normalizedDomain = domain == null ? "" : domain.trim().toLowerCase();
        String normalizedContent = normalizeContentKey(contentId);
        if (normalizedDomain.isBlank()) {
            return normalizedContent;
        }
        return normalizedDomain + ":" + normalizedContent;
    }

    private static String normalizeContentKey(String contentKey) {
        if (contentKey == null) {
            return "";
        }
        return contentKey.trim().toLowerCase();
    }
}