package net.swimmingtuna.pathtodivinity.inactivity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.data.BeyonderHolder;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.mixin.LOTMC.Sequence0DataAccessor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.swimmingtuna.lotm.beyonder.Pathways;

/**
 * Demotes Sequence 2, 1 and 0 players who stop playing ([inactivity] in the server config) by one Sequence.
 *
 * <p>A player's timeout restarts once they have been online for {@code reset_hours}, added up across sessions;
 * a logout longer than {@code max_logout_minutes} throws the added-up time away. When an offline limited-Sequence
 * player's timeout runs out, their LOTM slot is released straight away, so someone else can take it, and they
 * drop one Sequence when they next log in (an offline player's Beyonder data can't be edited). If the Sequence
 * below is full, LOTM force-claims a slot for the demoted player, so it can briefly go over its cap.
 *
 * <p>Demotions cascade: each one is a strike, and the next timeout is shorter (7, then 2, then 1 day by default),
 * until the player is out of the capped Sequences. Playing for {@code reset_hours} clears the strikes.
 *
 * <p>LOTM's own fixed login demotion is switched off while this is enabled, see {@code Sequence0DataMixin}.
 */
@EventBusSubscriber(modid = PTD.MOD_ID)
public final class InactivityTracker {

    private static final int SWEEP_INTERVAL_TICKS = 1200;
    private static final long MINUTE_MILLIS = 60_000L;
    private static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;

    /** When each online player's online time was last added up. Not saved: a crash loses at most a sweep's worth. */
    private static final Map<UUID, Long> LAST_CREDITED = new HashMap<>();

    private InactivityTracker() {
    }

    public static boolean isEnabled() {
        return PTDServerConfig.INACTIVITY_ENABLED.get();
    }

    /** 7 days for the first demotion, then 2, then 1 for every one after (by default). */
    public static long timeoutMillis(int strikes) {
        double days = switch (strikes) {
            case 0 -> PTDServerConfig.INACTIVITY_TIMEOUT_DAYS.get();
            case 1 -> PTDServerConfig.INACTIVITY_SECOND_TIMEOUT_DAYS.get();
            default -> PTDServerConfig.INACTIVITY_THIRD_TIMEOUT_DAYS.get();
        };
        return (long) (days * DAY_MILLIS);
    }

    /** HIGHEST: the demotion must land before LOTM's login handler re-claims the slot it was released from. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnabled()) {
            return;
        }
        InactivityData data = InactivityData.get(player.server);
        InactivityData.Entry entry = data.entryFor(player.getUUID());
        long now = System.currentTimeMillis();
        if (entry.pendingSteps > 0) {
            demote(player, entry, now);
        }
        long maxLogout = PTDServerConfig.INACTIVITY_MAX_LOGOUT_MINUTES.get() * MINUTE_MILLIS;
        if (entry.lastLogoutMillis != Long.MIN_VALUE && now - entry.lastLogoutMillis > maxLogout) {
            entry.onlineMillis = 0;
        }
        if (entry.lastActiveMillis == Long.MIN_VALUE) {
            entry.lastActiveMillis = now;
        }
        LAST_CREDITED.put(player.getUUID(), now);
        observe(player, entry, now);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnabled()) {
            return;
        }
        InactivityData data = InactivityData.get(player.server);
        InactivityData.Entry entry = data.entryFor(player.getUUID());
        long now = System.currentTimeMillis();
        credit(player.getUUID(), entry, now);
        LAST_CREDITED.remove(player.getUUID());
        entry.lastLogoutMillis = now;
        observe(player, entry, now);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LAST_CREDITED.clear();
        if (isEnabled()) {
            long now = System.currentTimeMillis();
            seedFromSlots(event.getServer(), now);
            sweep(event.getServer(), now);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SWEEP_INTERVAL_TICKS != 0 || !isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        InactivityData data = InactivityData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            InactivityData.Entry entry = data.entryFor(player.getUUID());
            credit(player.getUUID(), entry, now);
            observe(player, entry, now);
        }
        data.setDirty();
        sweep(server, now);
    }

    /** Adds the time since the last credit to the player's online time, restarting their timeout once it is enough. */
    private static void credit(UUID uuid, InactivityData.Entry entry, long now) {
        Long last = LAST_CREDITED.put(uuid, now);
        // A clock moved backwards gives a negative stretch, which is not counted.
        entry.onlineMillis += last == null ? 0 : Math.max(0, now - last);
        if (entry.onlineMillis >= (long) (PTDServerConfig.INACTIVITY_RESET_HOURS.get() * HOUR_MILLIS)) {
            entry.lastActiveMillis = now;
            entry.onlineMillis = 0;
            entry.strikes = 0;
        }
    }

