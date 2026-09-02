package com.aoki.commandmenuap.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class IconRenameDialog extends Screen {
    private final Screen parent;
    private final String iconId;
    private EditBox nameBox;

    public IconRenameDialog(Screen parent, String iconId) {
        super(Component.literal("Rename Icon"));
        this.parent = parent;
        this.iconId = iconId;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 25;
        this.nameBox = new EditBox(this.font, x, y, 200, 20, Component.literal("Icon name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setValue(CustomIconManager.getDisplayName(iconId));
        this.addRenderableWidget(this.nameBox);
        this.addRenderableWidget(new ModernButton(x, y + 28, 96, 20, Component.literal("Save"), () -> {
            CustomIconManager.rename(iconId, this.nameBox.getValue());
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }));
        this.addRenderableWidget(new ModernButton(x + 104, y + 28, 96, 20, Component.literal("Cancel"), () -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xD0080A10, 0xF0121520);
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
