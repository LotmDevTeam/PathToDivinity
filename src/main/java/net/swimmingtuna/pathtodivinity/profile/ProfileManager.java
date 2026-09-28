package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.client.Configs;
import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.combat.CombatTag;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.network.PTDNetwork;
import net.swimmingtuna.pathtodivinity.network.ProfileScreenS2CPacket;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class ProfileManager {

    private static final long TICKS_PER_MINUTE = 1200L;

    private ProfileManager() {
    }

    public static boolean isEnabled() {
        return PTDServerConfig.PROFILES_ENABLED.get();
    }

    @Nullable
    public static ProfileType getActiveProfile(Player player) {
        // Server first: the profile options are server config, only readable while a server runs.
        MinecraftServer server = player.getServer();
        if (server == null || !isEnabled()) {
            return null;
        }
        return PlayerProfileData.get(server).getActiveProfile(player.getUUID());
    }

    public static boolean isSafemode(Player player) {
        return getActiveProfile(player) == ProfileType.SAFEMODE;
    }

    public static boolean isSafemode(MinecraftServer server, UUID uuid) {
        if (!isEnabled()) {
            return false;
        }
        return PlayerProfileData.get(server).getActiveProfile(uuid) == ProfileType.SAFEMODE;
    }

    public static boolean needsToChooseProfile(ServerPlayer player) {
        if (!isEnabled()) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        PlayerProfileData data = PlayerProfileData.get(server);
        if (data.hasChosenProfile(player.getUUID())) {
            return false;
        }
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        if (holder.getCurrentClass() != null && holder.getSequence() >= 0) {
            applyActiveProfile(data, player, ProfileType.NORMAL);
            return false;
        }
        return true;
    }

    @Nullable
    public static Integer getSequenceCap(Player player) {
        return getActiveProfile(player) == ProfileType.SAFEMODE
                ? PTDServerConfig.SAFEMODE_MAX_SEQUENCE.get()
                : null;
    }

    public static boolean blocksSequence(Player player, int potionSequence) {
        Integer cap = getSequenceCap(player);
        return cap != null && potionSequence < cap;
    }
    
    public static void chooseInitialProfile(ServerPlayer player, ProfileType type) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        applyActiveProfile(PlayerProfileData.get(server), player, type);
    }

    /**
     * The only way a profile is written. Going through here means a profile cannot change without every
     * client hearing about it, which is what keeps the Safemode name tag honest.
     */
    private static void applyActiveProfile(PlayerProfileData data, ServerPlayer player, ProfileType type) {
        data.setActiveProfile(player.getUUID(), type);
        ProfileVisibility.broadcast(player, type);
    }

    @Nullable
    public static Component switchTo(ServerPlayer player, ProfileType target) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Component.literal("Profiles are only available on a running server.");
        }
        PlayerProfileData data = PlayerProfileData.get(server);
        UUID uuid = player.getUUID();

        ProfileType current = data.getActiveProfile(uuid);
        if (current == null) {
            chooseInitialProfile(player, target);
            return null;
        }
        if (current == target) {
            return Component.literal("You are already on your " + target.getDisplayName() + " profile.");
        }

        Component refusal = findSwitchBlocker(player, data, uuid, server);
        if (refusal != null) {
            return refusal;
        }

        CompoundTag outgoing;
        try {
            outgoing = ProfileSnapshot.capture(player);
        } catch (Exception exception) {
            PTD.LOGGER.error("Failed to capture profile {} for {}; aborting switch",
                    current.getId(), player.getGameProfile().getName(), exception);
            return Component.literal("Could not save your current profile, so nothing was changed. "
                    + "Tell an admin to check the server log.");
        }
        data.putSnapshot(uuid, current, outgoing);

        CompoundTag incoming = data.getSnapshot(uuid, target);
        try {
            ProfileSnapshot.restore(player, incoming == null ? new CompoundTag() : incoming);
        } catch (Exception exception) {
            PTD.LOGGER.error("Failed to restore profile {} for {}; rolling back to {}",
                    target.getId(), player.getGameProfile().getName(), current.getId(), exception);
            try {
                ProfileSnapshot.restore(player, outgoing);
            } catch (Exception rollbackFailure) {
                PTD.LOGGER.error("Rollback to profile {} for {} also failed",
                        current.getId(), player.getGameProfile().getName(), rollbackFailure);
            }
            data.clearSnapshot(uuid, current);
            return Component.literal("Could not load your " + target.getDisplayName()
                    + " profile. You have been left on " + current.getDisplayName() + ".");
        }

        applyActiveProfile(data, player, target);
        data.clearSnapshot(uuid, target);
        data.setLastSwitchGameTime(uuid, server.overworld().getGameTime());

        // Last, so a refused or rolled-back switch never moves the player.
        ProfileSpawn.sendToWorldSpawn(player, server);
        return null;
    }

    @Nullable
    public static Component getSwitchBlocker(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Component.literal("Profiles are only available on a running server.");
        }
        return findSwitchBlocker(player, PlayerProfileData.get(server), player.getUUID(), server);
    }

    public static String describeLiveBeyonder(Player player) {
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        BeyonderClass pathway = holder.getCurrentClass();
        int sequence = holder.getSequence();
        if (pathway == null || sequence < 0) {
            return "not a Beyonder";
        }
        List<Component> names = pathway.sequenceNames();
        String title = sequence < names.size() ? names.get(sequence).getString() : "Unknown";
        return title + ", Sequence " + sequence;
    }

    @Nullable
    private static Component findSwitchBlocker(ServerPlayer player, PlayerProfileData data, UUID uuid,
                                               MinecraftServer server) {
        int combatTimer = CombatTag.remainingTicks(player);
        if (combatTimer > 0) {
            return Component.literal("You cannot switch profiles while in combat. Wait "
                    + Math.max(1, combatTimer / 20) + " more second(s).");
        }

        long remaining = getRemainingCooldownTicks(data, uuid, server);
        if (remaining > 0) {
            return Component.literal("You must wait " + formatTicks(remaining)
                    + " before switching profiles again.");
        }

        Component slotBlocker = findLimitedSlotBlocker(player, uuid, server);
        if (slotBlocker != null) {
            return slotBlocker;
        }
        return null;
    }

    /**
     * Refuses the switch while the player occupies one of LOTM's capped Sequence 0/1/2 slots.
     *
     * <p>Without this a Sequence 0 holder could park on Safemode indefinitely — immune to regression — while
     * still holding the world's only Sequence 0 slot for their pathway, which is exactly the hoarding this
     * feature exists to prevent. Refusing is preferable to silently releasing the slot, which would hand the
     * player's hard-won position to someone else without warning.
     */
    @Nullable
    private static Component findLimitedSlotBlocker(ServerPlayer player, UUID uuid, MinecraftServer server) {
        if (!Configs.COMMON.onlyOneGod.get()) {
            return null;
        }
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        BeyonderClass pathway = holder.getCurrentClass();
        int sequence = holder.getSequence();
        if (pathway == null || sequence < 0 || !Sequence0Data.isLimitedSequence(sequence)) {
            return null;
        }
        if (!Sequence0Data.get(server).isSequenceHolder(uuid, pathway, sequence)) {
            return null;
        }
        return Component.literal("You hold one of this world's limited Sequence " + sequence
                + " slots. Give it up before switching profiles.");
    }

    public static long getRemainingCooldownTicks(MinecraftServer server, UUID uuid) {
        return getRemainingCooldownTicks(PlayerProfileData.get(server), uuid, server);
    }

    private static long getRemainingCooldownTicks(PlayerProfileData data, UUID uuid, MinecraftServer server) {
        int cooldownMinutes = PTDServerConfig.PROFILE_SWITCH_COOLDOWN_MINUTES.get();
        if (cooldownMinutes <= 0) {
            return 0L;
        }
        long lastSwitch = data.getLastSwitchGameTime(uuid);
        if (lastSwitch == Long.MIN_VALUE) {
            return 0L;
        }
        long elapsed = server.overworld().getGameTime() - lastSwitch;
        long required = cooldownMinutes * TICKS_PER_MINUTE;
        // A negative elapsed means the stamp is in the future - a rolled back world, say - so it is not trusted.
        if (elapsed < 0 || elapsed >= required) {
            return 0L;
        }
        return required - elapsed;
    }
    
    public static int setRemainingCooldownMinutes(MinecraftServer server, UUID uuid, int minutes) {
        PlayerProfileData data = PlayerProfileData.get(server);
        if (minutes <= 0) {
            data.clearLastSwitchGameTime(uuid);
            return 0;
        }
        long required = (long) PTDServerConfig.PROFILE_SWITCH_COOLDOWN_MINUTES.get() * TICKS_PER_MINUTE;
        long remaining = Math.min(minutes * TICKS_PER_MINUTE, required);
        data.setLastSwitchGameTime(uuid, server.overworld().getGameTime() - (required - remaining));
        return (int) (remaining / TICKS_PER_MINUTE);
    }

    /**
     * Ticks left on this player's post-death regression immunity, or {@code 0} when they have none.
     *
     * <p>Mirrors {@link #getRemainingCooldownTicks}, including its refusal to trust a stamp from the future.
     * The stamp lives in {@link PlayerProfileData} rather than the player's persistent data because a
     * respawn wipes the latter, which would let the window be skipped by dying again.
     */
    public static long getRemainingRegressionGraceTicks(MinecraftServer server, UUID uuid, int graceMinutes) {
        if (graceMinutes <= 0) {
            return 0L;
        }
        long lastRegression = PlayerProfileData.get(server).getLastRegressionGameTime(uuid);
        if (lastRegression == Long.MIN_VALUE) {
            return 0L;
        }
        long elapsed = server.overworld().getGameTime() - lastRegression;
        long required = (long) graceMinutes * TICKS_PER_MINUTE;
        // A negative elapsed means the stamp is in the future - a rolled back world, say - so it is not trusted.
        if (elapsed < 0 || elapsed >= required) {
            return 0L;
        }
        return required - elapsed;
    }

    /** Starts the post-death regression immunity window for this player, from now. */
    public static void startRegressionGrace(MinecraftServer server, UUID uuid) {
        PlayerProfileData.get(server).setLastRegressionGameTime(uuid, server.overworld().getGameTime());
    }

    public static String formatTicks(long ticks) {
        long totalMinutes = Math.max(1, ticks / TICKS_PER_MINUTE);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    public static void sendChoicePrompt(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                        "You are not yet a Beyonder. Choose a profile before you drink.")
                .withStyle(ChatFormatting.GOLD));
        openScreen(player);
    }

    public static void openScreen(ServerPlayer player) {
        openScreen(player, null, false);
    }

    public static void openScreen(ServerPlayer player, @Nullable Component status, boolean success) {
        PTDNetwork.sendToPlayer(ProfileScreenS2CPacket.forPlayer(player, status, success), player);
    }
}
