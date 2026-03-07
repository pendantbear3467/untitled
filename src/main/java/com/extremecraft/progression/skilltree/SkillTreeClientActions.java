package com.extremecraft.progression.skilltree;

import com.extremecraft.network.ModNetwork;

import java.util.Locale;

/**
 * Shared client-side action layer used by skill-tree screens.
 */
public final class SkillTreeClientActions {
    private SkillTreeClientActions() {
    }

    public static boolean requestUnlock(String treeId, String nodeId) {
        String normalizedTreeId = normalize(treeId);
        String normalizedNodeId = normalize(nodeId);
        if (normalizedTreeId.isEmpty() || normalizedNodeId.isEmpty()) {
            return false;
        }

        ModNetwork.CHANNEL.sendToServer(new UnlockSkillNodeC2S(normalizedTreeId, normalizedNodeId));
        return true;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}

