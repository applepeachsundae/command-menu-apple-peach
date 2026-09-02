package com.aoki.commandmenuap.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.awt.Color;

public class ColorHelper {
    public static int parseHex(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            String clean = hex.replace("#", "").trim();
            return Integer.parseInt(clean, 16);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    public static Component formatGroupTitle(String text, String colorMode, String color1, String color2) {
        if (text == null || text.isEmpty()) return Component.empty();
        String mode = (colorMode != null) ? colorMode.toUpperCase() : "SOLID";

        switch (mode) {
            case "RAINBOW" -> {
                long time = System.currentTimeMillis();
                MutableComponent rainbowText = Component.empty();
                for (int i = 0; i < text.length(); i++) {
                    float hue = (((time % 6000L) / 6000.0f) + (i * 0.08f)) % 1.0f;
                    int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
                    rainbowText.append(Component.literal(String.valueOf(text.charAt(i)))
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
                }
                return rainbowText;
            }
            case "GRADIENT" -> {
                int c1 = parseHex(color1, 0xFFAA00);
                int c2 = parseHex(color2, 0xFF5555);
                int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
                int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

                MutableComponent gradientText = Component.empty();
                int len = Math.max(1, text.length() - 1);

                for (int i = 0; i < text.length(); i++) {
                    float ratio = (float) i / len;
                    int r = (int) (r1 + ratio * (r2 - r1));
                    int g = (int) (g1 + ratio * (g2 - g1));
                    int b = (int) (b1 + ratio * (b2 - b1));
                    int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

                    gradientText.append(Component.literal(String.valueOf(text.charAt(i)))
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
                }
                return gradientText;
            }
            default -> {
                int rgb = parseHex(color1, 0xFFAA00);
                return Component.literal(text)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true));
            }
        }
    }
}