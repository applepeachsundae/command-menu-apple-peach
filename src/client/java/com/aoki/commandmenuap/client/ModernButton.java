package com.aoki.commandmenuap.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public final class ModernButton extends AbstractButton {
    private final Runnable action;
    private final int color;
    private final int hoverColor;

    public ModernButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
        this.color = 0xFF202938;
        this.hoverColor = 0xFF315A91;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int fill = this.isHovered() ? this.hoverColor : this.color;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), fill);
        graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFF4B6385);
        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
