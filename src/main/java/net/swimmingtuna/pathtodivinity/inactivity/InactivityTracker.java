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
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Demotes Sequence 0 players who stop playing ([inactivity] in the server config).
 *
 * <p>A player's timeout restarts once they have been online for {@code reset_hours}, added up across sessions;
 * a logout longer than {@code max_logout_minutes} throws the added-up time away. When an offline Sequence 0
 * player's timeout runs out, their LOTM Sequence 0 slot is released straight away, so someone else can take
 * it, and they drop to Sequence 1 when they next log in (an offline player's Beyonder data can't be edited).
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

    public static long timeoutMillis() {
        return (long) (PTDServerConfig.INACTIVITY_TIMEOUT_DAYS.get() * DAY_MILLIS);
    }

    /** HIGHEST: the demotion must land before LOTM's login handler re-claims the Sequence 0 slot it was released from. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEnabled()) {
            return;
        }
        InactivityData data = InactivityData.get(player.server);
        InactivityData.Entry entry = data.entryFor(player.getUUID());
        long now = System.currentTimeMillis();
        if (entry.pendingDemotion) {
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
        }
    }

    /** Records whether the player holds Sequence 0. Reaching it counts as activity, so a stale timeout can't fire at once. */
    private static void observe(ServerPlayer player, InactivityData.Entry entry, long now) {
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        BeyonderClass pathway = holder.getCurrentClass();
        if (pathway == null || holder.getSequence() != 0) {
            entry.sequence0Pathway = null;
            return;
        }
        if (entry.sequence0Pathway == null) {
            entry.lastActiveMillis = now;
        }
        entry.sequence0Pathway = BeyonderClassInit.getRegistry().getKey(pathway).toString();
    }

    /**
     * Sequence 0 holders this tracker has never seen (offline since before it was installed) start their
     * timeout now rather than never being tracked.
     */
    private static void seedFromSlots(MinecraftServer server, long now) {
        InactivityData data = InactivityData.get(server);
        Sequence0Data slots = Sequence0Data.get(server);
        for (BeyonderClass pathway : BeyonderClassInit.getRegistry()) {
            UUID holder = slots.getSequence0Holder(pathway);
            if (holder == null) {
                continue;
            }
            InactivityData.Entry entry = data.entryFor(holder);
            if (entry.sequence0Pathway == null) {
                entry.sequence0Pathway = BeyonderClassInit.getRegistry().getKey(pathway).toString();
            }
            if (entry.lastActiveMillis == Long.MIN_VALUE) {
                entry.lastActiveMillis = now;
            }
        }
        data.setDirty();
    }

    /** Releases the Sequence 0 slot of every offline holder whose timeout has run out. */
    private static void sweep(MinecraftServer server, long now) {
        InactivityData data = InactivityData.get(server);
        Sequence0Data slots = Sequence0Data.get(server);
        long timeout = timeoutMillis();
        for (Map.Entry<UUID, InactivityData.Entry> mapEntry : data.entries().entrySet()) {
            UUID uuid = mapEntry.getKey();
            InactivityData.Entry entry = mapEntry.getValue();
            if (entry.pendingDemotion || entry.sequence0Pathway == null
                    || server.getPlayerList().getPlayer(uuid) != null
                    || !isExpired(entry, now, timeout)) {
                continue;
            }
            ResourceLocation pathwayId = ResourceLocation.tryParse(entry.sequence0Pathway);
            BeyonderClass pathway = pathwayId == null ? null : BeyonderClassInit.getRegistry().get(pathwayId);
            if (pathway != null) {
                slots.releaseSequence(uuid, pathway, 0);
            }
            entry.pendingDemotion = true;
            data.setDirty();
            PTD.LOGGER.info("Released {}'s Sequence 0 slot ({}): inactive for {} days; demoting them to Sequence 1 at next login",
                    uuid, entry.sequence0Pathway, PTDServerConfig.INACTIVITY_TIMEOUT_DAYS.get());
        }
    }

    public static boolean isExpired(InactivityData.Entry entry, long now, long timeout) {
        // A timeout stamped in the future (clock moved backwards) gives a negative age, which never expires.
        return entry.lastActiveMillis != Long.MIN_VALUE && now - entry.lastActiveMillis >= timeout;
    }

    private static void demote(ServerPlayer player, InactivityData.Entry entry, long now) {
        entry.pendingDemotion = false;
        entry.sequence0Pathway = null;
        entry.lastActiveMillis = now;
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        if (holder.getCurrentClass() == null || holder.getSequence() != 0) {
            return; // an admin already changed their sequence
        }
        BeyonderUtil.setSequence(player, 1);
        if (holder.getSequence() == 0) {
            PTD.LOGGER.warn("Could not demote inactive Sequence 0 player {}; LOTM refused the sequence change",
                    player.getGameProfile().getName());
            return;
        }
        PTD.LOGGER.info("Demoted inactive player {} from Sequence 0 to Sequence 1", player.getGameProfile().getName());
        player.sendSystemMessage(Component.literal("You were inactive for too long and fell to Sequence 1. "
                + "Your Sequence 0 position was given up.").withStyle(ChatFormatting.RED));
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
