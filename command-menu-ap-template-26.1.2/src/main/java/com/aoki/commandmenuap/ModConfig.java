package com.aoki.commandmenuap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "command_menu_ap.json");

    public String serverTitle = "Donut SMP";
    public List<Group> groups = new ArrayList<>();

    public static class Group {
        public String name;
        public List<Entry> commands = new ArrayList<>();

        public Group(String name) {
            this.name = name;
        }
    }

    public static class Entry {
        public String label;
        public String command;
        // Icon type: "ITEM" or "CUSTOM" / アイコンの種類: "ITEM" または "CUSTOM"
        public String iconType;
        // Value: item ID or file name / アイテムIDまたは画像ファイル名
        public String iconValue;

        public Entry(String label, String command, String iconType, String iconValue) {
            this.label = label;
            this.command = command;
            this.iconType = iconType;
            this.iconValue = iconValue;
        }
    }

    public static ModConfig get() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                ModConfig cfg = GSON.fromJson(reader, ModConfig.class);
                if (cfg != null && cfg.groups != null) return cfg;
            } catch (Exception ignored) {}
        }
        ModConfig cfg = createDefault();
        cfg.save();
        return cfg;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception ignored) {}
    }

    private static ModConfig createDefault() {
        ModConfig cfg = new ModConfig();

        // Main group replicating the 2-column menu / 2列メニューを再現したメイングループ
        Group main = new Group("Main");
        main.commands.add(new Entry("Homes", "/homes", "ITEM", "minecraft:red_bed"));
        main.commands.add(new Entry("RTP", "/rtp", "ITEM", "minecraft:ender_pearl"));
        main.commands.add(new Entry("Auction", "/ah", "ITEM", "minecraft:gold_ingot"));
        main.commands.add(new Entry("RTP Queue", "/rtp queue", "ITEM", "minecraft:compass"));
        main.commands.add(new Entry("Quick Buy", "/shop", "ITEM", "minecraft:emerald"));
        main.commands.add(new Entry("Friends", "/friends", "ITEM", "minecraft:player_head"));
        main.commands.add(new Entry("Orders", "/orders", "ITEM", "minecraft:paper"));
        main.commands.add(new Entry("Shard Shop", "/shardshop", "ITEM", "minecraft:amethyst_shard"));
        main.commands.add(new Entry("Sell", "/sell", "ITEM", "minecraft:raw_gold"));
        main.commands.add(new Entry("Pay", "/pay", "ITEM", "minecraft:diamond"));
        main.commands.add(new Entry("Teleport", "/tpa", "ITEM", "minecraft:chorus_fruit"));
        main.commands.add(new Entry("Stats", "/stats", "ITEM", "minecraft:writable_book"));
        main.commands.add(new Entry("Leaderboards", "/lb", "ITEM", "minecraft:trophy"));
        main.commands.add(new Entry("Settings", "/settings", "ITEM", "minecraft:repeater"));
        cfg.groups.add(main);

        // Extra group for essentials / 予備の便利コマンドグループ
        Group essentials = new Group("Essentials");
        essentials.commands.add(new Entry("Spawn", "/spawn", "ITEM", "minecraft:compass"));
        essentials.commands.add(new Entry("Discord", "/discord", "CUSTOM", "discord.png"));
        cfg.groups.add(essentials);

        return cfg;
    }
}