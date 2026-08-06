package com.tinysx.personachat;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculates text pixel width and lines according to Minecraft's default font character widths.
 */
public class TextWidthCalculator {

    private static final Map<Character, Integer> CHAR_WIDTHS = new HashMap<>();

    static {
        // Special character widths in Minecraft default font
        setChars("!.,:;i|", 2);
        setChars("'`l", 3);
        setChars(" I[]t", 4);
        setChars("kf{}", 5);
        setChars("@~", 7);
        // Default for standard latin letters/numbers is 6
    }

    private static void setChars(String chars, int width) {
        for (char c : chars.toCharArray()) {
            CHAR_WIDTHS.put(c, width);
        }
    }

    public static int getCharWidth(char c) {
        return CHAR_WIDTHS.getOrDefault(c, 6);
    }

    public static int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        boolean isColorCode = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' || c == '&') {
                isColorCode = true;
                continue;
            }

            if (isColorCode) {
                isColorCode = false;
                // Hex format check §x§R§R§G§G§B§B
                if (c == 'x' && i + 12 < text.length() && text.charAt(i + 1) == '§') {
                    i += 12; // skip hex
                }
                continue;
            }

            width += getCharWidth(c);
        }
        return width;
    }

    public static class RenderInfo {
        public final int width;
        public final int lines;

        public RenderInfo(int width, int lines) {
            this.width = width;
            this.lines = lines;
        }
    }

    public static RenderInfo calculate(String nameText, String msgText, int maxLineWidthPx) {
        String combined = nameText + (nameText.isEmpty() || msgText.isEmpty() ? "" : " ") + msgText;
        if (maxLineWidthPx <= 0) maxLineWidthPx = 240;

        int currentLineWidth = 0;
        int maxLineFound = 0;
        int lineCount = 1;

        String[] words = combined.split(" ");
        for (String word : words) {
            int wordWidth = getStringWidth(word);
            int spaceWidth = getCharWidth(' ');

            if (currentLineWidth + wordWidth > maxLineWidthPx) {
                if (currentLineWidth > 0) {
                    lineCount++;
                    maxLineFound = Math.max(maxLineFound, currentLineWidth);
                    currentLineWidth = wordWidth;
                } else {
                    maxLineFound = Math.max(maxLineFound, wordWidth);
                    currentLineWidth = 0;
                }
            } else {
                currentLineWidth += (currentLineWidth == 0 ? 0 : spaceWidth) + wordWidth;
            }
        }
        maxLineFound = Math.max(maxLineFound, currentLineWidth);

        return new RenderInfo(maxLineFound, lineCount);
    }
}
