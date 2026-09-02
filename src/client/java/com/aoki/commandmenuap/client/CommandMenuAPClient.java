package com.aoki.commandmenuap.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

// Client entrypoint initializing keybindings / キーバインドを初期化するクライアントエントリポイント
public class CommandMenuAPClient implements ClientModInitializer {
    private static KeyMapping keyOpen;

    @Override
    public void onInitializeClient() {
        // Register keybind category / キーバインドのカテゴリ登録
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("command_menu_ap", "keybinds")
        );

        // Register default keybinding ('G' key) / デフォルトキーバインドの登録（'G'キー）
        keyOpen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.command_menu_ap.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));

        // Client tick event checking for keypress / キー押下を検知するクライアント毎tickイベント
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyOpen.consumeClick()) {
                if (client.player != null) {
                    if (client.screen instanceof CommandMenuScreen) {
                        client.setScreen(null);
                    } else {
                        // Open the command menu screen / コマンドメニュー画面を開く
                        client.setScreen(new CommandMenuScreen());
                    }
                }
            }
        });
    }
}