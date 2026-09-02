package com.aoki.commandmenuap;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.text.Text;

public class CommandMenuScreen extends Screen {
    private final ModConfig config;
    private int selectedGroup = 0;

    public CommandMenuScreen() {
        super(Text.literal("Command Menu"));
        this.config = ModConfig.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int topY = 22;

        // 1. Group / Category Tabs / グループ・カテゴリタブ
        if (!config.groups.isEmpty()) {
            int tabW = 75;
            int totalTabsW = config.groups.size() * (tabW + 4);
            int tabStartX = centerX - (totalTabsW / 2);

            for (int i = 0; i < config.groups.size(); i++) {
                final int idx = i;
                ModConfig.Group g = config.groups.get(i);
                boolean active = (i == selectedGroup);

                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal((active ? "§6" : "§7") + g.name),
                        b -> {
                            selectedGroup = idx;
                            // Reinitialize UI on group selection / タブ選択時にUIを再初期化
                            this.clearAndInit();
                        })
                        .dimensions(tabStartX + (i * (tabW + 4)), topY, tabW, 18)
                        .build());
            }
        }

        // 2. Command Button Grid (2 Columns Layout) / コマンドボタン配置（2列レイアウト）
        if (!config.groups.isEmpty() && selectedGroup < config.groups.size()) {
            ModConfig.Group activeG = config.groups.get(selectedGroup);
            int btnW = 115;
            int btnH = 20;
            int gapX = 4;
            int gapY = 3;
            int gridStartY = topY + 26;

            for (int i = 0; i < activeG.commands.size(); i++) {
                ModConfig.Entry cmd = activeG.commands.get(i);
                int col = i % 2;
                int row = i / 2;

                int x = (col == 0) ? (centerX - btnW - (gapX / 2)) : (centerX + (gapX / 2));
                int y = gridStartY + (row * (btnH + gapY));

                this.addDrawableChild(new CommandButton(x, y, btnW, btnH, cmd, b -> {
                    if (this.client != null && this.client.player != null) {
                        // Send chat command / チャットコマンドを送信
                        this.client.player.networkHandler.sendCommand(cmd.command.replaceFirst("^/", ""));
                        this.close();
                    }
                }));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Header title display / ヘッダータイトルの描画
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l" + config.serverTitle), this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        // Do not pause in multiplayer / マルチプレイ中にゲームを一時停止しない
        return false;
    }

    // Button component supporting item and image rendering / アイテム・画像描画対応ボタン
    private static class CommandButton extends ButtonWidget {
        private final ModConfig.Entry entry;
        private final ItemStack stack;
        private final Identifier customTex;

        public CommandButton(int x, int y, int w, int h, ModConfig.Entry entry, PressAction onPress) {
            super(x, y, w, h, Text.literal("   " + entry.label), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.entry = entry;

            if ("ITEM".equalsIgnoreCase(entry.iconType)) {
                Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(entry.iconValue));
                this.stack = new ItemStack(item != null ? item : Items.AIR);
                this.customTex = null;
            } else {
                this.stack = ItemStack.EMPTY;
                this.customTex = CustomIconManager.getTexture(entry.iconValue);
            }
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
            int iconX = this.getX() + 3;
            int iconY = this.getY() + (this.height - 16) / 2;

            if (!this.stack.isEmpty()) {
                // Render Minecraft item / バニラアイテムを描画
                context.drawItem(this.stack, iconX, iconY);
            } else if (this.customTex != null) {
                // Render external custom texture / 外部テクスチャを描画
                RenderSystem.setShaderTexture(0, this.customTex);
                context.drawTexture(this.customTex, iconX, iconY, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}