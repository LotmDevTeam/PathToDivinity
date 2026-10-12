package net.swimmingtuna.pathtodivinity.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A button drawn in {@link PTDGuiStyle}'s idiom instead of vanilla's stone one: a dark plate inside a thin
 * accent border, lit when the mouse is over it and shuttered when it cannot be pressed.
 *
 * <p>Beyond looks it carries two things the profile screen has no other way to show. {@link #setMarked} puts
 * a sigil beside the profile the player is living in, and {@link #setProgress} drains a bar along the bottom
 * edge for the confirm window, which until now ran its three seconds entirely out of sight.
 */
@OnlyIn(Dist.CLIENT)
public class MysteryButton extends Button {

    /**
     * Room kept clear at each edge of a marked button's label. The sigil is centred on {@code left + 6} and
     * is five pixels across, so this clears it with a little air to spare.
     */
    private static final int SIGIL_INSET = 12;

    private int accent = PTDGuiStyle.ACCENT;
    private float progress;
    private boolean marked;

    public MysteryButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /** The border colour, so a row can be tinted by the thing it stands for. */
    public void setAccent(int accent) {
        this.accent = accent;
    }

    /** How much of the confirm window is left, 0 to 1. Zero draws no bar at all. */
    public void setProgress(float progress) {
        this.progress = Math.max(0.0F, Math.min(1.0F, progress));
    }

    /** Whether to draw the sigil that marks this as the profile currently being lived in. */
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + this.width;
        int bottom = top + this.height;
        boolean lit = this.active && isHoveredOrFocused();

        if (lit) {
            // A one-pixel halo outside the border. Cheap, and it lifts the row off the panel.
            guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1,
                    PTDGuiStyle.withAlpha(this.accent, 0.25F));
        }

        guiGraphics.fill(left, top, right, bottom, plateColor(lit));

        int border = borderColor(lit);
        guiGraphics.fill(left, top, right, top + 1, border);
        guiGraphics.fill(left, bottom - 1, right, bottom, border);
        guiGraphics.fill(left, top, left + 1, bottom, border);
        guiGraphics.fill(right - 1, top, right, bottom, border);

        if (lit) {
            drawCarets(guiGraphics, left, right, top + this.height / 2, border);
        }
        if (this.marked) {
            drawSigil(guiGraphics, left + 6, top + this.height / 2);
        }
        if (this.progress > 0.0F) {
            int width = (int) ((this.width - 2) * this.progress);
            guiGraphics.fill(left + 1, bottom - 3, left + 1 + width, bottom - 1, this.accent);
        }

        drawLabel(guiGraphics, left, top, right, bottom);
    }

    private int plateColor(boolean lit) {
        if (!this.active) {
            return 0x90141414;
        }
        return lit ? 0xB0343446 : PTDGuiStyle.SUB_PANEL;
    }

    private int borderColor(boolean lit) {
        if (!this.active) {
            return PTDGuiStyle.withAlpha(this.accent, 0.35F);
        }
        return lit ? this.accent : PTDGuiStyle.withAlpha(this.accent, 0.7F);
    }

    /** Small arrowheads pointing in from either edge, marking the row the mouse has landed on. */
    private void drawCarets(GuiGraphics guiGraphics, int left, int right, int centerY, int color) {
        for (int i = 0; i < 3; i++) {
            guiGraphics.fill(left + 3 + i, centerY - 2 + i, left + 4 + i, centerY + 3 - i, color);
            guiGraphics.fill(right - 4 - i, centerY - 2 + i, right - 3 - i, centerY + 3 - i, color);
        }
    }

    /** A filled diamond: this is the profile you are living in. */
    private void drawSigil(GuiGraphics guiGraphics, int centerX, int centerY) {
        guiGraphics.fill(centerX, centerY - 3, centerX + 1, centerY + 4, PTDGuiStyle.ACCENT);
        guiGraphics.fill(centerX - 1, centerY - 2, centerX + 2, centerY + 3, PTDGuiStyle.ACCENT);
        guiGraphics.fill(centerX - 2, centerY - 1, centerX + 3, centerY + 2, PTDGuiStyle.ACCENT);
    }

    /**
     * The label, centred in whatever room the ornaments have left it and clipped to that.
     *
     * <p>The sigil is drawn inside the plate, so the label cannot have the whole width: centring across it
     * regardless would run a long label straight through the diamond. The inset is taken off both edges
     * rather than just the left so the text stays centred in the button rather than sitting off to one side.
     *
     * <p>The clip is not decoration either — these labels carry LOTM sequence titles, and the long ones
     * would otherwise run out through the border and across the frame.
     */
    private void drawLabel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        Font font = Minecraft.getInstance().font;
        Component message = getMessage();
        int color = this.active ? PTDGuiStyle.TEXT : PTDGuiStyle.TEXT_MUTED;
        int textY = top + (this.height - font.lineHeight) / 2 + 1;

        int inset = this.marked ? SIGIL_INSET : 1;
        int labelLeft = left + inset;
        int labelRight = right - inset;

        guiGraphics.enableScissor(labelLeft, top + 1, labelRight, bottom - 1);
        guiGraphics.drawString(font, message,
                (labelLeft + labelRight) / 2 - font.width(message) / 2, textY, color, true);
        guiGraphics.disableScissor();
    }
}
