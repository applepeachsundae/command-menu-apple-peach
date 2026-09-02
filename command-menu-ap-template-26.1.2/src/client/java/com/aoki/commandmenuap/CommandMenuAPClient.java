package com.aoki.commandmenuap.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CommandMenuAPClient implements ClientModInitializer {
    private static KeyBinding keyOpen;

    @Override
    public void onInitializeClient() {
        // Register keybind (Default: 'M') / キーバインドの登録（デフォルト: 'M'キー）
        keyOpen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.command_menu_ap.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.command_menu_ap"
        ));

        // Client tick event / 毎tickのキー検知イベント
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyOpen.wasPressed()) {
                if (client.player != null) {
                    // Open GUI screen / GUI画面を開く
                    client.setScreen(new CommandMenuScreen());
                }
            }
        });
    }
}