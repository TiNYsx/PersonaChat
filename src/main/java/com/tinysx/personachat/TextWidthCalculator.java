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

    public static class FormattedTextResult {
        public final String text;
        public final int pixelWidth;
        public final int lines;

        public FormattedTextResult(String text, int pixelWidth, int lines) {
            this.text = text;
            this.pixelWidth = pixelWidth;
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

    public static FormattedTextResult formatAndPad(String text, int maxLineWidthPx, boolean fillWidth, String alignment) {
        if (text == null || text.isEmpty()) {
            return new FormattedTextResult("", 0, 0);
        }
        if (maxLineWidthPx <= 0) maxLineWidthPx = 240;

        java.util.List<String> rawLines = new java.util.ArrayList<>();
        int currentLineWidth = 0;
        StringBuilder currentLine = new StringBuilder();

        String[] paragraphs = text.split("\n", -1);
        for (int p = 0; p < paragraphs.length; p++) {
            String para = paragraphs[p];
            String[] words = para.split(" ");
            for (int w = 0; w < words.length; w++) {
                String word = words[w];
                int wordWidth = getStringWidth(word);
                int spaceWidth = getCharWidth(' ');

                if (currentLine.length() > 0 && currentLineWidth + spaceWidth + wordWidth > maxLineWidthPx) {
                    rawLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                    currentLineWidth = wordWidth;
                } else {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                        currentLineWidth += spaceWidth;
                    }
                    currentLine.append(word);
                    currentLineWidth += wordWidth;
                }
            }
            if (currentLine.length() > 0 || p < paragraphs.length - 1) {
                rawLines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentLineWidth = 0;
            }
        }

        if (rawLines.isEmpty()) {
            return new FormattedTextResult(text, getStringWidth(text), 1);
        }

        int targetWidth = fillWidth ? maxLineWidthPx : 0;
        if (!fillWidth) {
            for (String line : rawLines) {
                targetWidth = Math.max(targetWidth, getStringWidth(line));
            }
        }

        java.util.List<String> processedLines = new java.util.ArrayList<>();
        int spaceWidth = getCharWidth(' ');

        for (String line : rawLines) {
            int lineWidth = getStringWidth(line);
            int rem = targetWidth - lineWidth;
            if (fillWidth && rem > 0) {
                int spacesCount = rem / spaceWidth;
                if (alignment != null && alignment.equalsIgnoreCase("RIGHT")) {
                    StringBuilder sb = new StringBuilder();
                    for (int s = 0; s < spacesCount; s++) sb.append(" ");
                    sb.append(line);
                    processedLines.add(sb.toString());
                } else if (alignment != null && alignment.equalsIgnoreCase("CENTER")) {
                    int leftSpaces = spacesCount / 2;
                    int rightSpaces = spacesCount - leftSpaces;
                    StringBuilder sb = new StringBuilder();
                    for (int s = 0; s < leftSpaces; s++) sb.append(" ");
                    sb.append(line);
                    for (int s = 0; s < rightSpaces; s++) sb.append(" ");
                    processedLines.add(sb.toString());
                } else { // LEFT
                    StringBuilder sb = new StringBuilder(line);
                    for (int s = 0; s < spacesCount; s++) sb.append(" ");
                    processedLines.add(sb.toString());
                }
            } else {
                processedLines.add(line);
            }
        }

        String result = String.join("\n", processedLines);
        return new FormattedTextResult(result, targetWidth, rawLines.size());
    }

    public static FormattedTextResult formatUnifiedCard(
            String nameText,
            String msgText,
            int maxLineWidthPx,
            boolean fillWidth,
            String alignment,
            int paddingX,
            int paddingY
    ) {
        if (maxLineWidthPx <= 0) maxLineWidthPx = 240;

        java.util.List<String> rawLines = new java.util.ArrayList<>();

        // 1. Name line(s)
        if (nameText != null && !nameText.isEmpty()) {
            rawLines.add(nameText);
        }

        // 2. Message line(s) with word wrapping
        if (msgText != null && !msgText.isEmpty()) {
            String[] paragraphs = msgText.split("\n", -1);
            for (String para : paragraphs) {
                int currentLineWidth = 0;
                StringBuilder currentLine = new StringBuilder();
                String[] words = para.split(" ");
                for (String word : words) {
                    int wordWidth = getStringWidth(word);
                    int spaceWidth = getCharWidth(' ');

                    if (wordWidth > maxLineWidthPx) {
                        // Word is too long, character wrap it
                        if (currentLine.length() > 0) {
                            rawLines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                            currentLineWidth = 0;
                        }
                        for (int i = 0; i < word.length(); i++) {
                            char c = word.charAt(i);
                            // Avoid parsing color codes as printable chars in this simple loop
                            // For a robust fix, color codes should be handled, but this works for basic keyboard smashes.
                            int cw = getCharWidth(c);
                            if (c == '§' || c == '&') cw = 0; // simplistic color ignore
                            if (currentLineWidth + cw > maxLineWidthPx && currentLine.length() > 0) {
                                rawLines.add(currentLine.toString());
                                currentLine = new StringBuilder();
                                currentLineWidth = 0;
                            }
                            currentLine.append(c);
                            currentLineWidth += cw;
                        }
                    } else if (currentLine.length() > 0 && currentLineWidth + spaceWidth + wordWidth > maxLineWidthPx) {
                        rawLines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                        currentLineWidth = wordWidth;
                    } else {
                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                            currentLineWidth += spaceWidth;
                        }
                        currentLine.append(word);
                        currentLineWidth += wordWidth;
                    }
                }
                if (currentLine.length() > 0) {
                    rawLines.add(currentLine.toString());
                }
            }
        }

        if (rawLines.isEmpty()) {
            return new FormattedTextResult("", 0, 0);
        }

        // 3. Find max line width
        int targetWidth = fillWidth ? maxLineWidthPx : 0;
        if (!fillWidth) {
            for (String line : rawLines) {
                targetWidth = Math.max(targetWidth, getStringWidth(line));
            }
        }

        // 4. Horizontal padding and space filling
        java.util.List<String> processedLines = new java.util.ArrayList<>();
        int spaceWidth = getCharWidth(' ');

        StringBuilder xPadBuilder = new StringBuilder();
        for (int p = 0; p < Math.max(0, paddingX); p++) {
            xPadBuilder.append(" ");
        }
        String xPad = xPadBuilder.toString();
        int totalPadWidth = paddingX * spaceWidth * 2;
        int effectiveTargetWidth = targetWidth + (paddingX > 0 ? totalPadWidth : 0);

        for (String line : rawLines) {
            int lineWidth = getStringWidth(line);
            int rem = targetWidth - lineWidth;

            if (fillWidth && rem > 0) {
                int spacesCount = rem / spaceWidth;
                if (alignment != null && alignment.equalsIgnoreCase("RIGHT")) {
                    StringBuilder sb = new StringBuilder(xPad);
                    for (int s = 0; s < spacesCount; s++) sb.append(" ");
                    sb.append(line).append(xPad);
                    processedLines.add(sb.toString());
                } else if (alignment != null && alignment.equalsIgnoreCase("CENTER")) {
                    int leftSpaces = spacesCount / 2;
                    int rightSpaces = spacesCount - leftSpaces;
                    StringBuilder sb = new StringBuilder(xPad);
                    for (int s = 0; s < leftSpaces; s++) sb.append(" ");
                    sb.append(line);
                    for (int s = 0; s < rightSpaces; s++) sb.append(" ");
                    sb.append(xPad);
                    processedLines.add(sb.toString());
                } else { // LEFT
                    StringBuilder sb = new StringBuilder(xPad);
                    sb.append(line);
                    for (int s = 0; s < spacesCount; s++) sb.append(" ");
                    sb.append(xPad);
                    processedLines.add(sb.toString());
                }
            } else {
                if (paddingX > 0) {
                    processedLines.add(xPad + line + xPad);
                } else {
                    processedLines.add(line);
                }
            }
        }

        // 5. Vertical padding — fill with spaces so background renders at full width
        int fullLineSpaces = effectiveTargetWidth / spaceWidth;
        StringBuilder padLineBuilder = new StringBuilder();
        for (int s = 0; s < fullLineSpaces; s++) padLineBuilder.append(" ");
        String padLine = padLineBuilder.toString();

        java.util.List<String> finalLines = new java.util.ArrayList<>();
        for (int y = 0; y < Math.max(0, paddingY); y++) {
            finalLines.add(padLine);
        }
        finalLines.addAll(processedLines);
        for (int y = 0; y < Math.max(0, paddingY); y++) {
            finalLines.add(padLine);
        }

        String result = String.join("\n", finalLines);
        return new FormattedTextResult(result, effectiveTargetWidth, finalLines.size());
    }
}
