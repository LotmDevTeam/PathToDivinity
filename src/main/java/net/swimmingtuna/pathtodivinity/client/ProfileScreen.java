package net.swimmingtuna.pathtodivinity.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.swimmingtuna.pathtodivinity.network.PTDNetwork;
import net.swimmingtuna.pathtodivinity.network.ProfileActionC2SPacket;
import net.swimmingtuna.pathtodivinity.network.ProfileScreenS2CPacket;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The profile screen: what each profile is, which one is live, and what switching would cost.
 *
 * <p>Drawn in {@link PTDGuiStyle}, which is LOTM's own panel style, so this sits alongside the pathway and
 * sequence menus the player reaches from the same place rather than looking like a vanilla dialog that
 * wandered in. It holds no authority of its own — every value shown arrives in a
 * {@link ProfileScreenS2CPacket} and every click goes back to the server to be judged there.
 */
@OnlyIn(Dist.CLIENT)
public class ProfileScreen extends Screen {

    private static final int PANEL_WIDTH = 264;
    private static final int BUTTON_WIDTH = 216;
    private static final int BUTTON_HEIGHT = 24;
    private static final int DONE_WIDTH = 120;
    private static final int DONE_HEIGHT = 20;

    // Everything below is measured down from the top of the panel.
    private static final int TITLE_Y = 14;
    private static final int TITLE_RULE_Y = 27;
    private static final int HEADING_Y = 34;
    private static final int BUTTON_STEP = 28;

    /** Gap below the rule that closes the button list, and again above the Done button. */
    private static final int BLOCK_GAP = 8;

    /** Clear space left under the Done button before the frame. */
    private static final int BOTTOM_MARGIN = 10;

    /** Height of one line of wrapped text, matching the step {@link #drawWrapped} advances by. */
    private static final int LINE_HEIGHT = 10;

    /** Height of the one-line hint that stands in for the blocker when nothing is blocking a switch. */
    private static final int HINT_HEIGHT = 12;

    /** How wide text may run before it wraps: the panel less a margin either side. */
    private static final int TEXT_WIDTH = PANEL_WIDTH - 32;

    private static final int CONFIRM_TICKS = 60;

    private ProfileScreenS2CPacket state;
    private final Map<ProfileType, MysteryButton> buttons = new EnumMap<>(ProfileType.class);

    @Nullable
    private ProfileType pendingConfirm;
    private int confirmTicksLeft;

    /** Drives the starfield. Counts up for as long as the screen is open and is never read for logic. */
    private int ticks;

    private int left;
    private int top;

    // Measured in init() rather than fixed. Headings, refusals and outcomes are all server-written prose of
    // unknown length, and the worst realistic case — a three-line blocker above a three-line failure — is
    // half again as tall as the common one. Sizing the panel to what it actually has to say beats picking a
    // fixed height that is either too short to hold that case or too empty in every other.
    private int panelHeight;
    private int buttonsTop;
    private int lowerRuleY;
    private int messageY;
    private int doneY;

    public ProfileScreen(ProfileScreenS2CPacket state) {
        super(Component.literal("Beyonder Profiles"));
        this.state = state;
    }

    public void refresh(ProfileScreenS2CPacket newState) {
        this.state = newState;
        this.pendingConfirm = null;
        this.confirmTicksLeft = 0;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        this.buttons.clear();
        layOut();

        int x = this.left + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int y = this.buttonsTop;
        for (ProfileType type : ProfileType.values()) {
            MysteryButton button = new MysteryButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, labelFor(type),
                    press -> onProfileClicked(type));
            button.setTooltip(Tooltip.create(rulesFor(type)));
            button.setAccent(accentFor(type));
            // The live profile is not somewhere you can go, and nobody may switch while blocked.
            button.active = type != this.state.getActive() && this.state.getBlocker() == null;
            this.buttons.put(type, button);
            addRenderableWidget(button);
            y += BUTTON_STEP;
        }

