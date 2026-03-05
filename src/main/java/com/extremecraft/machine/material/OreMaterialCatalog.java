package com.extremecraft.machine.material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OreMaterialCatalog {
    public static final Map<String, OreMaterialDefinition> MATERIALS = new LinkedHashMap<>();

    static {
        register(new OreMaterialDefinition("copper", MaterialTier.EARLY, true, true, 1, 0, 96, 18, 9, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("tin", MaterialTier.EARLY, true, false, 1, -16, 84, 16, 8, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("lead", MaterialTier.EARLY, true, false, 2, -40, 56, 12, 7, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("silver", MaterialTier.EARLY, true, false, 2, -24, 64, 10, 6, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("nickel", MaterialTier.EARLY, true, false, 2, -32, 48, 10, 6, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("aluminum", MaterialTier.EARLY, true, false, 1, 16, 120, 14, 8, Set.of("minecraft:overworld")));

        register(new OreMaterialDefinition("titanium", MaterialTier.MID, true, true, 3, -52, 24, 8, 6, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("platinum", MaterialTier.MID, true, false, 3, -56, 16, 6, 5, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("cobalt", MaterialTier.MID, true, false, 3, 8, 96, 7, 5, Set.of("minecraft:the_nether")));
        register(new OreMaterialDefinition("ardite", MaterialTier.MID, true, false, 3, 16, 112, 7, 5, Set.of("minecraft:the_nether")));

        register(new OreMaterialDefinition("uranium", MaterialTier.LATE, true, false, 4, -64, 8, 5, 4, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("iridium", MaterialTier.LATE, true, false, 4, -64, -8, 4, 4, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("osmium", MaterialTier.LATE, true, false, 4, -56, 8, 5, 4, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("mythril", MaterialTier.LATE, true, true, 4, -62, -20, 4, 4, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("void_crystal", MaterialTier.LATE, true, false, 4, 0, 64, 5, 4, Set.of("minecraft:the_end")));

        register(new OreMaterialDefinition("draconium", MaterialTier.ENDGAME, true, true, 5, -32, 24, 2, 3, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("aetherium", MaterialTier.ENDGAME, true, true, 5, 32, 180, 2, 3, Set.of("minecraft:overworld")));
        register(new OreMaterialDefinition("singularity_ore", MaterialTier.ENDGAME, false, false, 5, -64, -48, 1, 2, Set.of("minecraft:the_end")));
    }

    private static void register(OreMaterialDefinition definition) {
        MATERIALS.put(definition.id(), definition);
    }

    public static Optional<OreMaterialDefinition> byId(String id) {
        return Optional.ofNullable(MATERIALS.get(id));
    }

    private OreMaterialCatalog() {
    }
}
