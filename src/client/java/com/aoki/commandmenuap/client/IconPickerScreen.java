package com.aoki.commandmenuap.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.Locale;
import java.util.Comparator;

public class IconPickerScreen extends Screen {
    private final Screen parent;
    private final String iconType;
    private final Consumer<String> onSelect;

    private EditBox searchBox;
    private String searchQuery = "";
    private List<String> allEntries = new ArrayList<>();
    private int page = 0;
    private static final int PER_PAGE = 12;

    public IconPickerScreen(Screen parent, String iconType, Consumer<String> onSelect) {
        super(Component.literal("Select " + iconType + " Icon"));
        this.parent = parent;
        this.iconType = iconType;
        this.onSelect = onSelect;
        populateEntries();
    }

    @Override
    public void tick() {
        super.tick();
        CustomIconManager.tick();
    }

    private void populateEntries() {
        allEntries.clear();
        if ("ITEM".equalsIgnoreCase(iconType)) {
            BuiltInRegistries.ITEM.keySet().stream()
                    .filter(id -> !id.getPath().equals("player_head") && !id.getPath().equals("player_wall_head"))
                    .forEach(id -> allEntries.add(id.getPath()));
        } else if ("LOCAL".equalsIgnoreCase(iconType)) {
            File dir = CustomIconManager.ICONS_DIR;
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    for (File f : files) allEntries.add(f.getName());
                }
            }
        } else if ("URL".equalsIgnoreCase(iconType)) {
            List<String> history = CustomIconManager.getWebHistory();
            for (String url : history) {
                if (!allEntries.contains(url)) allEntries.add(url);
            }
        }
        allEntries.sort(Comparator.comparing((String id) -> !CustomIconManager.isPinned(id))
                .thenComparing(CustomIconManager::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelY = (this.height - 240) / 2;

        this.searchBox = new EditBox(this.font, centerX - 100, panelY + 32, 200, 18, Component.literal("Search"));
        this.searchBox.setMaxLength(4096);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(text -> {
            this.searchQuery = text;
            this.page = 0;
            this.rebuildWidgets();
            if (this.searchBox != null) {
                this.setFocused(this.searchBox);
                this.searchBox.setFocused(true);
            }
        });
        this.addRenderableWidget(this.searchBox);

        String query = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        List<String> filtered = allEntries.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(query)).toList();

        int actionWidth = "ITEM".equalsIgnoreCase(iconType) ? 16 : 52;
        int rowWidth = 20 + 2 + 92 + 4 + actionWidth;
        int columnGap = 8;
        int startX = centerX - ((rowWidth * 2 + columnGap) / 2);
        int startY = panelY + 56;
        int startIndex = page * PER_PAGE;
        int endIndex = Math.min(filtered.size(), startIndex + PER_PAGE);

        for (int i = startIndex; i < endIndex; i++) {
            final String selectedId = filtered.get(i);
            int slot = i - startIndex;
            int col = slot % 2;
            int row = slot / 2;

            int x = startX + (col * (rowWidth + columnGap));
            int y = startY + (row * 24);

            String displayName = CustomIconManager.getDisplayName(selectedId);
            displayName = trimText(displayName, 84);

            int actionX = x + 116;
            this.addRenderableWidget(new ModernButton(x + 22, y, 92, 20, Component.literal(displayName), () -> {
                this.onSelect.accept(selectedId);
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
            }));
            this.addRenderableWidget(new ModernButton(actionX, y, 16, 20,
                    Component.literal(CustomIconManager.isPinned(selectedId) ? "★" : "☆"), () -> {
                CustomIconManager.setPinned(selectedId, !CustomIconManager.isPinned(selectedId));
                populateEntries();
                this.rebuildWidgets();
            }));
            if (!"ITEM".equalsIgnoreCase(iconType)) {
                this.addRenderableWidget(new ModernButton(actionX + 18, y, 16, 20, Component.literal("✎"), () -> {
                    if (this.minecraft != null) this.minecraft.setScreen(new IconRenameDialog(this, selectedId));
                }));
                this.addRenderableWidget(new ModernButton(actionX + 36, y, 16, 20, Component.literal("X"), () -> {
                    CustomIconManager.delete(selectedId);
                    populateEntries();
                    this.rebuildWidgets();
                }));
            }
        }

        int maxPages = Math.max(0, (filtered.size() - 1) / PER_PAGE);
        int navY = panelY + 218;

        if (page > 0) {
            this.addRenderableWidget(new ModernButton(centerX - 100, navY, 48, 18, Component.literal("Prev"), () -> {
                this.page--;
                this.rebuildWidgets();
            }));
        }

        if (page < maxPages) {
            this.addRenderableWidget(new ModernButton(centerX + 52, navY, 48, 18, Component.literal("Next"), () -> {
                this.page++;
                this.rebuildWidgets();
            }));
        }

        this.addRenderableWidget(new ModernButton(centerX - 40, navY, 80, 18, Component.literal("Back"), () -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0080A10, 0xE0121520);
        int panelX = this.width / 2 - 190;
        int panelY = (this.height - 240) / 2;
        graphics.fill(panelX, panelY, panelX + 380, panelY + 240, 0xF51A1F2B);
        graphics.fill(panelX, panelY, panelX + 380, panelY + 2, 0xFF4F8CFF);
        graphics.outline(panelX, panelY, 380, 240, 0xFF354052);
        graphics.centeredText(this.font, this.title, this.width / 2, panelY + 7, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String query = this.searchBox == null ? "" : this.searchBox.getValue().toLowerCase().trim();
        List<String> filtered = allEntries.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(query)).toList();
        int startIndex = page * PER_PAGE;
        int endIndex = Math.min(filtered.size(), startIndex + PER_PAGE);
        int actionWidth = "ITEM".equalsIgnoreCase(iconType) ? 16 : 52;
        int rowWidth = 20 + 2 + 92 + 4 + actionWidth;
        int columnGap = 8;
        int startX = this.width / 2 - ((rowWidth * 2 + columnGap) / 2);
        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            int x = startX + ((slot % 2) * (rowWidth + columnGap));
            int y = panelY + 56 + ((slot / 2) * 24);
            graphics.fill(x, y, x + 20, y + 20, 0xFF202938);
            graphics.outline(x, y, 20, 20, 0xFF4B6385);
            renderPreview(graphics, filtered.get(i), x + 2, y + 2);
        }
    }

    private void renderPreview(GuiGraphicsExtractor graphics, String id, int x, int y) {
        if ("ITEM".equalsIgnoreCase(iconType)) {
            Identifier itemId = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)), x, y);
            }

        } else if ("LOCAL".equalsIgnoreCase(iconType) || "URL".equalsIgnoreCase(iconType)) {
            Identifier texture = CustomIconManager.getTexture(id);
            if (texture != null) {
                graphics.enableScissor(x, y, x + 16, y + 16);
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, 16, 16, 16, 16, -1);
                graphics.disableScissor();
            }
        }
    }

    private String trimText(String value, int maxWidth) {
            if (this.font.width(value) <= maxWidth) return value;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                String next = result + value.substring(i, i + 1);
                if (this.font.width(next) > maxWidth) break;
                result.append(value.charAt(i));
            }
            return result.toString();
        }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}