        MysteryButton done = new MysteryButton(this.left + (PANEL_WIDTH - DONE_WIDTH) / 2,
                this.doneY, DONE_WIDTH, DONE_HEIGHT, CommonComponents.GUI_DONE, press -> onClose());
        addRenderableWidget(done);
    }

    /**
     * Sizes the panel to the text this particular state has to show, then places everything inside it.
     *
     * <p>Measured top-down as a run of blocks, each starting where the last one ended, so nothing can be
     * written over anything else however long the server's prose turns out to be.
     */
    private void layOut() {
        // Two lines is what the heading takes in almost every case; the floor keeps the panel a steady size
        // there rather than letting it breathe in and out as the player's sequence title changes length.
        int headingLines = Math.max(2, this.font.split(heading(), TEXT_WIDTH).size());
        int headingBlock = HEADING_Y + headingLines * LINE_HEIGHT + 4;
        int buttonsBlock = ProfileType.values().length * BUTTON_STEP + 6;
        int messageBlock = this.state.getBlocker() != null
                ? this.font.split(this.state.getBlocker(), TEXT_WIDTH).size() * LINE_HEIGHT
                : HINT_HEIGHT;
        if (this.state.getStatus() != null) {
            messageBlock += 2 + this.font.split(this.state.getStatus(), TEXT_WIDTH).size() * LINE_HEIGHT;
        }

        this.panelHeight = headingBlock + buttonsBlock + BLOCK_GAP + messageBlock
                + BLOCK_GAP + DONE_HEIGHT + BOTTOM_MARGIN;

        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.buttonsTop = this.top + headingBlock;
        this.lowerRuleY = this.buttonsTop + buttonsBlock;
        this.messageY = this.lowerRuleY + BLOCK_GAP;
        this.doneY = this.top + this.panelHeight - BOTTOM_MARGIN - DONE_HEIGHT;
    }

    /** A profile's own colour, falling back to the frame's gold for any formatting that carries none. */
    private static int accentFor(ProfileType type) {
        Integer color = type.getColor().getColor();
        return color == null ? PTDGuiStyle.ACCENT : 0xFF000000 | color;
    }

    private void onProfileClicked(ProfileType type) {
        // A first-ever choice swaps nothing and costs nothing, so it does not need guarding behind a confirm.
        if (this.state.getActive() == null) {
            PTDNetwork.sendToServer(new ProfileActionC2SPacket(type));
            return;
        }
        if (this.pendingConfirm != type) {
            this.pendingConfirm = type;
            this.confirmTicksLeft = CONFIRM_TICKS;
            updateLabels();
            return;
        }
        this.pendingConfirm = null;
        this.confirmTicksLeft = 0;
        updateLabels();
        PTDNetwork.sendToServer(new ProfileActionC2SPacket(type));
    }

    @Override
    public void tick() {
        // Counted first: the starfield must keep moving whether or not a confirm is pending.
        this.ticks++;
        if (this.pendingConfirm == null) {
            return;
        }
        if (--this.confirmTicksLeft <= 0) {
            this.pendingConfirm = null;
            updateLabels();
        }
    }

    private void updateLabels() {
        this.buttons.forEach((type, button) -> button.setMessage(labelFor(type)));
    }

    private Component labelFor(ProfileType type) {
        if (type == this.pendingConfirm) {
            return Component.literal("Click again to confirm").withStyle(ChatFormatting.YELLOW);
        }
        MutableComponent name = Component.literal(type.getDisplayName()).withStyle(type.getColor());
        if (type == this.state.getActive()) {
            return name.copy().append(Component.literal(" (" + this.state.getActiveSummary() + ")"));
        }
        if (this.state.getActive() == null) {
            return Component.literal("Choose ").append(name);
        }
        return Component.literal("Switch to ").append(name)
                .append(Component.literal(" — " + this.state.getOtherSummary()));
    }

    /**
     * The line under the title, naming the profile being lived in and the Beyonder in it.
     *
     * <p>Measured in {@link #init()} as well as drawn, because sequence titles are LOTM's and some of them
     * are long enough to wrap this onto a third line, which the layout below has to make room for.
     */
    private Component heading() {
        if (this.state.getActive() == null) {
            return Component.literal("You have not chosen a profile yet.");
        }
        return Component.literal("Current profile: ")
                .append(Component.literal(this.state.getActive().getDisplayName())
                        .withStyle(this.state.getActive().getColor()))
                .append(Component.literal(" (" + this.state.getActiveSummary() + ")"));
    }

    /** The rules that profile plays by, read off the values the server sent. */
    private Component rulesFor(ProfileType type) {
        // An unstyled root, with the heading as its first child rather than the root itself: appended
        // children inherit the root's style, and every rule line below would otherwise come out bold.
        MutableComponent tooltip = Component.empty().append(Component.literal(type.getDisplayName())
                .withStyle(type.getColor(), ChatFormatting.BOLD));
        if (type == ProfileType.NORMAL) {
            tooltip.append(line("Full progression — can reach Sequence 0."));
            if (this.state.normalRegresses()) {
                tooltip.append(line("Loses a sequence when another player kills you."));
                tooltip.append(line("That sequence drops as a characteristic your killer can take."));
            } else {
                tooltip.append(line("Keeps its sequence when another player kills you."));
            }
        } else {
            tooltip.append(line("Capped at Sequence ")
                    .append(number(String.valueOf(this.state.getSafemodeMaxSequence())))
                    .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
            tooltip.append(line("Never loses a sequence to a player kill, and drops no characteristic."));
            tooltip.append(line("Ability damage against players is halved"));
            tooltip.append(line("Never occupies a limited Sequence 0/1/2 slot."));
        }
        tooltip.append(line("Profiles keep separate progress, inventory, curios and experience."));
        tooltip.append(line("Switching returns you to the world spawn."));
        if (this.state.getCooldownMinutes() > 0) {
            tooltip.append(line("Switching is limited to once every ")
                    .append(number(String.valueOf(this.state.getCooldownMinutes())))
                    .append(Component.literal(" minutes.").withStyle(ChatFormatting.GRAY)));
        }
        return tooltip;
    }

    private static MutableComponent line(String text) {
        return Component.literal("\n· " + text).withStyle(ChatFormatting.GRAY);
    }

    /** A figure the server decided, picked out of the surrounding grey so it can be found at a glance. */
    private static MutableComponent number(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int right = this.left + PANEL_WIDTH;
        int bottom = this.top + this.panelHeight;

        PTDGuiStyle.backdrop(guiGraphics, this.width, this.height);
        PTDGuiStyle.panel(guiGraphics, this.left, this.top, right, bottom, PTDGuiStyle.ACCENT);
        PTDGuiStyle.starfield(guiGraphics, this.left + 3, this.top + 3, right - 3, bottom - 3,
                this.ticks + partialTick);
        PTDGuiStyle.ornamentCorners(guiGraphics, this.left, this.top, right, bottom,
                PTDGuiStyle.withAlpha(PTDGuiStyle.ACCENT, 0.55F));

        drawCentered(guiGraphics, this.title.copy().withStyle(ChatFormatting.BOLD),
                this.top + TITLE_Y, PTDGuiStyle.ACCENT);
        PTDGuiStyle.divider(guiGraphics, this.left + 12, right - 12, this.top + TITLE_RULE_Y,
                PTDGuiStyle.ACCENT, true);

        drawWrapped(guiGraphics, heading(), this.top + HEADING_Y, PTDGuiStyle.TEXT);

        PTDGuiStyle.divider(guiGraphics, this.left + 20, right - 20, this.lowerRuleY,
                PTDGuiStyle.DIVIDER, false);

        int y = this.messageY;
        if (this.state.getBlocker() != null) {
            y = drawWrapped(guiGraphics, this.state.getBlocker(), y, PTDGuiStyle.REFUSAL);
        } else {
            drawCentered(guiGraphics, Component.literal("Hover a profile for the rules it plays by."), y,
                    PTDGuiStyle.TEXT_MUTED);
            y += HINT_HEIGHT;
        }
        if (this.state.getStatus() != null) {
            drawWrapped(guiGraphics, this.state.getStatus(), y + 2,
                    this.state.isStatusSuccess() ? PTDGuiStyle.SUCCESS : PTDGuiStyle.REFUSAL);
        }

        syncButtons();
        // Last, so the widgets and their tooltips sit above everything drawn above.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Pushes this frame's state onto the buttons: the sigil onto the live profile, and the draining confirm
     * bar onto whichever row is waiting on a second click. Done here rather than plumbed through every place
     * that changes the state, which keeps the two from ever falling out of step.
     */
    private void syncButtons() {
        this.buttons.forEach((type, button) -> {
            button.setMarked(type == this.state.getActive());
            button.setProgress(type == this.pendingConfirm
                    ? this.confirmTicksLeft / (float) CONFIRM_TICKS
                    : 0.0F);
        });
    }

    /**
     * Centred text with a drop shadow, which is what carries pale text over the panel's near-black and the
     * faint stars drifting across it.
     */
    private void drawCentered(GuiGraphics guiGraphics, Component text, int y, int color) {
        guiGraphics.drawString(this.font, text,
                this.left + PANEL_WIDTH / 2 - this.font.width(text) / 2, y, color, true);
    }

    private int drawWrapped(GuiGraphics guiGraphics, Component text, int y, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, TEXT_WIDTH);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(this.font, line,
                    this.left + PANEL_WIDTH / 2 - this.font.width(line) / 2, y, color, true);
            y += LINE_HEIGHT;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        // On a server the world never pauses anyway; pretending otherwise in single-player just hides the
        // cooldown ticking down.
        return false;
    }
}
