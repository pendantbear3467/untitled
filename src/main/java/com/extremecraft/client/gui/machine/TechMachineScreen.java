package com.extremecraft.client.gui.machine;

import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import com.extremecraft.client.gui.theme.ECGuiTheme;
import com.extremecraft.machine.menu.TechMachineMenu;
import com.extremecraft.network.sync.RuntimeSyncClientState;
import com.extremecraft.reactor.ReactorIdentity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class TechMachineScreen extends AbstractContainerScreen<TechMachineMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private Button scramButton;
    private boolean scramRequested;

    public TechMachineScreen(TechMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        scramButton = addRenderableWidget(Button.builder(Component.literal("SCRAM"), b -> scramRequested = true)
                .bounds(leftPos + imageWidth + 8, topPos + 94, 56, 20)
                .build());
        scramButton.visible = isReactorMachine();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        ECGuiPrimitives.drawPanelChrome(graphics, x, y, imageWidth, imageHeight, menu.containerId);

        // Process zone
        graphics.fill(x + 34, y + 16, x + 142, y + 74, 0x55202935);
        drawRoleSlot(graphics, x + 44, y + 35, "IN", true);
        drawRoleSlot(graphics, x + 8, y + 53, "PWR", true);
        drawRoleSlot(graphics, x + 116, y + 35, "OUT", false);

        int progress = menu.progress();
        graphics.fill(x + 78, y + 36, x + 103, y + 50, 0xAA1A2330);
        if (progress > 0) {
            graphics.blit(TEXTURE, x + 79, y + 34, 176, 14, progress + 1, 16);
        }
        graphics.drawString(font, Component.literal("->"), x + 84, y + 39, ECGuiTheme.ACCENT_CYAN, false);

        int energy = menu.energy();
        graphics.fill(x + 7, y + 17, x + 14, y + 71, 0xAA121A25);
        if (energy > 0) {
            graphics.fill(x + 8, y + 70 - energy, x + 13, y + 70, ECGuiTheme.ACCENT_CYAN);
        }
        graphics.drawString(font, Component.literal("E"), x + 8, y + 8, ECGuiTheme.TEXT_MUTED, false);

        // Side diagnostics panel with explicit machine state readability.
        int panelX = x + imageWidth + 6;
        int panelY = y;
        int panelW = 126;
        int panelH = imageHeight;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xD0111620);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xAA1C2533);

        ECGuiPrimitives.drawSectionHeader(graphics, font, Component.literal("Machine"), panelX + 6, panelY + 6, 94, ECGuiTheme.ACCENT_CYAN);
        graphics.drawString(font, machineTitle(), panelX + 6, panelY + 17, ECGuiTheme.TEXT_SECONDARY, false);
        ECGuiPrimitives.drawStatusChip(graphics, font, panelX + 6, panelY + 28, Component.literal(machineStatusLine()), statusColor());

        int rawEnergy = menu.rawEnergy();
        int rawEnergyMax = menu.rawMaxEnergy();
        int rawProgress = menu.rawProgress();
        int rawProgressMax = menu.rawMaxProgress();

        graphics.drawString(font, Component.literal("Energy: " + rawEnergy + " / " + rawEnergyMax), panelX + 6, panelY + 44, ECGuiTheme.TEXT_SECONDARY, false);
        graphics.drawString(font, Component.literal("Progress: " + rawProgress + " / " + rawProgressMax), panelX + 6, panelY + 55, ECGuiTheme.TEXT_SECONDARY, false);

        if (isReactorMachine()) {
            CompoundTag reactor = reactorState();

            graphics.drawString(font, Component.literal("Reactor Panel"), panelX + 6, panelY + 71, ECGuiTheme.ACCENT_AMBER, false);
            if (reactor.isEmpty()) {
                graphics.drawString(font, Component.literal("Telemetry: awaiting sync"), panelX + 6, panelY + 82, ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal("State is server-owned"), panelX + 6, panelY + 93, ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal("Heat/rads are not estimated"), panelX + 6, panelY + 104, ECGuiTheme.STATE_WARN, false);
            } else {
                double heat = reactor.getDouble("heat");
                double steam = reactor.getDouble("steam");
                double waste = reactor.getDouble("waste");
                double reactivity = reactor.getDouble("reactivity");
                double radiation = reactor.getDouble("radiation");
                boolean scrammed = reactor.getBoolean("scrammed");
                boolean meltedDown = reactor.getBoolean("melted_down");

                graphics.drawString(font, Component.literal("State: " + reactorStateLabel(reactor)), panelX + 6, panelY + 82, reactorStateColor(reactor), false);
                graphics.drawString(font, Component.literal(String.format("Heat: %.1f", heat)), panelX + 6, panelY + 93, heat >= 800.0D ? ECGuiTheme.STATE_ERROR : ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal(String.format("Reactivity: %.1f", reactivity)), panelX + 6, panelY + 104, ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal(String.format("Steam: %.1f", steam)), panelX + 6, panelY + 115, ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal(String.format("Waste: %.2f", waste)), panelX + 6, panelY + 126, waste >= 5.0D ? ECGuiTheme.STATE_WARN : ECGuiTheme.TEXT_PRIMARY, false);
                graphics.drawString(font, Component.literal(String.format("Radiation: %.1f", radiation)), panelX + 6, panelY + 137, radiation >= 5.0D ? ECGuiTheme.STATE_WARN : ECGuiTheme.TEXT_PRIMARY, false);

                if (scramRequested) {
                    graphics.drawString(font, Component.literal("SCRAM requested (client)"), panelX + 6, panelY + 148, ECGuiTheme.STATE_WARN, false);
                } else if (meltedDown) {
                    graphics.drawString(font, Component.literal("Containment failure recorded"), panelX + 6, panelY + 148, ECGuiTheme.STATE_ERROR, false);
                } else if (scrammed) {
                    graphics.drawString(font, Component.literal("Control rods fully inserted"), panelX + 6, panelY + 148, ECGuiTheme.ACCENT_AMBER, false);
                }
            }
        } else {
            graphics.drawString(font, Component.literal("What powers it: fuel/energy"), panelX + 6, panelY + 74, ECGuiTheme.TEXT_PRIMARY, false);
            graphics.drawString(font, Component.literal("Output: item processing"), panelX + 6, panelY + 85, ECGuiTheme.TEXT_PRIMARY, false);
            graphics.drawString(font, Component.literal("If stalled: check IO/fuel"), panelX + 6, panelY + 96, ECGuiTheme.TEXT_PRIMARY, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, machineTitle(), 8, 6, 0x404040, false);

        if (isHovering(8, 70, 5, 52, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Energy buffer"), mouseX - leftPos, mouseY - topPos);
        }
        if (isHovering(79, 34, 24, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(machineStatusLine()), mouseX - leftPos, mouseY - topPos);
        }
    }

    private boolean isReactorMachine() {
        return ReactorIdentity.isFirstReleaseReactor(menu.machineId());
    }

    private String machineStatusLine() {
        ItemStack input = menu.getSlot(0).getItem();
        ItemStack fuel = menu.getSlot(1).getItem();
        ItemStack output = menu.getSlot(2).getItem();
        CompoundTag reactor = reactorState();

        if (isReactorMachine()) {
            if (reactor.isEmpty()) {
                return "Telemetry pending";
            }
            if (reactor.getBoolean("melted_down")) {
                return "Meltdown recorded";
            }
            if (reactor.getBoolean("scrammed")) {
                return "SCRAMMED";
            }
            if (reactor.getDouble("heat") >= 800.0D || reactor.getDouble("radiation") >= 5.0D) {
                return "Reactor warning";
            }
            if (reactor.getDouble("reactivity") > 0.0D || reactor.getInt("fuel_ticks_remaining") > 0) {
                return "Stable";
            }
        }
        if (menu.rawProgress() > 0) {
            return "Processing";
        }
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return "Output full";
        }
        if (menu.rawEnergy() <= 0) {
            if (menu.machineId().contains("mana") || menu.machineId().contains("arcane") || menu.machineId().contains("rune")) {
                return "Insufficient Aether";
            }
            if (fuel.isEmpty()) {
                return "Missing fuel";
            }
            return "Insufficient energy";
        }
        if (input.isEmpty()) {
            return "No valid recipe";
        }
        return "Structure incomplete";
    }

    private int statusColor() {
        String status = machineStatusLine();
        if ("Processing".equals(status) || "Stable".equals(status)) {
            return ECGuiTheme.STATE_READY;
        }
        if ("SCRAMMED".equals(status)) {
            return ECGuiTheme.STATE_WARN;
        }
        if (status.contains("Meltdown") || status.contains("warning") || status.contains("Missing") || status.contains("Insufficient") || status.contains("No valid")) {
            return ECGuiTheme.STATE_ERROR;
        }
        return ECGuiTheme.TEXT_SECONDARY;
    }

    private void drawRoleSlot(GuiGraphics graphics, int x, int y, String role, boolean powered) {
        ECGuiPrimitives.drawFramedSlot(graphics, x, y, powered);
        graphics.drawString(font, Component.literal(role), x - 1, y - 9, powered ? ECGuiTheme.TEXT_MUTED : ECGuiTheme.TEXT_SECONDARY, false);
    }

    private Component machineTitle() {
        return Component.translatable("block.extremecraft." + menu.machineId());
    }

    private CompoundTag machineState() {
        return RuntimeSyncClientState.machineStates().getCompound(Long.toString(menu.blockPos().asLong()));
    }

    private CompoundTag reactorState() {
        return machineState().getCompound("reactor");
    }

    private String reactorStateLabel(CompoundTag reactor) {
        if (reactor.getBoolean("melted_down")) {
            return "MELTDOWN";
        }
        if (reactor.getBoolean("scrammed")) {
            return "SCRAMMED";
        }
        if (reactor.getDouble("heat") >= 800.0D || reactor.getDouble("radiation") >= 5.0D) {
            return "Warning";
        }
        if (reactor.getDouble("reactivity") > 0.0D || reactor.getInt("fuel_ticks_remaining") > 0) {
            return "Stable";
        }
        return "Idle";
    }

    private int reactorStateColor(CompoundTag reactor) {
        String state = reactorStateLabel(reactor);
        if ("Stable".equals(state) || "Idle".equals(state)) {
            return 0xD6E7F7;
        }
        if ("SCRAMMED".equals(state)) {
            return 0xFFDCC7A1;
        }
        return 0xFFDF9A7A;
    }
}
