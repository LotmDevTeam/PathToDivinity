package net.swimmingtuna.pathtodivinity.profile;

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

public class PlayerProfileData extends SavedData {

    private static final String FILE_ID = "pathtodivinity_profiles";

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_LAST_SWITCH = "lastSwitch";
    private static final String KEY_LAST_REGRESSION = "lastRegression";
    private static final String KEY_SNAPSHOT_PREFIX = "snapshot_";

    private final Map<UUID, Entry> entries = new HashMap<>();

    private static class Entry {
        @Nullable
        ProfileType active;
        final Map<ProfileType, CompoundTag> snapshots = new HashMap<>();
        long lastSwitchGameTime = Long.MIN_VALUE;
        /**
         * When this player last lost a sequence to a player kill, as overworld game time.
         *
         * <p>Kept here rather than in the player's persistent data, where LOTM's own
         * {@code sequenceRegressionProtectionTimer} lives: NeoForge carries only the {@code PlayerPersisted}
         * subtag across a respawn, so a stamp written there is erased by the very death that sets it and the
         * grace window never spans anything.
         */
        long lastRegressionGameTime = Long.MIN_VALUE;
    }

    public static PlayerProfileData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(PlayerProfileData::new, PlayerProfileData::load, null), FILE_ID);
    }

    private static PlayerProfileData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerProfileData data = new PlayerProfileData();
        ListTag players = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (!playerTag.hasUUID(KEY_UUID)) {
                continue;
            }
            Entry entry = new Entry();
            entry.active = ProfileType.byId(playerTag.getString(KEY_ACTIVE));
            entry.lastSwitchGameTime = playerTag.contains(KEY_LAST_SWITCH)
                    ? playerTag.getLong(KEY_LAST_SWITCH)
                    : Long.MIN_VALUE;
            entry.lastRegressionGameTime = playerTag.contains(KEY_LAST_REGRESSION)
                    ? playerTag.getLong(KEY_LAST_REGRESSION)
                    : Long.MIN_VALUE;
            for (ProfileType type : ProfileType.values()) {
                String key = KEY_SNAPSHOT_PREFIX + type.getId();
                if (playerTag.contains(key)) {
                    entry.snapshots.put(type, playerTag.getCompound(key));
                }
            }
            data.entries.put(playerTag.getUUID(KEY_UUID), entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Entry> mapEntry : this.entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(KEY_UUID, mapEntry.getKey());
            if (entry.active != null) {
                playerTag.putString(KEY_ACTIVE, entry.active.getId());
            }
            if (entry.lastSwitchGameTime != Long.MIN_VALUE) {
                playerTag.putLong(KEY_LAST_SWITCH, entry.lastSwitchGameTime);
            }
            if (entry.lastRegressionGameTime != Long.MIN_VALUE) {
                playerTag.putLong(KEY_LAST_REGRESSION, entry.lastRegressionGameTime);
            }
            for (Map.Entry<ProfileType, CompoundTag> snapshot : entry.snapshots.entrySet()) {
                playerTag.put(KEY_SNAPSHOT_PREFIX + snapshot.getKey().getId(), snapshot.getValue());
            }
            players.add(playerTag);
        }
        tag.put(KEY_PLAYERS, players);
        return tag;
    }

    private Entry entryFor(UUID uuid) {
        return this.entries.computeIfAbsent(uuid, ignored -> new Entry());
    }

    @Nullable
    public ProfileType getActiveProfile(UUID uuid) {
        Entry entry = this.entries.get(uuid);
        return entry == null ? null : entry.active;
    }

    public boolean hasChosenProfile(UUID uuid) {
        return getActiveProfile(uuid) != null;
    }

    public void setActiveProfile(UUID uuid, ProfileType type) {
        entryFor(uuid).active = type;
        setDirty();
    }

    @Nullable
    public CompoundTag getSnapshot(UUID uuid, ProfileType type) {
        Entry entry = this.entries.get(uuid);
        return entry == null ? null : entry.snapshots.get(type);
    }

    public void putSnapshot(UUID uuid, ProfileType type, CompoundTag snapshot) {
        entryFor(uuid).snapshots.put(type, snapshot);
        setDirty();
    }

    public void clearSnapshot(UUID uuid, ProfileType type) {
        Entry entry = this.entries.get(uuid);
        if (entry != null && entry.snapshots.remove(type) != null) {
            setDirty();
        }
    }

    public long getLastSwitchGameTime(UUID uuid) {
        Entry entry = this.entries.get(uuid);
        return entry == null ? Long.MIN_VALUE : entry.lastSwitchGameTime;
    }

    public void setLastSwitchGameTime(UUID uuid, long gameTime) {
        entryFor(uuid).lastSwitchGameTime = gameTime;
        setDirty();
    }
    
    public long getLastRegressionGameTime(UUID uuid) {
        Entry entry = this.entries.get(uuid);
        return entry == null ? Long.MIN_VALUE : entry.lastRegressionGameTime;
    }

    public void setLastRegressionGameTime(UUID uuid, long gameTime) {
        entryFor(uuid).lastRegressionGameTime = gameTime;
        setDirty();
    }

    public void clearLastSwitchGameTime(UUID uuid) {
        Entry entry = this.entries.get(uuid);
        if (entry != null && entry.lastSwitchGameTime != Long.MIN_VALUE) {
            entry.lastSwitchGameTime = Long.MIN_VALUE;
            setDirty();
        }
    }
}
