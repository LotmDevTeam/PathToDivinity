package net.swimmingtuna.pathtodivinity.inactivity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player activity for {@link InactivityTracker}, saved with the overworld. Every time is real-world
 * {@link System#currentTimeMillis()}, so days the server is down still count.
 */
public class InactivityData extends SavedData {

    private static final String FILE_ID = "pathtodivinity_inactivity";

    public static final class Entry {
        /** When this player's timeout last restarted. */
        public long lastActiveMillis = Long.MIN_VALUE;
        /** Online time counted towards restarting the timeout. */
        public long onlineMillis;
        public long lastLogoutMillis = Long.MIN_VALUE;
        /** Pathway id this player was last seen holding a limited Sequence (2, 1 or 0) in, or null. */
        @Nullable
        public String limitedPathway;
        /** The limited Sequence they were last seen at; meaningless while {@link #limitedPathway} is null. */
        public int limitedSequence;
        /** Demotions decided while offline (their slot already released); applied together at next login. */
        public int pendingSteps;
        /** Inactivity demotions in a row; picks the next, shorter timeout. Cleared once they are active again. */
        public int strikes;
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static InactivityData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(InactivityData::new, InactivityData::load, null), FILE_ID);
    }

    private static InactivityData load(CompoundTag tag, HolderLookup.Provider registries) {
        InactivityData data = new InactivityData();
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (!playerTag.hasUUID("uuid")) {
                continue;
            }
            Entry entry = new Entry();
            entry.lastActiveMillis = playerTag.contains("lastActive") ? playerTag.getLong("lastActive") : Long.MIN_VALUE;
            entry.onlineMillis = playerTag.getLong("online");
            entry.lastLogoutMillis = playerTag.contains("lastLogout") ? playerTag.getLong("lastLogout") : Long.MIN_VALUE;
            if (playerTag.contains("limitedPathway")) {
                entry.limitedPathway = playerTag.getString("limitedPathway");
                entry.limitedSequence = playerTag.getInt("limitedSequence");
            } else if (playerTag.contains("sequence0Pathway")) {
                entry.limitedPathway = playerTag.getString("sequence0Pathway"); // saved before Sequences 1 and 2 were tracked
            }
            entry.pendingSteps = playerTag.getInt("pendingSteps");
            entry.strikes = playerTag.getInt("strikes");
            if (playerTag.getBoolean("pendingDemotion")) { // saved before demotions could cascade
                entry.pendingSteps = 1;
                entry.strikes = 1;
            }
            data.entries.put(playerTag.getUUID("uuid"), entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", mapEntry.getKey());
            if (entry.lastActiveMillis != Long.MIN_VALUE) {
                playerTag.putLong("lastActive", entry.lastActiveMillis);
            }
            playerTag.putLong("online", entry.onlineMillis);
            if (entry.lastLogoutMillis != Long.MIN_VALUE) {
                playerTag.putLong("lastLogout", entry.lastLogoutMillis);
            }
            if (entry.limitedPathway != null) {
                playerTag.putString("limitedPathway", entry.limitedPathway);
                playerTag.putInt("limitedSequence", entry.limitedSequence);
            }
            playerTag.putInt("pendingSteps", entry.pendingSteps);
            playerTag.putInt("strikes", entry.strikes);
            players.add(playerTag);
        }
        tag.put("players", players);
        return tag;
    }

    /** Callers change the entry in place, then call {@link #setDirty()}. */
    public Entry entryFor(UUID uuid) {
        return entries.computeIfAbsent(uuid, ignored -> new Entry());
    }

    public Map<UUID, Entry> entries() {
        return entries;
    }
}
