package com.aoki.commandmenuap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

// EN: Per-server and per-world configuration manager / JA: サーバー・ワールド個別設定マネージャー
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "command_menu_ap");

    public String serverTitle = "Command Menu";
    public int selectedGroup = 0;
    public List<Group> groups = new ArrayList<>();
    public List<Entry> globalCommands = new ArrayList<>();

    // EN: Category Group class with color properties / JA: カラープロパティ付きカテゴリグループクラス
    public static class Group {
        public String name;
        public String iconType; // "ITEM", "EFFECT", "CUSTOM", or "NONE"
        public String iconId;
        
        // EN: Custom color settings: SOLID, GRADIENT, or RAINBOW
        // JA: カスタムカラー設定（SOLID: 単色, GRADIENT: グラデーション, RAINBOW: アニメーション虹色）
        public String colorMode = "SOLID";
        public String color1 = "#FFAA00";
        public String color2 = "#FF5555";
        public boolean pinned;
        public long pinnedAt;

        public List<Entry> commands = new ArrayList<>();

        public Group(String name, String iconType, String iconId) {
            this.name = name;
            this.iconType = (iconType == null || iconType.isEmpty()) ? "NONE" : iconType;
            this.iconId = (iconId == null) ? "" : iconId;
            this.colorMode = "SOLID";
            this.color1 = "#FFAA00";
            this.color2 = "#FF5555";
        }
    }

    // EN: Command Entry class / JA: コマンド項目クラス
    public static class Entry {
        public String label;
        public String command;
        public boolean closeOnClick;
        public String iconType;
        public String iconId;
        public boolean pinned;

        public Entry(String label, String command, boolean closeOnClick, String iconType, String iconId) {
            this.label = label;
            this.command = command;
            this.closeOnClick = closeOnClick;
            this.iconType = (iconType == null || iconType.isEmpty()) ? "NONE" : iconType;
            this.iconId = (iconId == null) ? "" : iconId;
        }
    }

    private static File getTargetFile(String contextKey) {
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
        String safeKey = (contextKey == null || contextKey.isEmpty()) ? "default" : contextKey.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return new File(CONFIG_DIR, safeKey + ".json");
    }

    public static ModConfig get(String contextKey) {
        File file = getTargetFile(contextKey);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ModConfig cfg = GSON.fromJson(reader, ModConfig.class);
                if (cfg != null) {
                    if (cfg.groups == null) cfg.groups = new ArrayList<>();
                    if (cfg.globalCommands == null) cfg.globalCommands = new ArrayList<>();
                    cfg.removePlaceholderCommands();
                    cfg.save(contextKey);
                    return cfg;
                }

            } catch (Exception ignored) {}
        }
        ModConfig cfg = new ModConfig();
        cfg.save(contextKey);
        return cfg;
    }

    private void removePlaceholderCommands() {
        globalCommands.removeIf(ModConfig::isPlaceholder);
        for (Group group : groups) {
            if (group != null && group.commands != null) group.commands.removeIf(ModConfig::isPlaceholder);
        }
    }

    private static boolean isPlaceholder(Entry entry) {
        return entry != null && entry.label != null && "New Command".equalsIgnoreCase(entry.label.trim());
    }

    public void save(String contextKey) {
        File file = getTargetFile(contextKey);
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {}
    }
}