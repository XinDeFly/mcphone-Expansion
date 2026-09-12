package cn.blockforge.generated.generatedmod.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 可复用的滚动文本视图：自动折行、右侧滑条、底部淡出。
 *
 * <p>支持鼠标滚轮滚动、点击轨道跳转与按住滑块拖拽；
 * 供手机使用说明页与桌面帮助页等任意需要长文本滚动的界面共用。</p>
 */
public final class ScrollableText {

    /** 滑条宽度。 */
    public static final int BAR_WIDTH = 4;
    /** 行高。 */
    public static final int ROW_HEIGHT = 10;

    private final List<FormattedCharSequence> rows = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private boolean dragging;
    private int barX;
    private int trackY;
    private int trackH;
    private int thumbH;

    /**
     * 重置内容：按给定宽度折行（空行跳过）。
     *
     * <p>自动套用 {@link NoteText} 强调样式：{@code （…）} 内补充说明为暖金色、
     * {@code 【…】} 条目标题为亮青色，正文保持 {@link NoteText#BASE_COLOR}。</p>
     */
    public void setLines(List<String> lines, Font font, int rowWidth) {
        this.setStyledLines(NoteText.styled(lines), font, rowWidth);
    }

    /** 重置内容（已带样式的文本，样式随折行保留）。 */
    public void setStyledLines(List<Component> lines, Font font, int rowWidth) {
        this.rows.clear();
        if (lines != null) {
            for (Component line : lines) {
                if (line != null && !line.getString().isEmpty()) {
                    this.rows.addAll(font.split(line, rowWidth));
                }
            }
        }
        this.scroll = 0;
    }

    /**
     * 渲染文本视口：折行文字 + 底部淡出 + 右侧滑条。
     *
     * @param x, y, w, h 文本视口（屏幕坐标，滑条占据右侧 BAR_WIDTH）
     * @param fadeH      底部淡出高度（含在 h 内）
     * @param fadeColor  淡出遮盖色（与所在面板底色一致）
     */
    public void render(GuiGraphics graphics, Font font, int x, int y, int w, int h, int fadeH, int fadeColor) {
        int total = this.rows.size() * ROW_HEIGHT;
        int viewH = h - fadeH;
        this.maxScroll = Math.max(0, total - viewH);
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll));
        for (int i = 0; i < this.rows.size(); i++) {
            int rowY = y + i * ROW_HEIGHT - this.scroll;
            if (rowY >= y - ROW_HEIGHT && rowY < y + h) {
                graphics.drawString(font, this.rows.get(i), x, rowY, NoteText.BASE_COLOR);
            }
        }
        // 底部淡出：以面板底色渐变条纹遮盖靠近边缘的文字。
        for (int i = 0; i < fadeH; i++) {
            int a = (int) Math.round(255.0 * (i + 1) / fadeH);
            graphics.fill(x, y + viewH - 6 + i, x + w - BAR_WIDTH - 4, y + viewH - 6 + i + 1, (a << 24) | fadeColor);
        }
        // 滑条。
        this.barX = x + w - BAR_WIDTH;
        this.trackY = y;
        this.trackH = h;
        graphics.fill(this.barX, this.trackY, this.barX + BAR_WIDTH, this.trackY + this.trackH, 0xFF0E1720);
        graphics.fill(this.barX, this.trackY, this.barX + BAR_WIDTH, this.trackY + 1, 0xFF3B526F);
        graphics.fill(this.barX, this.trackY + this.trackH - 1, this.barX + BAR_WIDTH, this.trackY + this.trackH, 0xFF3B526F);
        graphics.fill(this.barX, this.trackY, this.barX + 1, this.trackY + this.trackH, 0xFF3B526F);
        graphics.fill(this.barX + BAR_WIDTH - 1, this.trackY, this.barX + BAR_WIDTH, this.trackY + this.trackH, 0xFF3B526F);
        if (this.maxScroll > 0) {
            this.thumbH = Math.max(10, Math.round(this.trackH * (float) viewH / total));
            int thumbY = this.trackY + Math.round((this.trackH - this.thumbH) * (this.scroll / (float) this.maxScroll));
            graphics.fill(this.barX, thumbY, this.barX + BAR_WIDTH, thumbY + this.thumbH, 0xFF3B526F);
        } else {
            this.thumbH = this.trackH;
        }
    }

    /** 鼠标滚轮滚动；返回 true 表示已处理。 */
    public boolean mouseScrolled(double delta) {
        if (this.maxScroll <= 0) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll + (delta < 0 ? 12 : -12)));
        return true;
    }

    /** 点击滑条轨道：跳转到对应位置，并进入拖拽状态。 */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (this.maxScroll <= 0) {
            return false;
        }
        if (mouseX >= this.barX && mouseX < this.barX + BAR_WIDTH
                && mouseY >= this.trackY && mouseY < this.trackY + this.trackH) {
            int track = this.trackH - this.thumbH;
            if (track > 0) {
                this.scroll = Math.round((float) (mouseY - this.trackY - this.thumbH / 2.0) / track * this.maxScroll);
            }
            this.dragging = true;
            return true;
        }
        return false;
    }

    /** 滑块拖拽。 */
    public boolean mouseDragged(double mouseY) {
        if (!this.dragging || this.maxScroll <= 0) {
            return false;
        }
        int track = this.trackH - this.thumbH;
        if (track > 0) {
            this.scroll = Math.round((float) (mouseY - this.trackY - this.thumbH / 2.0) / track * this.maxScroll);
        }
        return true;
    }

    /** 松开鼠标，结束拖拽。 */
    public void mouseReleased() {
        this.dragging = false;
    }
}
