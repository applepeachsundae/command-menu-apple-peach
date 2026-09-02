package com.aoki.commandmenuap.client;

import com.aoki.commandmenuap.ModConfig;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EditCommandDialog extends Screen {
    private final Screen parent;
    private final ModConfig.Entry entry;
    private final boolean isNew;
    private final Runnable onSave;

    private EditBox labelBox;
    private EditBox commandBox;
    private EditBox iconIdBox;
    private boolean closeOnClick;
    private String iconType;

    public EditCommandDialog(Screen parent, ModConfig.Entry entry, boolean isNew, Runnable onSave) {
        super(Component.literal(isNew ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.entry = entry;
        this.isNew = isNew;
        this.onSave = onSave;
        this.closeOnClick = entry.closeOnClick;
        this.iconType = entry.iconType != null ? entry.iconType : "ITEM";
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 75;

        this.labelBox = new EditBox(this.font, centerX - 100, startY, 200, 18, Component.literal("Label"));
        this.labelBox.setMaxLength(128);
        this.labelBox.setValue(this.entry.label);
        this.addRenderableWidget(this.labelBox);

        this.commandBox = new EditBox(this.font, centerX - 100, startY + 22, 200, 18, Component.literal("Command"));
        this.commandBox.setMaxLength(1024);
        this.commandBox.setValue(this.entry.command);
        this.addRenderableWidget(this.commandBox);

        // Siklus tipe ikon: ITEM -> URL -> LOCAL -> NONE
        ModernButton typeBtn = new ModernButton(centerX - 100, startY + 44, 98, 18,
                Component.literal("Type: §e" + this.iconType), () -> {
                    if ("ITEM".equals(this.iconType)) this.iconType = "URL";
                    else if ("URL".equals(this.iconType)) this.iconType = "LOCAL";
                    else if ("LOCAL".equals(this.iconType)) this.iconType = "NONE";
                    else this.iconType = "ITEM";
                    this.rebuildWidgets();
                });
        this.addRenderableWidget(typeBtn);

        this.addRenderableWidget(new ModernButton(centerX + 2, startY + 44, 98, 18,
                Component.literal("Browse..."), () -> {
                    if (!"NONE".equalsIgnoreCase(this.iconType) && this.minecraft != null) {
                        this.minecraft.setScreen(new IconPickerScreen(this, this.iconType, selected -> {
                            this.entry.iconId = selected;
                            if (this.iconIdBox != null) this.iconIdBox.setValue(selected);
                        }));
                    }
                }));

        this.iconIdBox = new EditBox(this.font, centerX - 100, startY + 66, 200, 18, Component.literal("Icon ID"));
        this.iconIdBox.setMaxLength(4096);
        this.iconIdBox.setValue(this.entry.iconId != null ? this.entry.iconId : "diamond");
        this.addRenderableWidget(this.iconIdBox);

        this.addRenderableWidget(new ModernButton(centerX - 100, startY + 90, 200, 18,
                Component.literal("Close On Click: " + (this.closeOnClick ? "YES" : "NO")), () -> {
                    this.closeOnClick = !this.closeOnClick;
                })
                );

        this.addRenderableWidget(new ModernButton(centerX - 100, startY + 116, 95, 20, Component.literal("Save"), () -> {
            this.entry.label = this.labelBox.getValue();
            this.entry.command = this.commandBox.getValue();
            this.entry.closeOnClick = this.closeOnClick;
            this.entry.iconType = this.iconType;
            this.entry.iconId = this.iconIdBox.getValue();
            this.onSave.run();
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));

        this.addRenderableWidget(new ModernButton(centerX + 5, startY + 116, 95, 20, Component.literal("Cancel"), () -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0080A10, 0xE0121520);
        int panelX = this.width / 2 - 120;
        int panelY = this.height / 2 - 93;
        graphics.fill(panelX, panelY, panelX + 240, panelY + 166, 0xF51A1F2B);
        graphics.fill(panelX, panelY, panelX + 240, panelY + 2, 0xFF4F8CFF);
        graphics.outline(panelX, panelY, 240, 166, 0xFF354052);
        graphics.centeredText(this.font, this.title, this.width / 2, panelY + 8, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}