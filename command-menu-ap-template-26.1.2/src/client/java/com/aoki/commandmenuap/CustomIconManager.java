package com.aoki.commandmenuap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomIconManager {
    // Identifier cache to avoid redundant texture registration / 重複登録を防ぐテクスチャキャッシュ
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    public static final File ICONS_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "command_menu_ap_icons");

    public static Identifier getTexture(String fileName) {
        if (CACHE.containsKey(fileName)) return CACHE.get(fileName);
        if (!ICONS_DIR.exists()) ICONS_DIR.mkdirs();

        File file = new File(ICONS_DIR, fileName);
        if (!file.exists()) return null;

        try (FileInputStream in = new FileInputStream(file)) {
            // Load image into native dynamic texture / 外部画像を動的テクスチャとして読み込み
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            String clean = fileName.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier id = Identifier.fromNamespaceAndPath("command_menu_ap", "icon_" + clean);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            CACHE.put(fileName, id);
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}