    /**
     * Records which limited Sequence (2, 1 or 0) the player holds. Reaching a new one counts as activity, so a
     * stale timeout can't fire at once.
     */
    private static void observe(ServerPlayer player, InactivityData.Entry entry, long now) {
        BeyonderHolder holder = BeyonderHolder.get(player);
        BeyonderClass pathway = holder.getCurrentClass();
        int sequence = holder.getSequence();
        if (pathway == null || !Sequence0Data.isLimitedSequence(sequence)) {
            entry.limitedPathway = null;
            entry.strikes = 0; // out of the capped Sequences, so the chain is over
            return;
        }
        if (entry.limitedPathway != null && sequence < entry.limitedSequence) {
            entry.strikes = 0; // advanced, so clearly playing
        }
        if (entry.limitedPathway == null || entry.limitedSequence != sequence) {
            entry.lastActiveMillis = now;
        }
        entry.limitedPathway = BeyonderClassInit.getRegistry().getKey(pathway).toString();
        entry.limitedSequence = sequence;
    }

    /**
     * Slot holders this tracker has never seen (offline since before it was installed) start their timeout now
     * rather than never being tracked.
     */
    private static void seedFromSlots(MinecraftServer server, long now) {
        InactivityData data = InactivityData.get(server);
        Map<Integer, Map<String, Set<UUID>>> holders =
                ((Sequence0DataAccessor) Sequence0Data.get(server)).pathtodivinity$sequenceHolders();
        for (BeyonderClass pathway : BeyonderClassInit.getRegistry()) {
            String name = Sequence0DataAccessor.pathtodivinity$pathwayName(pathway);
            for (Map.Entry<Integer, Map<String, Set<UUID>>> level : holders.entrySet()) {
                for (UUID holder : level.getValue().getOrDefault(name, Set.of())) {
                    InactivityData.Entry entry = data.entryFor(holder);
                    if (entry.limitedPathway == null) {
                        entry.limitedPathway = BeyonderClassInit.getRegistry().getKey(pathway).toString();
                        entry.limitedSequence = level.getKey();
                    }
                    if (entry.lastActiveMillis == Long.MIN_VALUE) {
                        entry.lastActiveMillis = now;
                    }
                }
            }
        }
        data.setDirty();
    }

    /**
     * Demotes every offline limited-Sequence holder whose timeout has run out, one Sequence per timeout, until they
     * leave the capped Sequences. Each step releases the slot they would hold and is applied at their next login.
     */
    private static void sweep(MinecraftServer server, long now) {
        InactivityData data = InactivityData.get(server);
        for (Map.Entry<UUID, InactivityData.Entry> mapEntry : data.entries().entrySet()) {
            UUID uuid = mapEntry.getKey();
            InactivityData.Entry entry = mapEntry.getValue();
            if (entry.limitedPathway == null || server.getPlayerList().getPlayer(uuid) != null) {
                continue;
            }
            cascade(server, uuid, entry, now);
            data.setDirty();
        }
    }

    /** Queues one demotion per timeout that has run out for an offline player, stopping below Sequence 2. */
    public static void cascade(MinecraftServer server, UUID uuid, InactivityData.Entry entry, long now) {
        while (Sequence0Data.isLimitedSequence(effectiveSequence(entry))
                && isExpired(entry, now, timeoutMillis(entry.strikes))) {
            // The next timeout runs from when this demotion was due, so server downtime still counts.
            entry.lastActiveMillis += timeoutMillis(entry.strikes);
            entry.strikes++;
            queueDemotion(server, uuid, entry);
            PTD.LOGGER.info("{} ({}) inactive: demoted to Sequence {} at next login (strike {})",
                    uuid, entry.limitedPathway, effectiveSequence(entry), entry.strikes);
        }
    }

