package com.extremecraft.game;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestRegistry {
    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    static {
        add(new QuestDefinition("initiation_of_war", "Initiation of War", QuestType.KILL_MOBS, 25, PlayerClass.FIGHTER.id, 1, 1));
        add(new QuestDefinition("stone_and_blood", "Stone and Blood", QuestType.BREAK_BLOCKS, 64, PlayerClass.MINER.id, 1, 1));
        add(new QuestDefinition("long_road", "The Long Road", QuestType.BREAK_BLOCKS, 180, PlayerClass.EXPLORER.id, 2, 1));
        add(new QuestDefinition("market_of_ashes", "Market of Ashes", QuestType.CRAFT_ITEMS, 20, PlayerClass.TRADER.id, 1, 2));
        add(new QuestDefinition("mercy_protocol", "Mercy Protocol", QuestType.CRAFT_ITEMS, 40, PlayerClass.MEDICAL.id, 2, 1));
        add(new QuestDefinition("surgical_dread", "Surgical Dread", QuestType.KILL_MOBS, 120, PlayerClass.DOCTOR.id, 2, 2));
        add(new QuestDefinition("forbidden_theory", "Forbidden Theory", QuestType.CRAFT_ITEMS, 90, PlayerClass.SCIENTIST.id, 3, 3));
    }

    private static void add(QuestDefinition q) {
        QUESTS.put(q.id(), q);
    }

    public static Map<String, QuestDefinition> all() {
        return QUESTS;
    }

    public static QuestDefinition get(String id) {
        return QUESTS.get(id);
    }

    private QuestRegistry() {}
}
