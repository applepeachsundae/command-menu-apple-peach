package com.aoki.commandmenuap.client;

import com.aoki.commandmenuap.ModConfig;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EditGroupDialog extends Screen {
    private final Screen parent;
    private final ModConfig.Group group;
    private final boolean isNew;
    private final Runnable onSave;

    private EditBox nameBox;
    private EditBox iconIdBox;
    private EditBox color1Box;
    private EditBox color2Box;

    private String iconType;
    private String colorMode;
    private String draftName;
    private String draftIconId;
    private String draftColor1;
    private String draftColor2;

    public EditGroupDialog(Screen parent, ModConfig.Group group, boolean isNew, Runnable onSave) {
        super(Component.literal(isNew ? "Add Group" : "Edit Group"));
        this.parent = parent;
        this.group = group;
        this.isNew = isNew;
        this.onSave = onSave;
        this.iconType = (group.iconType != null) ? group.iconType : "ITEM";
        this.colorMode = (group.colorMode != null) ? group.colorMode : "SOLID";
        this.draftName = group.name != null ? group.name : "Category";
        this.draftIconId = group.iconId != null ? group.iconId : "diamond";
        this.draftColor1 = group.color1 != null ? group.color1 : "#FFAA00";
        this.draftColor2 = group.color2 != null ? group.color2 : "#FF5555";
    }

    private void saveDraftFields() {
        if (this.nameBox != null) this.draftName = this.nameBox.getValue();
        if (this.iconIdBox != null) this.draftIconId = this.iconIdBox.getValue();
        if (this.color1Box != null) this.draftColor1 = this.color1Box.getValue();
        if (this.color2Box != null) this.draftColor2 = this.color2Box.getValue();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 75;

        this.nameBox = new EditBox(this.font, centerX - 100, startY, 200, 18, Component.literal("Group Name"));
        this.nameBox.setMaxLength(128);
        this.nameBox.setValue(this.draftName);
        this.addRenderableWidget(this.nameBox);

        // Siklus tipe ikon: ITEM -> URL -> LOCAL -> NONE
        ModernButton iconTypeBtn = new ModernButton(centerX - 100, startY + 22, 98, 18,
                Component.literal("Type: §e" + this.iconType),
                () -> {
                    if ("ITEM".equals(this.iconType)) this.iconType = "URL";
                    else if ("URL".equals(this.iconType)) this.iconType = "LOCAL";
                    else if ("LOCAL".equals(this.iconType)) this.iconType = "NONE";
                    else this.iconType = "ITEM";
                    this.saveDraftFields();
                    this.rebuildWidgets();
                });
        this.addRenderableWidget(iconTypeBtn);

        this.addRenderableWidget(new ModernButton(centerX + 2, startY + 22, 98, 18,
                Component.literal("§b🔍 Browse..."),
                () -> {
                    if (!"NONE".equalsIgnoreCase(this.iconType) && this.minecraft != null) {
                        this.saveDraftFields();
                        this.minecraft.setScreen(new IconPickerScreen(this, this.iconType, selected -> {
                            this.draftIconId = selected;
                        }));
                    }
                }));

        this.iconIdBox = new EditBox(this.font, centerX - 100, startY + 44, 200, 18, Component.literal("Icon ID"));
        this.iconIdBox.setMaxLength(4096);
        this.iconIdBox.setValue(this.draftIconId);
        this.addRenderableWidget(this.iconIdBox);

        ModernButton colorModeBtn = new ModernButton(centerX - 100, startY + 68, 200, 18,
                Component.literal("Color Mode: §b" + this.colorMode),
                () -> {
                    if ("SOLID".equals(this.colorMode)) this.colorMode = "GRADIENT";
                    else if ("GRADIENT".equals(this.colorMode)) this.colorMode = "RAINBOW";
                    else this.colorMode = "SOLID";
                    this.saveDraftFields();
                    this.rebuildWidgets();
                });
        this.addRenderableWidget(colorModeBtn);

        if ("SOLID".equals(this.colorMode)) {
            this.color1Box = new EditBox(this.font, centerX - 100, startY + 90, 200, 18, Component.literal("Color HEX"));
            this.color1Box.setValue(this.draftColor1);
            this.addRenderableWidget(this.color1Box);
        } else if ("GRADIENT".equals(this.colorMode)) {
            this.color1Box = new EditBox(this.font, centerX - 100, startY + 90, 98, 18, Component.literal("Start HEX"));
            this.color1Box.setValue(this.draftColor1);
            this.addRenderableWidget(this.color1Box);

            this.color2Box = new EditBox(this.font, centerX + 2, startY + 90, 98, 18, Component.literal("End HEX"));
            this.color2Box.setValue(this.draftColor2);
            this.addRenderableWidget(this.color2Box);
        }

        this.addRenderableWidget(new ModernButton(centerX - 100, startY + 116, 95, 20, Component.literal("Save"), () -> {
            this.saveDraftFields();
            this.group.name = this.draftName.trim();
            this.group.iconType = this.iconType;
            this.group.iconId = this.draftIconId.trim();
            this.group.colorMode = this.colorMode;
            if (this.color1Box != null) this.group.color1 = this.draftColor1.trim();
            if (this.color2Box != null) this.group.color2 = this.draftColor2.trim();
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