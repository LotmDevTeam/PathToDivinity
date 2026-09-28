package net.swimmingtuna.pathtodivinity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.client.PTDClientScreenHandlers;
import net.swimmingtuna.pathtodivinity.profile.PlayerProfileData;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import net.swimmingtuna.pathtodivinity.profile.ProfileSnapshot;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Everything the profile screen draws, assembled server-side.
 *
 * <p>The client is told what is true rather than working it out: the profile store is server-only, and the
 * config values below come from the server's file — Forge's COMMON config is loaded per side, so a client
 * reading its own copy would show the wrong rules on someone else's server.
 *
 * <p>Sent both to open the screen and to refresh it after an attempted switch, with {@link #status} carrying
 * the outcome.
 */
public class ProfileScreenS2CPacket {

    /** Marks "this player has not chosen a profile yet" on the wire, where {@code null} cannot be written. */
    private static final byte NO_PROFILE = -1;

    @Nullable
    private final ProfileType active;
    private final String activeSummary;
    private final String otherSummary;
    @Nullable
    private final Component blocker;
    @Nullable
    private final Component status;
    private final boolean statusSuccess;
    private final boolean normalRegresses;
    private final int safemodeMaxSequence;
    private final double safemodeAbilityDamage;
    private final int cooldownMinutes;

    public ProfileScreenS2CPacket(@Nullable ProfileType active, String activeSummary, String otherSummary,
                                  @Nullable Component blocker, @Nullable Component status, boolean statusSuccess,
                                  boolean normalRegresses, int safemodeMaxSequence, double safemodeAbilityDamage,
                                  int cooldownMinutes) {
        this.active = active;
        this.activeSummary = activeSummary;
        this.otherSummary = otherSummary;
        this.blocker = blocker;
        this.status = status;
        this.statusSuccess = statusSuccess;
        this.normalRegresses = normalRegresses;
        this.safemodeMaxSequence = safemodeMaxSequence;
        this.safemodeAbilityDamage = safemodeAbilityDamage;
        this.cooldownMinutes = cooldownMinutes;
    }

    /**
     * Builds the current picture for one player.
     *
     * @param status        the outcome of the action that triggered this refresh, or {@code null} when opening
     * @param statusSuccess whether that outcome was a completed switch, which decides how it is coloured
     */
    public static ProfileScreenS2CPacket forPlayer(ServerPlayer player, @Nullable Component status,
                                                  boolean statusSuccess) {
        MinecraftServer server = player.getServer();
        ProfileType active = ProfileManager.getActiveProfile(player);

        String activeSummary = ProfileManager.describeLiveBeyonder(player);
        String otherSummary = "never used";
        if (server != null && active != null) {
            otherSummary = ProfileSnapshot.describe(
                    PlayerProfileData.get(server).getSnapshot(player.getUUID(), active.other()));
        }

        // Only a player who already has a profile can be blocked; a first-time choice swaps nothing.
        Component blocker = active == null ? null : ProfileManager.getSwitchBlocker(player);

        return new ProfileScreenS2CPacket(active, activeSummary, otherSummary, blocker, status, statusSuccess,
                PTDServerConfig.NORMAL_PROFILE_REGRESSES.get(),
                PTDServerConfig.SAFEMODE_MAX_SEQUENCE.get(),
                PTDServerConfig.SAFEMODE_ABILITY_DAMAGE.get(),
                PTDServerConfig.PROFILE_SWITCH_COOLDOWN_MINUTES.get());
    }

    public ProfileScreenS2CPacket(FriendlyByteBuf buffer) {
        byte activeId = buffer.readByte();
        this.active = activeId == NO_PROFILE ? null : ProfileType.values()[activeId];
        this.activeSummary = buffer.readUtf();
        this.otherSummary = buffer.readUtf();
        this.blocker = buffer.readBoolean() ? buffer.readComponent() : null;
        this.status = buffer.readBoolean() ? buffer.readComponent() : null;
        this.statusSuccess = buffer.readBoolean();
        this.normalRegresses = buffer.readBoolean();
        this.safemodeMaxSequence = buffer.readVarInt();
        this.safemodeAbilityDamage = buffer.readDouble();
        this.cooldownMinutes = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(this.active == null ? NO_PROFILE : (byte) this.active.ordinal());
        buffer.writeUtf(this.activeSummary);
        buffer.writeUtf(this.otherSummary);
        buffer.writeBoolean(this.blocker != null);
        if (this.blocker != null) {
            buffer.writeComponent(this.blocker);
        }
        buffer.writeBoolean(this.status != null);
        if (this.status != null) {
            buffer.writeComponent(this.status);
        }
        buffer.writeBoolean(this.statusSuccess);
        buffer.writeBoolean(this.normalRegresses);
        buffer.writeVarInt(this.safemodeMaxSequence);
        buffer.writeDouble(this.safemodeAbilityDamage);
        buffer.writeVarInt(this.cooldownMinutes);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PTDClientScreenHandlers.openProfileScreen(this)));
        ctx.setPacketHandled(true);
    }

    @Nullable
    public ProfileType getActive() {
        return this.active;
    }

    /** The live profile's Beyonder state; when no profile is chosen, simply the player's current state. */
    public String getActiveSummary() {
        return this.activeSummary;
    }

    /** The stored profile's Beyonder state, or why there is none to describe. */
    public String getOtherSummary() {
        return this.otherSummary;
    }

    @Nullable
    public Component getBlocker() {
        return this.blocker;
    }

    @Nullable
    public Component getStatus() {
        return this.status;
    }

    /** Whether {@link #getStatus()} reports a completed switch rather than a refusal. */
    public boolean isStatusSuccess() {
        return this.statusSuccess;
    }

    /** Whether the Normal profile loses a sequence to a player kill on this server. */
    public boolean normalRegresses() {
        return this.normalRegresses;
    }

    public int getSafemodeMaxSequence() {
        return this.safemodeMaxSequence;
    }

    public double getSafemodeAbilityDamage() {
        return this.safemodeAbilityDamage;
    }

    public int getCooldownMinutes() {
        return this.cooldownMinutes;
    }
}
