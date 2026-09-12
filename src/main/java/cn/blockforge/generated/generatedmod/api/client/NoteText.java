package cn.blockforge.generated.generatedmod.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 可复用的「说明文字强调」工具：把说明文本里的括号内容渲染成更有辨识度的颜色。
 *
 * <ul>
 *   <li>{@code （…）} / {@code (…)} 内的补充说明 → <b>暖金色</b>（{@link #NOTE_RGB}）</li>
 *   <li>{@code 【…】} 内的条目标题 → <b>亮青色</b>（{@link #HEAD_RGB}）</li>
 *   <li>其余正文保持传入的基础色（{@link #BASE_COLOR} 为推荐值）</li>
 * </ul>
 *
 * <p>两种用法：</p>
 * <pre>
 *   // 1) 交给原版文本管线（可折行、可入提示框）：样式随 Component 保留
 *   Component c = NoteText.styled("基础 9 格（一排）；最多 81 格");
 *
 *   // 2) 直接绘制到界面（支持一行内多段括号）
 *   NoteText.draw(graphics, font, text, x, y, baseColor);
 *   NoteText.drawCentered(graphics, font, text, centerX, y, baseColor);
 * </pre>
 */
public final class NoteText {

    /** 括号内补充说明的颜色（暖金）。 */
    public static final int NOTE_RGB = 0xFFC66B;
    /** 【】条目标题的颜色（亮青）。 */
    public static final int HEAD_RGB = 0x7FD4FF;
    /** 推荐的基础正文色。 */
    public static final int BASE_COLOR = 0xFFC8D3E0;

    private static final Style NOTE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(NOTE_RGB));
    private static final Style HEAD_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(HEAD_RGB));

    private NoteText() {
    }

    /** 把一行说明文本转成带强调样式的组件（括号内容着色，其余保持原样）。 */
    public static MutableComponent styled(String text) {
        MutableComponent root = Component.empty();
        if (text == null || text.isEmpty()) {
            return root;
        }
        StringBuilder buffer = new StringBuilder();
        int mode = 0;   // 0=正文 1=（）内 2=【】内
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '（' || c == '(') {
                mode = flush(root, buffer, mode, 1);
            } else if (c == '【') {
                mode = flush(root, buffer, mode, 2);
            }
            buffer.append(c);
            if (c == '）' || c == ')' || c == '】') {
                mode = flush(root, buffer, mode, 0);
            }
        }
        flush(root, buffer, mode, 0);
        return root;
    }

    /** 批量转换（供滚动文本、帮助页等使用）。 */
    public static List<Component> styled(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                out.add(styled(line));
            }
        }
        return out;
    }

    /** 直接绘制：括号内容用强调色，其余用 baseColor（支持一行内多段括号）。 */
    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int baseColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int cursor = x;
        StringBuilder buffer = new StringBuilder();
        int mode = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '（' || c == '(') {
                cursor = drawSegment(graphics, font, buffer, mode, cursor, y, baseColor);
                mode = 1;
            } else if (c == '【') {
                cursor = drawSegment(graphics, font, buffer, mode, cursor, y, baseColor);
                mode = 2;
            }
            buffer.append(c);
            if (c == '）' || c == ')' || c == '】') {
                cursor = drawSegment(graphics, font, buffer, mode, cursor, y, baseColor);
                mode = 0;
            }
        }
        drawSegment(graphics, font, buffer, mode, cursor, y, baseColor);
    }

    /** 直接绘制（以 centerX 为水平中心）。 */
    public static void drawCentered(GuiGraphics graphics, Font font, String text, int centerX, int y, int baseColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        draw(graphics, font, text, centerX - font.width(text) / 2, y, baseColor);
    }

    // ---- 内部实现 ----

    /** 把缓冲区内容按当前模式并入 Component，并返回新模式。 */
    private static int flush(MutableComponent root, StringBuilder buffer, int mode, int nextMode) {
        if (buffer.length() > 0) {
            MutableComponent part = Component.literal(buffer.toString());
            if (mode == 1) {
                part.withStyle(NOTE_STYLE);
            } else if (mode == 2) {
                part.withStyle(HEAD_STYLE);
            }
            root.append(part);
            buffer.setLength(0);
        }
        return nextMode;
    }

    /** 绘制缓冲区内容并返回新的绘制游标 x。 */
    private static int drawSegment(GuiGraphics graphics, Font font, StringBuilder buffer, int mode,
                                   int cursor, int y, int baseColor) {
        if (buffer.length() == 0) {
            return cursor;
        }
        String segment = buffer.toString();
        int color = switch (mode) {
            case 1 -> 0xFF000000 | NOTE_RGB;
            case 2 -> 0xFF000000 | HEAD_RGB;
            default -> baseColor;
        };
        graphics.drawString(font, segment, cursor, y, color, false);
        buffer.setLength(0);
        return cursor + font.width(segment);
    }
}
