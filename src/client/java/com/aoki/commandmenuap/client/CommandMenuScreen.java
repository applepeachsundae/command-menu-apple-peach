package com.aoki.commandmenuap.client;

import com.aoki.commandmenuap.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandMenuScreen extends Screen {
    private final String contextKey;
    private final ModConfig config;
    private int selectedGroup = 0;
    private boolean editMode = false;
    private double commandScroll = 0;
    private double groupScroll = 0;
    private double commandMaxScroll = 0;
    private int commandViewportTop = 0;
    private int commandViewportBottom = 0;
    private EditBox commandSearch;
    private static final Map<String, Integer> LAST_SELECTED_GROUP = new HashMap<>();

    private final List<ButtonBounds> clickableButtons = new ArrayList<>();

    public CommandMenuScreen() {
        super(Component.literal("Command Menu"));
        this.contextKey = resolveContextKey();
        this.config = ModConfig.get(this.contextKey);
        int savedGroup = LAST_SELECTED_GROUP.getOrDefault(this.contextKey, this.config.selectedGroup);
        this.selectedGroup = Math.max(0, Math.min(savedGroup, this.config.groups.size() - 1));
    }

    @Override
    protected void init() {
        int panelH = Math.min(260, Math.max(0, this.height - 32));
        int panelY = (this.height - panelH) / 2;
        int searchWidth = Math.min(200, Math.max(120, this.width - 180));
        this.commandSearch = new EditBox(this.font, this.width / 2 - searchWidth / 2, panelY + 38, searchWidth, 18, Component.literal("Search commands"));
        this.commandSearch.setMaxLength(128);
        this.commandSearch.setResponder(value -> this.commandScroll = 0);
        this.addRenderableWidget(this.commandSearch);
    }

    private static String resolveContextKey() {
        Minecraft client = Minecraft.getInstance();
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            String worldPath = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString();
            return "world_" + worldPath;
        } else if (client.getCurrentServer() != null) {
            ServerData server = client.getCurrentServer();
            return "server_" + server.ip;
        }
        return "general_menu";
    }

    @Override
    public void tick() {
        super.tick();
        CustomIconManager.tick();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (setMovementKey(event.key(), true)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (setMovementKey(event.key(), false)) return true;
        return super.keyReleased(event);
    }

    private boolean setMovementKey(int keyCode, boolean pressed) {
        if (this.minecraft == null) return false;
        if (this.commandSearch != null && this.commandSearch.isFocused()) return false;
        if (keyCode == GLFW.GLFW_KEY_W) this.minecraft.options.keyUp.setDown(pressed);
        else if (keyCode == GLFW.GLFW_KEY_S) this.minecraft.options.keyDown.setDown(pressed);
        else if (keyCode == GLFW.GLFW_KEY_A) this.minecraft.options.keyLeft.setDown(pressed);
        else if (keyCode == GLFW.GLFW_KEY_D) this.minecraft.options.keyRight.setDown(pressed);
        else if (keyCode == GLFW.GLFW_KEY_SPACE) this.minecraft.options.keyJump.setDown(pressed);
        else return false;
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        clickableButtons.clear();
        // Latar belakang gelap lembut minimalis
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xB00F0F13, 0xD018181C);

        int panelW = Math.min(380, Math.max(0, this.width - 32));
        int panelH = Math.min(260, Math.max(0, this.height - 32));
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // Panel Utama bergaya modern flat
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE141418);
        renderOutlineBox(guiGraphics, panelX, panelY, panelW, panelH, 0xFF2A2A32);
        // Tombol Atas (Close, Clear Group, Clear All, Edit Mode)
        renderCustomButton(guiGraphics, "Close", panelX + 12, panelY + 12, 50, 18, 0xFF2D2D35, 0xFF3F3F4A, mouseX, mouseY, () -> this.onClose());

        if (this.editMode) {
            renderCustomButton(guiGraphics, "Clear Group", centerXPos(panelX, panelW, 164) + 2, panelY + 12, 78, 18, 0xFF451A03, 0xFF78350F, mouseX, mouseY, () -> {
                if (!config.groups.isEmpty() && selectedGroup < config.groups.size()) {
                    config.groups.get(selectedGroup).commands.clear();
                    config.save(contextKey);
                }
            });
            renderCustomButton(guiGraphics, "Clear All", centerXPos(panelX, panelW, 164) + 84, panelY + 12, 78, 18, 0xFF450A0A, 0xFF7F1D1D, mouseX, mouseY, () -> {
                config.groups.clear();
                selectedGroup = 0;
                config.selectedGroup = 0;
                config.save(contextKey);
            });
        }

        String editLabel = this.editMode ? "Done" : "Edit Mode";
        int editColor = this.editMode ? 0xFF14532D : 0xFF27272A;
        int editHoverColor = this.editMode ? 0xFF166534 : 0xFF3F3F46;
        renderCustomButton(guiGraphics, editLabel, panelX + panelW - 72, panelY + 12, 60, 18, editColor, editHoverColor, mouseX, mouseY, () -> {
            this.editMode = !this.editMode;
        });

        // Jika Group Kosong
        if (config.groups.isEmpty()) {
            guiGraphics.centeredText(this.font, "No Command Groups Available", panelX + (panelW / 2), panelY + 104, 0xFF71717A);
            renderCustomButton(guiGraphics, "+ Add New Group", panelX + (panelW / 2) - 60, panelY + 122, 120, 20, 0xFF1D4ED8, 0xFF2563EB, mouseX, mouseY, this::addGroup);
            super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            return;
        }

        if (selectedGroup >= config.groups.size()) {
            selectedGroup = Math.max(0, config.groups.size() - 1);
        }

        // Render Tab Kategori Modern
        int topY = panelY + 66;
        int tabW = 82;
        int tabH = 20;
        int tabGap = 6;
        int tabStartX = panelX + 16;
        int groupViewportRight = panelX + panelW - 16;
        int groupContentWidth = config.groups.size() * (tabW + tabGap) - tabGap;
        int maxGroupScroll = Math.max(0, groupContentWidth - (groupViewportRight - tabStartX));
        groupScroll = Math.max(0, Math.min(groupScroll, maxGroupScroll));
        guiGraphics.enableScissor(tabStartX, topY, groupViewportRight, topY + tabH);

        List<Integer> groupOrder = new ArrayList<>();
        for (int i = 0; i < config.groups.size(); i++) groupOrder.add(i);
        groupOrder.sort((a, b) -> {
            ModConfig.Group first = config.groups.get(a);
            ModConfig.Group second = config.groups.get(b);
            if (first.pinned != second.pinned) return Boolean.compare(!first.pinned, !second.pinned);
            if (first.pinned) return Long.compare(second.pinnedAt, first.pinnedAt);
            return Integer.compare(a, b);
        });
        for (int position = 0; position < groupOrder.size(); position++) {
            final int idx = groupOrder.get(position);
            ModConfig.Group g = config.groups.get(idx);
            boolean active = (idx == selectedGroup);

            int tabX = (int) (tabStartX + (position * (tabW + tabGap)) - groupScroll);
            int tabY = topY;

            int bgCol = active ? 0xFF2A2A32 : 0xFF1C1C21;
            guiGraphics.fill(tabX, tabY, tabX + tabW, tabY + tabH, bgCol);
            
            int outlineCol = active ? 0xFF3B82F6 : 0xFF2A2A32;
            renderOutlineBox(guiGraphics, tabX, tabY, tabW, tabH, outlineCol);

            renderIconPreview(guiGraphics, g.iconType, g.iconId, tabX + 4, tabY + 2);

            String title = trimText(g.name, tabW - (g.pinned ? 44 : 32));
            Component titleComp = ColorHelper.formatGroupTitle(title, g.colorMode, g.color1, g.color2);
            guiGraphics.enableScissor(tabX + 24, tabY, tabX + tabW - 2, tabY + tabH);
            guiGraphics.text(this.font, titleComp, tabX + 26, tabY + 6, 0xFFFFFFFF);
            if (g.pinned) {
                guiGraphics.centeredText(this.font, "★", tabX + tabW - 8, tabY + 6, 0xFFFFD166);
            }
            guiGraphics.disableScissor();

            if (tabX + tabW >= tabStartX && tabX <= groupViewportRight) {
                clickableButtons.add(new ButtonBounds(tabX, tabY, tabW, tabH, () -> selectGroup(idx)));
            }
        }
        guiGraphics.disableScissor();

        int groupBottomY = topY + tabH + 8;
        if (maxGroupScroll > 0) {
            int trackWidth = groupViewportRight - tabStartX;
            int thumbWidth = Math.max(28, trackWidth * trackWidth / Math.max(1, groupContentWidth));
            int thumbX = tabStartX + (int) ((trackWidth - thumbWidth) * groupScroll / maxGroupScroll);
            int scrollbarY = topY + tabH + 5;
            guiGraphics.fill(tabStartX, scrollbarY, groupViewportRight, scrollbarY + 3, 0xFF252832);
            guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + 3, 0xFF6B7280);
        }

        if (this.editMode) {
            int editBarY = groupBottomY + 4;
            int btnWidth = 64;
            int totalEditW = (btnWidth * 5) + 16;
            int editStartX = panelX + (panelW / 2) - (totalEditW / 2);

            renderCustomButton(guiGraphics, "+ Group", editStartX, editBarY, btnWidth, 18, 0xFF14532D, 0xFF166534, mouseX, mouseY, this::addGroup);
            renderCustomButton(guiGraphics, "+ Command", editStartX + btnWidth + 4, editBarY, btnWidth, 18, 0xFF14532D, 0xFF166534, mouseX, mouseY, this::addGroupCommand);

            renderCustomButton(guiGraphics, config.groups.get(selectedGroup).pinned ? "★" : "☆", editStartX + (btnWidth + 4) * 2, editBarY, btnWidth, 18, 0xFF78350F, 0xFFB45309, mouseX, mouseY, () -> {
                ModConfig.Group group = config.groups.get(selectedGroup);
                group.pinned = !group.pinned;
                group.pinnedAt = group.pinned ? System.currentTimeMillis() : 0L;
                config.save(contextKey);
            });

            renderCustomButton(guiGraphics, "✎ Edit", editStartX + (btnWidth + 4) * 3, editBarY, btnWidth, 18, 0xFF78350F, 0xFFB45309, mouseX, mouseY, () -> {
                if (this.minecraft != null && !config.groups.isEmpty()) {
                    ModConfig.Group currentG = config.groups.get(selectedGroup);
                    this.minecraft.setScreen(new EditGroupDialog(this, currentG, false, () -> {
                        this.config.save(this.contextKey);
                    }));
                }
            });

            renderCustomButton(guiGraphics, "✕ Delete", editStartX + (btnWidth + 4) * 4, editBarY, btnWidth, 18, 0xFF7F1D1D, 0xFF991B1B, mouseX, mouseY, () -> {
                if (!config.groups.isEmpty()) {
                    config.groups.remove(selectedGroup);
                    selectedGroup = Math.max(0, config.groups.size() - 1);
                    config.selectedGroup = selectedGroup;
                    config.save(contextKey);
                }
            });

            groupBottomY = editBarY + 22;
        }

        // Render Grid Tombol Perintah
        ModConfig.Group activeG = config.groups.get(selectedGroup);
        String commandQuery = commandSearch == null ? "" : commandSearch.getValue().toLowerCase().trim();
        List<Integer> visibleCommands = new ArrayList<>();
        for (int i = 0; i < activeG.commands.size(); i++) {
            ModConfig.Entry entry = activeG.commands.get(i);
            if (commandQuery.isEmpty() || safeLower(entry.label).contains(commandQuery) || safeLower(entry.command).contains(commandQuery)) {
                visibleCommands.add(i);
            }
        }
        visibleCommands.sort((a, b) -> Boolean.compare(!activeG.commands.get(a).pinned, !activeG.commands.get(b).pinned));
        int gridStartY = groupBottomY + 6;
        int btnW = Math.max(1, (panelW - 48) / 2);
        int btnH = 22;
        int gapX = 8;
        int gapY = 6;
        int globalCount = filteredGlobalCommandCount(commandQuery);
        if (globalCount > 0) {
            renderGlobalCommands(guiGraphics, panelX, gridStartY, panelW, panelH, mouseX, mouseY);
            gridStartY += globalCount * 28;
        }

        int viewportTop = gridStartY;
        int viewportBottom = panelY + panelH - 12;
        List<Integer> commandOrder = new ArrayList<>();
        commandOrder.addAll(visibleCommands);
        int displayedCount = visibleCommands.size() + (this.editMode ? 1 : 0);
        int contentHeight = Math.max(btnH, ((displayedCount + 1) / 2) * (btnH + gapY));
        double maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        this.commandMaxScroll = maxScroll;
        this.commandViewportTop = viewportTop;
        this.commandViewportBottom = viewportBottom;
        commandScroll = Math.max(0, Math.min(commandScroll, maxScroll));
        guiGraphics.enableScissor(panelX + 1, viewportTop, panelX + panelW - 1, viewportBottom);

        int commandGridWidth = Math.min(panelW - 32, 2 * btnW + gapX);
        int commandGridX = panelX + (panelW - commandGridWidth) / 2;
        if (commandOrder.size() + (this.editMode ? 1 : 0) <= 1) commandGridX = panelX + (panelW - btnW) / 2;
        for (int position = 0; position < commandOrder.size(); position++) {
            final int cmdIndex = commandOrder.get(position);
            ModConfig.Entry cmd = activeG.commands.get(cmdIndex);
            int col = position % 2;
            int row = position / 2;

            int btnX = commandGridX + (col * (btnW + gapX));
            int btnY = (int) (gridStartY + (row * (btnH + gapY)) - commandScroll);

            boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, isHovered ? 0xFF2A2A32 : 0xFF1C1C21);
            renderOutlineBox(guiGraphics, btnX, btnY, btnW, btnH, isHovered ? 0xFF52525B : 0xFF2A2A32);

            renderIconPreview(guiGraphics, cmd.iconType, cmd.iconId, btnX + 4, btnY + 3);
            guiGraphics.enableScissor(btnX + 24, btnY, btnX + btnW - (this.editMode ? 58 : 4), btnY + btnH);
            int labelWidth = this.editMode ? btnW - 78 : (cmd.pinned ? btnW - 48 : btnW - 30);
            guiGraphics.text(this.font, trimText(cmd.label, labelWidth), btnX + 24, btnY + 7, 0xFFE4E4E7);
            if (cmd.pinned) {
                guiGraphics.centeredText(this.font, "★", btnX + btnW - (this.editMode ? 62 : 9), btnY + 7, 0xFFFFD166);
            }
            guiGraphics.disableScissor();

            if (!this.editMode) {
                if (btnY + btnH >= viewportTop && btnY <= viewportBottom) clickableButtons.add(new ButtonBounds(btnX, btnY, btnW, btnH, () -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.connection.sendCommand(cmd.command.replaceFirst("^/", ""));
                        this.config.selectedGroup = this.selectedGroup;
                        LAST_SELECTED_GROUP.put(this.contextKey, this.selectedGroup);
                        this.config.save(this.contextKey);
                        if (cmd.closeOnClick) this.onClose();
                    }
                }));
            } else {
                renderCustomButton(guiGraphics, "★", btnX + btnW - 54, btnY + 3, 16, 16, cmd.pinned ? 0xFFB45309 : 0xFF3F3F46, 0xFFD97706, mouseX, mouseY, () -> {
                    cmd.pinned = !cmd.pinned;
                    this.config.save(this.contextKey);
                });
                renderCustomButton(guiGraphics, "✎", btnX + btnW - 36, btnY + 3, 16, 16, 0xFF78350F, 0xFFB45309, mouseX, mouseY, () -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new EditCommandDialog(this, cmd, false, () -> {
                            this.config.save(this.contextKey);
                        }));
                    }
                });
                renderCustomButton(guiGraphics, "✕", btnX + btnW - 18, btnY + 3, 16, 16, 0xFF7F1D1D, 0xFF991B1B, mouseX, mouseY, () -> {
                    activeG.commands.remove(cmdIndex);
                    this.config.save(this.contextKey);
                });
            }
        }

        guiGraphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = panelX + panelW - 10;
            int trackH = viewportBottom - viewportTop;
            int thumbH = Math.max(24, (int) (trackH * trackH / (double) contentHeight));
            int thumbY = viewportTop + (int) ((trackH - thumbH) * commandScroll / maxScroll);
            guiGraphics.fill(trackX, viewportTop, trackX + 4, viewportBottom, 0xFF252832);
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF6B7280);
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelW = Math.min(380, Math.max(0, this.width - 32));
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - Math.min(260, Math.max(0, this.height - 32))) / 2;
        int groupY = panelY + 66;
        if (mouseX >= panelX + 16 && mouseX <= panelX + panelW - 16 && mouseY >= groupY && mouseY <= groupY + 26) {
            groupScroll -= (verticalAmount != 0 ? verticalAmount : horizontalAmount) * 36.0;
            return true;
        }
        if (mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= this.commandViewportTop && mouseY <= this.commandViewportBottom
                && this.commandMaxScroll > 0) {
            double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
            commandScroll = Math.max(0, Math.min(this.commandMaxScroll, commandScroll - amount * 28.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderGlobalCommands(GuiGraphicsExtractor graphics, int panelX, int panelY, int panelW, int panelH, int mouseX, int mouseY) {
        String query = commandSearch == null ? "" : commandSearch.getValue().toLowerCase().trim();
        int y = panelY;
        int width = Math.max(1, (panelW - 40) / 2);
        for (ModConfig.Entry entry : config.globalCommands) {
            if (!query.isEmpty() && !safeLower(entry.label).contains(query) && !safeLower(entry.command).contains(query)) continue;
            int x = panelX + 16;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 22;
            graphics.fill(x, y, x + width, y + 22, hovered ? 0xFF2A2A32 : 0xFF1C1C21);
            renderOutlineBox(graphics, x, y, width, 22, hovered ? 0xFF52525B : 0xFF2A2A32);
            graphics.text(this.font, trimText(entry.label, width - 12), x + 8, y + 7, 0xFFE4E4E7);
            clickableButtons.add(new ButtonBounds(x, y, width, 22, () -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(entry.command.replaceFirst("^/", ""));
                    if (entry.closeOnClick) this.onClose();
                }
            }));
            y += 28;
            if (y > panelY + panelH - 24) break;
        }
    }

    private int filteredGlobalCommandCount(String query) {
        int count = 0;
        for (ModConfig.Entry entry : config.globalCommands) {
            if (query.isEmpty() || safeLower(entry.label).contains(query) || safeLower(entry.command).contains(query)) count++;
        }
        return count;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void addGroupCommand() {
        ModConfig.Entry newEntry = new ModConfig.Entry("New Command", "/help", true, "ITEM", "book");
        if (this.minecraft != null && !config.groups.isEmpty() && selectedGroup < config.groups.size()) {
            this.minecraft.setScreen(new EditCommandDialog(this, newEntry, true, () -> {
                config.groups.get(selectedGroup).commands.add(newEntry);
                config.save(contextKey);
            }));
        }
    }

    private void addGroup() {
        if (this.minecraft != null) {
            ModConfig.Group newGroup = new ModConfig.Group("New", "ITEM", "book");
            this.minecraft.setScreen(new EditGroupDialog(this, newGroup, true, () -> {
                config.groups.add(newGroup);
                selectedGroup = config.groups.size() - 1;
                config.selectedGroup = selectedGroup;
                config.save(contextKey);
            }));
        }
    }

    private int centerXPos(int panelX, int panelW, int contentW) {
        return panelX + (panelW / 2) - (contentW / 2);
    }

    private String trimText(String value, int maxWidth) {
        if (value == null || value.isEmpty() || this.font.width(value) <= maxWidth) return value == null ? "" : value;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (this.font.width(result + value.substring(i, i + 1)) > maxWidth) break;
            result.append(value.charAt(i));
        }
        return result.toString();
    }

    private void renderCustomButton(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int w, int h, int baseColor, int hoverColor, int mouseX, int mouseY, Runnable onClick) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        guiGraphics.fill(x, y, x + w, y + h, hovered ? hoverColor : baseColor);
        renderOutlineBox(guiGraphics, x, y, w, h, hovered ? 0xFF71717A : 0xFF3F3F46);
        guiGraphics.centeredText(this.font, text, x + (w / 2), y + ((h - 8) / 2), 0xFFFFFFFF);

        clickableButtons.add(new ButtonBounds(x, y, w, h, onClick));
    }

    private void renderIconPreview(GuiGraphicsExtractor guiGraphics, String type, String id, int x, int y) {
        if (type == null || "NONE".equalsIgnoreCase(type) || id == null || id.isEmpty()) return;

        if ("ITEM".equalsIgnoreCase(type)) {
            Identifier regId = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
            Item item = BuiltInRegistries.ITEM.getValue(regId);
            if (item != null && item != Items.AIR) {
                guiGraphics.item(new ItemStack(item), x, y);
            }
        } else if ("LOCAL".equalsIgnoreCase(type) || "URL".equalsIgnoreCase(type)) {
            Identifier tex = CustomIconManager.getTexture(id);
            if (tex != null && this.minecraft != null) {
                guiGraphics.enableScissor(x, y, x + 16, y + 16);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, 16, 16, 16, 16, -1);
                guiGraphics.disableScissor();
            }
        }
    }

    private void renderOutlineBox(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x, y, x + w, y + 1, color);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
        guiGraphics.fill(x, y, x + 1, y + h, color);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            for (ButtonBounds b : clickableButtons) {
                if (b.contains(mouseX, mouseY)) {
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void selectGroup(int index) {
        this.selectedGroup = index;
        this.commandScroll = 0;
        this.config.selectedGroup = index;
        LAST_SELECTED_GROUP.put(this.contextKey, index);
        this.config.save(this.contextKey);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            setMovementKey(GLFW.GLFW_KEY_W, false);
            setMovementKey(GLFW.GLFW_KEY_S, false);
            setMovementKey(GLFW.GLFW_KEY_A, false);
            setMovementKey(GLFW.GLFW_KEY_D, false);
            setMovementKey(GLFW.GLFW_KEY_SPACE, false);
        }
        this.config.selectedGroup = this.selectedGroup;
        LAST_SELECTED_GROUP.put(this.contextKey, this.selectedGroup);
        this.config.save(this.contextKey);
        super.onClose();
    }

    private record ButtonBounds(int x, int y, int w, int h, Runnable action) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}