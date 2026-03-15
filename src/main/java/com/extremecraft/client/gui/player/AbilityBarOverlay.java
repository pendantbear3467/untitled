package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.debug.DeveloperOverlayState;
import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import com.extremecraft.client.gui.theme.ECGuiTheme;
import com.extremecraft.config.DwConfig;
import com.extremecraft.core.ECConstants;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.radiation.ChunkContaminationService;
import com.extremecraft.radiation.RadiationService;
import com.extremecraft.reactor.ReactorIdentity;
import com.extremecraft.network.sync.RuntimeSyncClientState;
import com.extremecraft.platform.data.registry.MachineDataRegistry;
import com.extremecraft.platform.data.registry.RecipeDataRegistry;
import com.extremecraft.platform.data.registry.SkillTreeDataRegistry;
import com.extremecraft.platform.data.sync.client.PlatformDataClientState;
import com.extremecraft.platform.data.validator.DataValidationService;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbilityBarOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SLOT_BG = ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/gui/magic_slot.png");
    private static final ResourceLocation FALLBACK_ICON = ResourceLocation.fromNamespaceAndPath(
            ECConstants.MODID,
            "textures/gui/abilities/ability_default.png"
    );

    private final Map<String, Integer> cooldownMaxByAbility = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> resolvedIcons = new LinkedHashMap<>();
    private final Set<String> loggedMissingAbilityIcons = new HashSet<>();
    private boolean loggedMissingFallback;

    private static final int SLOT_SIZE = 22;
    private static final int ICON_SIZE = 16;
    private static final int SLOT_COUNT = 4;
    private static final int DEV_PANEL_WIDTH = 196;
    private static final int HUD_PANEL_WIDTH = 116;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int anchorX = DwConfig.CLIENT.hudAnchorX.get();
        int anchorY = DwConfig.CLIENT.hudAnchorY.get();

        int totalWidth = (SLOT_COUNT * SLOT_SIZE) + ((SLOT_COUNT - 1) * 4);
        int startX = (screenWidth - totalWidth) / 2 + anchorX;
        int y = screenHeight - 42 + anchorY;

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            int x = startX + slotIndex * (SLOT_SIZE + 4);
            renderSlot(gui, player, slotIndex, x, y);
        }

        if (DwConfig.CLIENT.enableManaHudOverlay.get()) {
            renderManaHud(gui, player, startX - HUD_PANEL_WIDTH - 6, y);
        }

        if (DwConfig.CLIENT.enableRadiationHudOverlay.get()) {
            renderRadiationHud(gui, player, startX + totalWidth + 6, y);
        }

        if (DeveloperOverlayState.isEnabled()) {
            renderDeveloperOverlay(gui, screenWidth);
        }
    }

    private void renderManaHud(GuiGraphics gui, LocalPlayer player, int x, int y) {
        int width = HUD_PANEL_WIDTH;
        int currentMana = ManaApi.get(player).map(m -> (int) Math.floor(m.currentMana())).orElse(0);
        int maxMana = Math.max(1, ManaApi.get(player).map(m -> (int) Math.ceil(m.maxMana())).orElse(1));
        gui.fill(x, y, x + width, y + SLOT_SIZE, 0xB0121821);
        ECGuiPrimitives.drawSectionHeader(gui, Minecraft.getInstance().font, Component.literal("Mana"), x + 4, y + 3, 40, ECGuiTheme.ACCENT_VIOLET);
        ECGuiPrimitives.drawSegmentedBar(gui, x + 4, y + 14, width - 8, 4, currentMana / (float) maxMana, ECGuiTheme.ACCENT_VIOLET);
        ECGuiPrimitives.drawStatusChip(gui, Minecraft.getInstance().font, x + width - 50, y + 2, Component.literal(currentMana + "/" + maxMana), currentMana > 0 ? ECGuiTheme.TEXT_SECONDARY : ECGuiTheme.STATE_ERROR);
    }

    private void renderRadiationHud(GuiGraphics gui, LocalPlayer player, int x, int y) {
        int width = HUD_PANEL_WIDTH;
        double ambient = RadiationService.ambientExposure(player);
        double dose = RadiationService.accumulatedDose(player);
        double contamination = RadiationService.contaminationPressure(player);
        double threshold = Math.max(1.0D, ChunkContaminationService.profile().debuffThreshold());
        boolean warning = dose >= threshold || contamination >= threshold || ambient >= 2.0D;

        gui.fill(x, y, x + width, y + SLOT_SIZE + 18, warning ? 0xB03A120E : 0xB0111A12);
        ECGuiPrimitives.drawSectionHeader(gui, Minecraft.getInstance().font, Component.literal("Radiation"), x + 4, y + 3, 72, warning ? ECGuiTheme.STATE_WARN : ECGuiTheme.STATE_READY);
        gui.drawString(Minecraft.getInstance().font, Component.literal(String.format("Dose %.1f  Env %.1f", dose, ambient)), x + 4, y + 13, ECGuiTheme.TEXT_PRIMARY, false);

        int barRight = x + width - 4;
        int barLeft = x + 62;
        double contaminationRatio = Math.max(0.0D, Math.min(1.0D, contamination / threshold));
        int contaminationFill = Math.min(barRight - barLeft, (int) Math.round(contaminationRatio * (barRight - barLeft)));
        ECGuiPrimitives.drawSegmentedBar(gui, barLeft, y + 15, barRight - barLeft, 4, (float) contaminationRatio, warning ? ECGuiTheme.STATE_WARN : ECGuiTheme.ACCENT_CYAN_DIM);
        gui.drawString(Minecraft.getInstance().font, Component.literal(String.format("Contam %.1f", contamination)), x + 4, y + 14, ECGuiTheme.TEXT_SECONDARY, false);

        if (dose >= threshold || contamination >= threshold) {
            gui.drawString(Minecraft.getInstance().font, Component.literal("Warning: Decon advised"), x + 4, y + SLOT_SIZE + 7, ECGuiTheme.STATE_WARN, false);
        } else if (ambient >= 2.0D) {
            gui.drawString(Minecraft.getInstance().font, Component.literal("Exposure rising"), x + 4, y + SLOT_SIZE + 7, ECGuiTheme.STATE_WARN, false);
        } else {
            gui.drawString(Minecraft.getInstance().font, Component.literal("Status: Stable"), x + 4, y + SLOT_SIZE + 7, ECGuiTheme.STATE_READY, false);
        }

        CompoundTag states = RuntimeSyncClientState.machineStates();
        boolean reactorWarning = false;
        for (String key : states.getAllKeys()) {
            CompoundTag machine = states.getCompound(key);
            String machineId = machine.getString("machine");
            if (!ReactorIdentity.isFirstReleaseReactor(machineId)) {
                continue;
            }
            CompoundTag reactor = machine.getCompound("reactor");
            if (reactor.isEmpty()) {
                continue;
            }
            if (reactor.getBoolean("melted_down")
                    || reactor.getBoolean("scrammed")
                    || reactor.getDouble("heat") >= 800.0D
                    || reactor.getDouble("radiation") >= 5.0D) {
                reactorWarning = true;
                break;
            }
        }
        if (reactorWarning) {
            ECGuiPrimitives.drawStatusChip(gui, Minecraft.getInstance().font, x + width - 72, y + 2, Component.literal("Reactor Alert"), ECGuiTheme.STATE_WARN);
        }
    }

    private void renderSlot(GuiGraphics gui, LocalPlayer player, int slotIndex, int x, int y) {
        String abilityId = RuntimeSyncClientState.abilityInSlot(slotIndex);
        if (abilityId == null || abilityId.isBlank()) {
            abilityId = switch (slotIndex) {
                case 0 -> "firebolt";
                case 1 -> "blink";
                case 2 -> "arcane_shield";
                case 3 -> "meteor";
                default -> "";
            };
        }

        int manaCost = RuntimeSyncClientState.manaCostForSlot(slotIndex);
        int cooldownTicks = RuntimeSyncClientState.abilityCooldowns().getOrDefault(abilityId, 0);
        if (cooldownTicks > 0) {
            cooldownMaxByAbility.put(abilityId, Math.max(cooldownTicks, cooldownMaxByAbility.getOrDefault(abilityId, cooldownTicks)));
        } else {
            cooldownMaxByAbility.remove(abilityId);
        }

        gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xAA0F1218);
        ECGuiPrimitives.drawFramedSlot(gui, x + 3, y + 3, cooldownTicks == 0 && !abilityId.isBlank());
        gui.blit(SLOT_BG, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        if (!abilityId.isBlank()) {
            ResourceLocation icon = resolveAbilityIcon(Minecraft.getInstance(), abilityId);
            gui.blit(icon, x + 3, y + 3, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }

        if (cooldownTicks > 0) {
            int maxCooldown = Math.max(cooldownMaxByAbility.getOrDefault(abilityId, cooldownTicks), 1);
            float ratio = Math.max(0.0F, Math.min(1.0F, cooldownTicks / (float) maxCooldown));
            drawCooldownRadial(gui, x + 3, y + 3, ICON_SIZE, ratio);
            String label = String.valueOf((int) Math.ceil(cooldownTicks / 20.0D));
            gui.drawCenteredString(Minecraft.getInstance().font, label, x + (SLOT_SIZE / 2), y + 7, 0xFFFFFFFF);
        }

        int currentMana = ManaApi.get(player).map(m -> (int) Math.floor(m.currentMana())).orElse(0);
        int manaColor = currentMana >= manaCost ? ECGuiTheme.ACCENT_CYAN : ECGuiTheme.STATE_ERROR;
        gui.fill(x + 2, y + SLOT_SIZE - 4, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x6611141A);
        gui.drawString(Minecraft.getInstance().font, Component.literal(String.valueOf(manaCost)), x + 4, y + SLOT_SIZE - 11, manaColor, false);

        if (!abilityId.isBlank()) {
            gui.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(abilityId),
                    x - 2,
                    y - 9,
                ECGuiTheme.TEXT_MUTED,
                    false
            );
        }
    }

    private void drawCooldownRadial(GuiGraphics gui, int x, int y, int size, float ratio) {
        int radius = size / 2;
        int centerX = x + radius;
        int centerY = y + radius;
        double cutoffAngle = ratio * (Math.PI * 2.0D);

        for (int px = 0; px < size; px++) {
            for (int py = 0; py < size; py++) {
                int dx = px - radius;
                int dy = py - radius;
                if ((dx * dx) + (dy * dy) > (radius * radius)) {
                    continue;
                }

                double angle = Math.atan2(dy, dx) + Math.PI;
                if (angle <= cutoffAngle) {
                    gui.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, 0xAA000000);
                }
            }
        }
    }

    private ResourceLocation resolveAbilityIcon(Minecraft minecraft, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return FALLBACK_ICON;
        }

        return resolvedIcons.computeIfAbsent(abilityId, id -> {
            ResourceLocation specificIcon = ResourceLocation.fromNamespaceAndPath(
                    ECConstants.MODID,
                    "textures/gui/abilities/" + id + ".png"
            );
            if (hasResource(minecraft.getResourceManager(), specificIcon)) {
                return specificIcon;
            }

            ResourceLocation spellIcon = ResourceLocation.fromNamespaceAndPath(
                    ECConstants.MODID,
                    "textures/gui/spells/" + id + ".png"
            );
            if (hasResource(minecraft.getResourceManager(), spellIcon)) {
                return spellIcon;
            }

            if (loggedMissingAbilityIcons.add(id)) {
                LOGGER.warn("[ExtremeCraft Validation] Missing ability icon: extremecraft:{} (using fallback texture)", id);
            }

            if (hasResource(minecraft.getResourceManager(), FALLBACK_ICON)) {
                return FALLBACK_ICON;
            }

            if (!loggedMissingFallback) {
                LOGGER.error("[ExtremeCraft Validation] Missing fallback ability icon texture: {}", FALLBACK_ICON);
                loggedMissingFallback = true;
            }
            return SLOT_BG;
        });
    }

    private static boolean hasResource(ResourceManager resourceManager, ResourceLocation location) {
        return resourceManager.getResource(location).isPresent();
    }

    private void renderDeveloperOverlay(GuiGraphics gui, int screenWidth) {
        int x = screenWidth - DEV_PANEL_WIDTH - 8;
        int y = 8;
        int panelHeight = 82;

        gui.fill(x, y, x + DEV_PANEL_WIDTH, y + panelHeight, 0xAA090D12);
        ECGuiPrimitives.drawPanelChrome(gui, x, y, DEV_PANEL_WIDTH, panelHeight, 0);

        int lineY = y + 6;
        gui.drawString(Minecraft.getInstance().font, "ExtremeCraft Dev Overlay", x + 6, lineY, ECGuiTheme.TEXT_PRIMARY, false);

        lineY += 12;
        gui.drawString(Minecraft.getInstance().font, "Loaded machines: " + PlatformDataClientState.machines().size(), x + 6, lineY, ECGuiTheme.TEXT_SECONDARY, false);

        lineY += 10;
        gui.drawString(Minecraft.getInstance().font, "Loaded skill trees: " + PlatformDataClientState.skillTrees().size(), x + 6, lineY, ECGuiTheme.TEXT_SECONDARY, false);

        lineY += 10;
        String datapackCounts = "Datapacks M/S/R: "
                + MachineDataRegistry.registry().size()
                + "/"
                + SkillTreeDataRegistry.registry().size()
                + "/"
                + RecipeDataRegistry.registry().size();
        gui.drawString(Minecraft.getInstance().font, datapackCounts, x + 6, lineY, ECGuiTheme.TEXT_SECONDARY, false);

        lineY += 10;
        int runtimeMachines = RuntimeSyncClientState.machineStates().getAllKeys().size();
        gui.drawString(Minecraft.getInstance().font, "Runtime machines synced: " + runtimeMachines, x + 6, lineY, ECGuiTheme.TEXT_SECONDARY, false);

        lineY += 10;
        gui.drawString(
                Minecraft.getInstance().font,
                "Validation status: " + DataValidationService.lastValidationStatus(),
                x + 6,
                lineY,
                ECGuiTheme.ACCENT_AMBER,
                false
        );
    }
}