    /** The Sequence the player will be at once their queued demotions land. */
    public static int effectiveSequence(InactivityData.Entry entry) {
        return entry.limitedSequence + entry.pendingSteps;
    }

    /** Frees the slot an offline player would hold and queues one more demotion for their next login. */
    private static void queueDemotion(MinecraftServer server, UUID uuid, InactivityData.Entry entry) {
        ResourceLocation pathwayId = ResourceLocation.tryParse(entry.limitedPathway);
        BeyonderClass pathway = pathwayId == null ? null : BeyonderClassInit.getRegistry().get(pathwayId);
        if (pathway != null) {
            Sequence0Data.get(server).releaseSequence(uuid, pathway, effectiveSequence(entry));
        }
        entry.pendingSteps++;
    }

    /**
     * {@code /beyonder demote}: one Sequence down through the same path as an inactivity demotion. Online players
     * change at once; offline players need a tracked Sequence 2-0 entry, and the change lands at their next login.
     *
     * @return null on success, otherwise why it could not be done
     */
    @Nullable
    public static String adminDemote(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            BeyonderHolder holder = BeyonderHolder.get(player);
            int sequence = holder.getSequence();
            if (holder.getCurrentClass() == null || sequence < 0 || sequence >= 9) {
                return "They are not a Beyonder who can drop a Sequence.";
            }
            Pathways.setSequence(player, sequence + 1);
            if (holder.getSequence() == sequence) {
                return "LOTM refused the sequence change.";
            }
            if (isEnabled()) {
                InactivityData data = InactivityData.get(server);
                observe(player, data.entryFor(uuid), System.currentTimeMillis());
                data.setDirty();
            }
            return null;
        }
        if (!isEnabled()) {
            return "They are offline, and offline demotion needs [inactivity] enabled.";
        }
        InactivityData data = InactivityData.get(server);
        InactivityData.Entry entry = data.entries().get(uuid);
        if (entry == null || entry.limitedPathway == null || !Sequence0Data.isLimitedSequence(effectiveSequence(entry))) {
            return "They are offline and not holding Sequence 2-0, so they can only be demoted while online.";
        }
        queueDemotion(server, uuid, entry);
        data.setDirty();
        return null;
    }

    public static boolean isExpired(InactivityData.Entry entry, long now, long timeout) {
        // A timeout stamped in the future (clock moved backwards) gives a negative age, which never expires.
        return entry.lastActiveMillis != Long.MIN_VALUE && now - entry.lastActiveMillis >= timeout;
    }

    /** Applies every demotion queued while the player was offline. Strikes are kept, so the next timeout is short. */
    private static void demote(ServerPlayer player, InactivityData.Entry entry, long now) {
        int sequence = entry.limitedSequence;
        int target = effectiveSequence(entry);
        entry.pendingSteps = 0;
        entry.limitedPathway = null;
        entry.lastActiveMillis = now;
        BeyonderHolder holder = BeyonderHolder.get(player);
        if (holder.getCurrentClass() == null || holder.getSequence() != sequence) {
            return; // an admin already changed their sequence
        }
        Pathways.setSequence(player, target);
        if (holder.getSequence() == sequence) {
            PTD.LOGGER.warn("Could not demote Sequence {} player {}; LOTM refused the sequence change",
                    sequence, player.getGameProfile().getName());
            return;
        }
        PTD.LOGGER.info("Demoted player {} from Sequence {} to Sequence {}",
                player.getGameProfile().getName(), sequence, target);
        player.sendSystemMessage(Component.literal("While you were away you fell to Sequence " + target
                + ". Your Sequence " + sequence + " position was given up.").withStyle(ChatFormatting.RED));
    }

    /** Readable "2d 5h" / "3h 10m" / "4m". */
    public static String formatMillis(long millis) {
        long minutes = Math.max(0, millis / MINUTE_MILLIS);
        long days = minutes / (24 * 60);
        long hours = minutes / 60 % 24;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return hours > 0 ? hours + "h " + minutes % 60 + "m" : minutes + "m";
    }
}
