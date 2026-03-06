package com.extremecraft.dev.validation;

import java.util.List;
import java.util.regex.Pattern;

public final class ValidatorRules {
    public static final Pattern ON_BLOCK_START_BREAK = Pattern.compile("\\bonBlockStartBreak\\s*\\(");
    public static final Pattern BLOCK_BREAK_EVENT = Pattern.compile("\\bBlockBreakEvent\\b");
    public static final Pattern LIVING_HURT_EVENT = Pattern.compile("\\bLivingHurtEvent\\b");
    public static final Pattern LIVING_ATTACK_EVENT = Pattern.compile("\\bLivingAttackEvent\\b");
    public static final Pattern DESTROY_BLOCK_CALL = Pattern.compile("\\b(?:gameMode\\.)?destroyBlock\\s*\\(");
    public static final Pattern BREAK_BLOCK_CALL = Pattern.compile("\\bbreakBlock\\s*\\(");
    public static final Pattern DAMAGE_CALL = Pattern.compile("\\.hurt\\s*\\(");
    public static final Pattern ATTACK_CALL = Pattern.compile("\\.attack\\s*\\(");

    public static final Pattern TOOL_GUARD = Pattern.compile("ThreadLocal<|Set<BlockPos>|processed|processing|guard|HAMMER_ACTIVE");
    public static final Pattern SERVER_GUARD = Pattern.compile("!\\s*\\w+\\.isClientSide|instanceof\\s+ServerPlayer|instanceof\\s+ServerLevel|LogicalSide|isClientSide\\s*\\)|ServerPlayer|ServerLevel");
    public static final Pattern GAMEPLAY_MUTATION = Pattern.compile("\\bdestroyBlock\\s*\\(|\\bsetBlock\\s*\\(|\\baddFreshEntity\\s*\\(|\\.hurt\\s*\\(|\\.teleportTo\\s*\\(|\\bdiscard\\s*\\(");

    public static final Pattern NETWORK_HANDLER = Pattern.compile("\\bhandle\\s*\\(|\\bonMessage\\s*\\(|\\breceive\\s*\\(");
    public static final Pattern NETWORK_SEND = Pattern.compile("\\bsendToServer\\s*\\(|\\bPacketDistributor\\b|\\.send\\s*\\(");
    public static final Pattern NETWORK_GUARD = Pattern.compile("getReceptionSide|enqueueWork|isClientSide|PacketFlow|context\\.");

    public static final Pattern STATIC_COLLECTION = Pattern.compile("static\\s+(?:final\\s+)?(?:List|Set|Map|Queue|Deque|Collection)<([^>]+)>\\s+(\\w+)\\s*(?:=|;)");
    public static final Pattern CLEANUP_HINT = Pattern.compile("\\.clear\\s*\\(|\\.remove\\s*\\(|cleanup\\s*\\(|invalidate\\s*\\(|onPlayerLoggedOut|onLogout|reset\\s*\\(");

    public static final Pattern REGISTRY_REGISTER = Pattern.compile("register\\(\"([a-z0-9_./-]+)\"");
    public static final Pattern NAMESPACE_ID = Pattern.compile("extremecraft:([a-z0-9_./-]+)");
    public static final Pattern ITEM_OR_RESULT_ID = Pattern.compile("\"(?:item|result)\"\\s*:\\s*\"(extremecraft:[a-z0-9_./-]+)\"");
    public static final Pattern TAG_VALUE_ID = Pattern.compile("\"(#?extremecraft:[a-z0-9_./-]+)\"");
    public static final Pattern SUSPICIOUS_DUPLICATE_ORE = Pattern.compile("extremecraft:[a-z0-9_./-]*_ore_ore[a-z0-9_./-]*");

    public static final Pattern MODEL_TEXTURE_REF = Pattern.compile("\"(?:layer\\d+|particle|all|top|bottom|side|front|back|north|south|east|west|up|down)\"\\s*:\\s*\"extremecraft:([a-z0-9_./-]+)\"");
    public static final Pattern MODEL_REF = Pattern.compile("\"model\"\\s*:\\s*\"extremecraft:([a-z0-9_./-]+)\"");
    public static final Pattern LANG_KEY = Pattern.compile("\"([a-z0-9_.-]+)\"\\s*:");

    public static final List<Severity> ORDERED_SEVERITIES = List.of(
            Severity.CRITICAL,
            Severity.HIGH,
            Severity.MEDIUM,
            Severity.LOW
    );

    private ValidatorRules() {
    }

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
