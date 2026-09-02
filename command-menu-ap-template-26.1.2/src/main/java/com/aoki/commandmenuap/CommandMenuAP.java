package com.aoki.commandmenuap;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandMenuAP implements ModInitializer {
    public static final String MOD_ID = "command_menu_ap";

    // Logger instance for console debugging / コンソール出力用のロガー
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Runs on game initialization / ゲーム初期化時に実行
        LOGGER.info("Command Menu AP initialized successfully!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}