package com.extremecraft.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class GameplayAuthoritySourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void legacyPlayerStatsGameplayEventsNoLongerOwnXpWrites() throws IOException {
        String source = read("progression/src/main/java/com/extremecraft/progression/capability/PlayerStatsGameplayEvents.java");
        assertFalse(source.contains("grantPlayerXp("), "Legacy player-stats event surface should not grant player XP");
        assertFalse(source.contains("grantSkillXp("), "Legacy player-stats event surface should not grant skill XP");
        assertFalse(source.contains("grantCombatKillXp("), "Legacy player-stats event surface should not grant combat XP");
    }

    @Test
    void modularDrillRoutesThroughCanonicalModuleRuntime() throws IOException {
        String source = read("src/main/java/com/extremecraft/item/tool/ModularDrillItem.java");
        String playerStatsService = read("progression/src/main/java/com/extremecraft/progression/PlayerStatsService.java");
        assertTrue(source.contains("extends AbstractModularToolItem"), "Live modular drill should extend the canonical modular tool base");
        assertTrue(source.contains("ModuleStackData.readModules"), "Live modular drill tooltip should inspect canonical module stack data");
        assertFalse(source.contains("com.extremecraft.item.module"), "Live modular drill should not depend on the legacy item.module runtime");
        assertFalse(playerStatsService.contains("ModuleEffectService"), "PlayerStatsService should not rescan the legacy item.module runtime");
    }

    @Test
    void stageSyncHasDedicatedRegisteredMirror() throws IOException {
        String network = read("src/main/java/com/extremecraft/network/ModNetwork.java");
        String syncService = read("src/main/java/com/extremecraft/network/sync/RuntimeSyncService.java");
        String clientState = read("src/main/java/com/extremecraft/network/sync/RuntimeSyncClientState.java");

        assertTrue(network.contains("SyncStageStateS2CPacket.class"), "Stage sync packet must be registered in ModNetwork");
        assertTrue(syncService.contains("syncStageState(player, true);"), "RuntimeSyncService.syncAll should include stage sync");
        assertTrue(syncService.contains("new SyncStageStateS2CPacket(payload)"), "RuntimeSyncService should emit the dedicated stage sync packet");
        assertTrue(clientState.contains("applyStageState"), "Client runtime sync state should expose a stage mirror");
    }

    @Test
    void commandsRouteProgressionMutationsThroughFacade() throws IOException {
        String devCommands = read("src/main/java/com/extremecraft/command/ECDevCommands.java");
        String progressCommands = read("progression/src/main/java/com/extremecraft/progression/ProgressCommands.java");

        assertFalse(devCommands.contains("LevelService.grantXp("), "Dev command XP writes should avoid legacy LevelService mutation path");
        assertFalse(devCommands.contains("LevelService.setLevel("), "Dev command level writes should avoid legacy LevelService mutation path");
        assertTrue(devCommands.contains("ProgressionFacade.grantPlayerXp("), "Dev command XP writes should route through ProgressionFacade");
        assertTrue(devCommands.contains("ProgressionFacade.setPlayerLevel("), "Dev command level writes should route through ProgressionFacade");
        assertFalse(progressCommands.contains("ProgressionMutationService.setLevel("), "Progress command level set should route through ProgressionFacade");
        assertTrue(progressCommands.contains("ProgressionFacade.setPlayerLevel("), "Progress command level set should route through ProgressionFacade");
    }

    @Test
    void skillXpPolicyIsCombatOnlyWithDebugOverride() throws IOException {
        String skillService = read("progression/src/main/java/com/extremecraft/progression/SkillProgressionService.java");

        assertTrue(skillService.contains("case COMBAT, DEBUG_COMMAND -> true"), "Skill XP policy should allow combat and debug sources only");
        assertTrue(skillService.contains("case MINING, ENGINEERING, ARCANE, EXPLORATION -> false"), "Non-combat skill XP sources should be rejected");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
