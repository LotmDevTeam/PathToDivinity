package net.swimmingtuna.pathtodivinity.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * The look this mod's screens are drawn in: LOTM's own panel style, with a few flourishes of its own.
 *
 * <p>The first five colours below are lifted from the LOTM mod's screens rather than invented, so a screen of
 * ours sits next to the pathway and sequence menus without announcing that it came from somewhere else.
 * {@code PathwayBaseScreen} fills its panel with {@link #PANEL_BODY} inside a two-pixel border, insets its
 * description box in {@link #SUB_PANEL}, and rules it off with {@link #DIVIDER}; {@code FixedScaleScreen}
 * washes the whole screen in the {@link #BACKDROP_TOP}–{@link #BACKDROP_BOTTOM} gradient first.
 *
 * <p>Everything here draws with plain rectangles. No textures and no glyphs, so a resource pack cannot
 * replace the frame with something else and no ornament can come out as a missing-character box.
 */
@OnlyIn(Dist.CLIENT)
public final class PTDGuiStyle {

    /** Top of the wash the LOTM screens lay over the world behind them. */
    public static final int BACKDROP_TOP = 0xC0101010;

    /** Bottom of that wash — very slightly more opaque, so the screen settles downward. */
    public static final int BACKDROP_BOTTOM = 0xD0101010;

    /** The body of a panel. */
    public static final int PANEL_BODY = 0xD0101010;

    /** A box inset within a panel, a shade lighter so it reads as a layer above it. */
    public static final int SUB_PANEL = 0x80202020;

    /** LOTM's rule colour, used here as the midpoint of a rule that fades out towards both ends. */
    public static final int DIVIDER = 0xFF444444;

    /** Antique gold. The frame, the ornaments and the title. */
    public static final int ACCENT = 0xFFC8A24A;

    /** The same gold with the light off: borders of things that cannot be clicked. */
    public static final int ACCENT_DIM = 0xFF6B5426;

    /** Body text — pale enough to carry on near-black, cool enough not to compete with the gold. */
    public static final int TEXT = 0xFFC9C2D8;

    public static final int TEXT_MUTED = 0xFF8A82A0;

    /** Refusals: why a switch will not happen. Lifted off pure red so it stays legible on black. */
    public static final int REFUSAL = 0xFFE0503C;

    /** A switch that went through. */
    public static final int SUCCESS = 0xFF6FD08C;

    public static final int STAR = 0xFFD8D2E8;

    /** Thickness of a panel border, matching LOTM's. */
    private static final int BORDER = 2;

    private static final int STAR_COUNT = 60;

    /**
     * Star positions as fractions of the panel, so they scale with it and — being fixed at class load — do
     * not crawl about from one frame to the next. The seed is arbitrary but must stay constant.
     */
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];
    private static final float[] STAR_SPEED = new float[STAR_COUNT];
    private static final float[] STAR_PEAK = new float[STAR_COUNT];

    static {
        Random random = new Random(0x50544431L);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = random.nextFloat();
            // Squared, which crowds the stars towards the top of the panel and leaves the busier lower half
            // — buttons, blockers, status lines — comparatively clear.
            STAR_Y[i] = random.nextFloat() * random.nextFloat();
            STAR_PHASE[i] = random.nextFloat() * ((float) Math.PI * 2.0F);
            STAR_SPEED[i] = 0.02F + random.nextFloat() * 0.05F;
            STAR_PEAK[i] = 0.12F + random.nextFloat() * 0.33F;
        }
    }

    private PTDGuiStyle() {
    }

    /** The wash LOTM lays over the world behind a screen. Replaces {@code Screen.renderBackground}. */
    public static void backdrop(GuiGraphics guiGraphics, int width, int height) {
        guiGraphics.fillGradient(0, 0, width, height, BACKDROP_TOP, BACKDROP_BOTTOM);
    }

    /**
     * A panel: body plus the two-pixel border on all four sides, laid out exactly as LOTM lays out its own.
     * Bounds are exclusive at the right and bottom, as {@code GuiGraphics.fill} takes them.
     */
    public static void panel(GuiGraphics guiGraphics, int left, int top, int right, int bottom,
                             int borderColor) {
        guiGraphics.fill(left, top, right, bottom, PANEL_BODY);
        guiGraphics.fill(left, top, right, top + BORDER, borderColor);
        guiGraphics.fill(left, bottom - BORDER, right, bottom, borderColor);
        guiGraphics.fill(left, top, left + BORDER, bottom, borderColor);
        guiGraphics.fill(right - BORDER, top, right, bottom, borderColor);
    }

    /** A box inset within a panel. */
    public static void subPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, SUB_PANEL);
    }

    /**
     * A horizontal rule that fades out towards both ends rather than stopping dead, optionally with a small
     * diamond set into the middle of it.
     */
    public static void divider(GuiGraphics guiGraphics, int left, int right, int y, int color,
                               boolean diamond) {
        int width = right - left;
        if (width <= 0) {
            return;
        }
        int centerX = left + width / 2;
        for (int x = left; x < right; x++) {
            // 1 at the centre, 0 at either end.
            float distance = Math.abs(x - centerX) / (width / 2.0F);
            float strength = 1.0F - distance * distance;
            if (strength > 0.0F) {
                guiGraphics.fill(x, y, x + 1, y + 1, withAlpha(color, strength));
            }
        }
        if (diamond) {
            guiGraphics.fill(centerX - 1, y - 2, centerX + 1, y - 1, color);
            guiGraphics.fill(centerX - 2, y - 1, centerX + 2, y + 2, color);
            guiGraphics.fill(centerX - 1, y + 2, centerX + 1, y + 3, color);
        }
    }

    /** Short brackets tucked inside each corner of a panel, just inside its border. */
    public static void ornamentCorners(GuiGraphics guiGraphics, int left, int top, int right, int bottom,
                                       int color) {
        int inset = BORDER + 1;
        int length = 8;
        int l = left + inset;
        int t = top + inset;
        int r = right - inset;
        int b = bottom - inset;

        guiGraphics.fill(l, t, l + length, t + 1, color);
        guiGraphics.fill(l, t, l + 1, t + length, color);

        guiGraphics.fill(r - length, t, r, t + 1, color);
        guiGraphics.fill(r - 1, t, r, t + length, color);

        guiGraphics.fill(l, b - 1, l + length, b, color);
        guiGraphics.fill(l, b - length, l + 1, b, color);

        guiGraphics.fill(r - length, b - 1, r, b, color);
        guiGraphics.fill(r - 1, b - length, r, b, color);
    }

    /**
     * The stars above the gray fog: a scatter of faint points that breathe in and out of view.
     *
     * <p>Every star is kept dim on purpose. This is the ground that text is read off, so the field has to
     * suggest depth without ever competing with a sequence title drawn over it.
     *
     * @param time client ticks plus the partial tick, so the twinkle runs smoothly between ticks
     */
    public static void starfield(GuiGraphics guiGraphics, int left, int top, int right, int bottom,
                                 float time) {
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) {
            return;
        }
        for (int i = 0; i < STAR_COUNT; i++) {
            // Sine gives the slow fade in and out; halving and offsetting keeps it within 0..1.
            float pulse = (float) Math.sin(time * STAR_SPEED[i] + STAR_PHASE[i]) * 0.5F + 0.5F;
            float alpha = STAR_PEAK[i] * pulse;
            if (alpha < 0.03F) {
                continue;
            }
            int x = left + (int) (STAR_X[i] * width);
            int y = top + (int) (STAR_Y[i] * height);
            guiGraphics.fill(x, y, x + 1, y + 1, withAlpha(STAR, alpha));

            // The brightest few grow arms at the top of their pulse, which reads as a star rather than dust.
            if (STAR_PEAK[i] > 0.4F && pulse > 0.85F) {
                int armColor = withAlpha(STAR, alpha * 0.45F);
                guiGraphics.fill(x - 1, y, x, y + 1, armColor);
                guiGraphics.fill(x + 1, y, x + 2, y + 1, armColor);
                guiGraphics.fill(x, y - 1, x + 1, y, armColor);
                guiGraphics.fill(x, y + 1, x + 1, y + 2, armColor);
            }
        }
    }

    /** The colour with its alpha scaled by {@code factor}, clamped to a drawable range. */
    public static int withAlpha(int argb, float factor) {
        int alpha = (int) ((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, factor)));
        return (Math.min(255, alpha) << 24) | (argb & 0x00FFFFFF);
    }
}
