package cn.blockforge.generated.generatedmod.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.EditBox;

/**
 * 可复用的「文本框输入保护」：当搜索框/数量框等文本框处于编辑状态时，
 * 吞掉会误触发游戏行为的快捷键，保证玩家能正常输入字母与数字。
 *
 * <p>典型问题：原版 {@code AbstractContainerScreen#keyPressed} 在子控件未消费按键时，
 * 会用**背包键（默认 E）关闭界面**；而 {@code EditBox#keyPressed} 对字母键返回 false
 * （字母通过 {@code charTyped} 写入），于是"在搜索框里打 e"会直接关掉界面。
 * 同理数字键（快捷栏交换）、Q（丢弃）、F（副手交换）、Tab（玩家列表）等也会误触发。</p>
 *
 * <p>做法：在界面的 {@code keyPressed} 开头调用本工具——被吞掉的按键不会再传给
 * {@code super}，而字母/数字仍会经 {@code charTyped} 正常进入文本框（两条链路互不影响）。</p>
 *
 * <pre>
 *   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
 *       if (TextInputGuard.consumeHotkeysWhileEditing(keyCode, scanCode, this.searchBox, this.amountBox)) {
 *           return true;
 *       }
 *       return super.keyPressed(keyCode, scanCode, modifiers);
 *   }
 * </pre>
 */
public final class TextInputGuard {

    private TextInputGuard() {
    }

    /** 是否有任一文本框正在编辑。 */
    public static boolean isEditing(EditBox... boxes) {
        if (boxes == null) {
            return false;
        }
        for (EditBox box : boxes) {
            if (box != null && box.isFocused()) {
                return true;
            }
        }
        return false;
    }

    /** 编辑状态下吞掉游戏快捷键（便捷重载：调用方自行判断编辑状态）。 */
    public static boolean consumeHotkeysWhileEditing(int keyCode, int scanCode, boolean editing) {
        if (!editing) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return false;
        }
        Options options = minecraft.options;
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        // 关闭界面 / 背包键（E）、丢弃（Q）、副手交换（F）、玩家列表（Tab）、聊天（T）、命令（/）
        if (matches(options.keyInventory, key)
                || matches(options.keyDrop, key)
                || matches(options.keySwapOffhand, key)
                || matches(options.keyPlayerList, key)
                || matches(options.keyChat, key)
                || matches(options.keyCommand, key)) {
            return true;
        }
        // 快捷栏 1~9：避免输入数字时与悬停格子发生交换
        for (KeyMapping hotbar : options.keyHotbarSlots) {
            if (matches(hotbar, key)) {
                return true;
            }
        }
        return false;
    }

    /** 编辑状态下吞掉游戏快捷键。 */
    public static boolean consumeHotkeysWhileEditing(int keyCode, int scanCode, EditBox... boxes) {
        return consumeHotkeysWhileEditing(keyCode, scanCode, isEditing(boxes));
    }

    private static boolean matches(KeyMapping mapping, InputConstants.Key key) {
        return mapping != null && mapping.isActiveAndMatches(key);
    }
}
