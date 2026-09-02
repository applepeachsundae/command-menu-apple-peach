package com.aoki.commandmenuap.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

// EN: Helper for rendering item, url, and local icon labels
// JA: アイテム・URL・ローカルアイコンのラベル装飾ヘルパー
public class IconHelper {
    public static String getPrefixedLabel(String label, String iconType, String iconId) {
        if (iconType == null || "NONE".equalsIgnoreCase(iconType) || iconId == null || iconId.isEmpty()) {
            return label;
        }

        if ("ITEM".equalsIgnoreCase(iconType)) {
            Identifier id = Identifier.tryParse(iconId.contains(":") ? iconId : "minecraft:" + iconId);
            boolean exists = id != null && BuiltInRegistries.ITEM.containsKey(id);
            return (exists ? "§6[✦] §f" : "§7[?] §f") + label;
        } else if ("LOCAL".equalsIgnoreCase(iconType) || "URL".equalsIgnoreCase(iconType)) {
            boolean isUrl = iconId.startsWith("http://") || iconId.startsWith("https://");
            boolean exists = isUrl || CustomIconManager.ICONS_DIR.toPath().resolve(iconId).toFile().exists();
            return (exists ? (isUrl ? "§3[🌐] §f" : "§a[🖼] §f") : "§c[✕] §f") + label;
        }

        return label;
    }
}