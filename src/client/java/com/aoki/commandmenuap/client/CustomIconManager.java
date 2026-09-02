package com.aoki.commandmenuap.client;

import com.aoki.commandmenuap.CommandMenuAP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CustomIconManager {
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final Set<String> LOADING = new HashSet<>();
    private static final Map<String, List<NativeImage>> ANIMATED_FRAMES = new HashMap<>();
    private static final Map<String, DynamicTexture> ANIMATED_TEXTURES = new HashMap<>();
    private static long lastAnimationUpdate;
    private static int animationFrame;
    public static final File ICONS_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "command_menu_ap_icons");
    private static final File HISTORY_FILE = new File(ICONS_DIR, "icon_history.json");
    private static final File METADATA_FILE = new File(ICONS_DIR, "icon_metadata.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, IconMetadata> METADATA = new HashMap<>();

    private static class IconMetadata {
        String name;
        boolean pinned;
    }

    private static void loadMetadata() {
        if (!METADATA.isEmpty() || !METADATA_FILE.exists()) return;
        try (FileReader reader = new FileReader(METADATA_FILE)) {
            Map<String, IconMetadata> loaded = GSON.fromJson(reader, new TypeToken<Map<String, IconMetadata>>(){}.getType());
            if (loaded != null) METADATA.putAll(loaded);
        } catch (Exception ignored) {}
    }

    private static void saveMetadata() {
        try {
            if (!ICONS_DIR.exists()) ICONS_DIR.mkdirs();
            try (FileWriter writer = new FileWriter(METADATA_FILE)) {
                GSON.toJson(METADATA, writer);
            }
        } catch (Exception ignored) {}
    }

    public static String getDisplayName(String iconId) {
        loadMetadata();
        IconMetadata metadata = METADATA.get(iconId);
        return metadata != null && metadata.name != null && !metadata.name.isBlank() ? metadata.name : iconId;
    }

    public static boolean isPinned(String iconId) {
        loadMetadata();
        IconMetadata metadata = METADATA.get(iconId);
        return metadata != null && metadata.pinned;
    }

    public static void setPinned(String iconId, boolean pinned) {
        loadMetadata();
        IconMetadata metadata = METADATA.computeIfAbsent(iconId, key -> new IconMetadata());
        metadata.pinned = pinned;
        saveMetadata();
    }

    public static void rename(String iconId, String name) {
        loadMetadata();
        IconMetadata metadata = METADATA.computeIfAbsent(iconId, key -> new IconMetadata());
        metadata.name = name == null || name.isBlank() ? null : name.trim();
        saveMetadata();
    }

    public static void delete(String iconId) {
        loadMetadata();
        METADATA.remove(iconId);
        synchronized (CACHE) {
            CACHE.remove(iconId);
        }
        List<String> history = getWebHistory();
        history.remove(iconId);
        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            GSON.toJson(history, writer);
        } catch (Exception ignored) {}
        if (!iconId.startsWith("http://") && !iconId.startsWith("https://")) {
            File localFile = new File(ICONS_DIR, iconId);
            if (localFile.isFile()) localFile.delete();
        }
        saveMetadata();
    }

    public static List<String> getWebHistory() {
        if (!HISTORY_FILE.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(HISTORY_FILE)) {
            List<String> list = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void addWebHistory(String url) {
        if (url == null || !url.startsWith("http")) return;
        List<String> history = getWebHistory();
        if (!history.contains(url)) {
            history.add(0, url);
            try {
                if (!ICONS_DIR.exists()) ICONS_DIR.mkdirs();
                try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
                    GSON.toJson(history, writer);
                }
            } catch (Exception ignored) {}
        }
    }

    // EN: Asynchronous texture loader to prevent game freeze / JA: ゲーム停止を防ぐ非同期テクスチャローダー
    public static Identifier getTexture(String iconId) {
        if (iconId == null || iconId.isEmpty()) return null;
        synchronized (CACHE) {
            if (CACHE.containsKey(iconId)) return CACHE.get(iconId);
        }
        synchronized (LOADING) {
            if (!LOADING.add(iconId)) return null;
        }
        CompletableFuture.runAsync(() -> {
            try {
                NativeImage img = null;
                List<NativeImage> frames = null;
                if (iconId.startsWith("http://") || iconId.startsWith("https://")) {
                    HttpURLConnection connection = (HttpURLConnection) URI.create(iconId).toURL().openConnection();
                    connection.setRequestProperty("User-Agent", "CommandMenuAP/1.0");
                    connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
                    connection.setRequestProperty("Referer", "https://www.pinterest.com/");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.setInstanceFollowRedirects(true);
                    try (InputStream in = connection.getInputStream()) {
                        if (isGif(iconId, connection.getContentType())) frames = readGif(in);
                        else img = readImage(in);
                    }
                    connection.disconnect();
                    addWebHistory(iconId);
                } else {
                    if (!ICONS_DIR.exists()) ICONS_DIR.mkdirs();
                    File file = new File(ICONS_DIR, iconId);
                    if (file.exists()) {
                        try (FileInputStream in = new FileInputStream(file)) {
                            if (isGif(iconId, null)) frames = readGif(in);
                            else img = readImage(in);
                        }

                    }
                }

                if (frames != null && !frames.isEmpty()) {
                    img = frames.get(0);
                    synchronized (ANIMATED_FRAMES) {
                        ANIMATED_FRAMES.put(iconId, frames);
                    }
                }
                if (img != null) {
                    final NativeImage finalImg = img;
                    Minecraft.getInstance().execute(() -> {
                        try {
                            String clean = iconId.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
                            DynamicTexture tex = new DynamicTexture(() -> "custom_tex_" + clean, finalImg);
                            Identifier id = Identifier.fromNamespaceAndPath("command_menu_ap", "custom_icon_" + Math.abs(iconId.hashCode()));
                            Minecraft.getInstance().getTextureManager().register(id, tex);
                            synchronized (ANIMATED_TEXTURES) {
                                ANIMATED_TEXTURES.put(iconId, tex);
                            }
                            synchronized (CACHE) {
                                CACHE.put(iconId, id);
                            }
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) {
                CommandMenuAP.LOGGER.warn("Unable to load custom icon: {}", iconId, e);
            } finally {
                synchronized (LOADING) {
                    LOADING.remove(iconId);
                }
            }
        });

        return null;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastAnimationUpdate < 100) return;
        lastAnimationUpdate = now;
        animationFrame++;
        synchronized (ANIMATED_FRAMES) {
            for (Map.Entry<String, List<NativeImage>> entry : ANIMATED_FRAMES.entrySet()) {
                List<NativeImage> frames = entry.getValue();
                DynamicTexture texture;
                synchronized (ANIMATED_TEXTURES) {
                    texture = ANIMATED_TEXTURES.get(entry.getKey());
                }
                if (texture != null && !frames.isEmpty()) {
                    NativeImage frame = frames.get(animationFrame % frames.size());
                    if (frame.getWidth() == texture.getPixels().getWidth() && frame.getHeight() == texture.getPixels().getHeight()) {
                        texture.getPixels().copyFrom(frame);
                        texture.upload();
                    }
                }
            }
        }
    }

    private static List<NativeImage> readGif(InputStream input) throws java.io.IOException {
        List<NativeImage> frames = new ArrayList<>();
        try (ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("GIF");
            if (!readers.hasNext()) return frames;
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D canvasGraphics = canvas.createGraphics();
                canvasGraphics.setComposite(java.awt.AlphaComposite.Clear);
                canvasGraphics.fillRect(0, 0, width, height);
                canvasGraphics.setComposite(java.awt.AlphaComposite.SrcOver);
                for (int i = 0; i < reader.getNumImages(true); i++) {
                    BufferedImage frame = reader.read(i);
                    canvasGraphics.drawImage(frame, 0, 0, null);
                    BufferedImage snapshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    snapshot.getGraphics().drawImage(canvas, 0, 0, null);
                    frames.add(toNativeImage(snapshot));
                }

                canvasGraphics.dispose();
            } finally {
                reader.dispose();
            }
        }
        return frames;
    }

    private static boolean isGif(String iconId, String contentType) {
        return iconId.toLowerCase().contains(".gif") ||
                (contentType != null && contentType.toLowerCase().contains("image/gif"));
    }

    private static NativeImage readImage(InputStream input) throws java.io.IOException {
        BufferedImage buffered = ImageIO.read(input);
        if (buffered == null) return null;
        return toNativeImage(buffered);
    }

    private static NativeImage toNativeImage(BufferedImage buffered) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, buffered.getWidth(), buffered.getHeight(), false);
        for (int y = 0; y < buffered.getHeight(); y++) {
            for (int x = 0; x < buffered.getWidth(); x++) {
                int argb = buffered.getRGB(x, y);
                int abgr = (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x000000FF) << 16);
                image.setPixelABGR(x, y, abgr);
            }
        }
        return image;
    }
}