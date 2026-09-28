package net.swimmingtuna.pathtodivinity;

import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world storage for the sequence lock — the strongest sequence players are allowed to advance to.
 *
 * <p>Sequences run 9 (weakest) down to 0 (strongest), so a lock of 7 means 9, 8 and 7 are reachable
 * and anything lower is refused. {@link #NO_LOCK} means no cap is in effect.
 *
 * <p>Stored on the overworld's data storage so there is exactly one lock per save, not one per dimension.
 */
public class SequenceLockData extends SavedData {

    /** Sentinel for "no cap in effect". Chosen to sit outside the valid 0-9 range. */
    public static final int NO_LOCK = -1;

    private static final String FILE_ID = "pathtodivinity_sequence_lock";
    private static final String KEY = "lockedSequence";

    private int lockedSequence = NO_LOCK;

    public static SequenceLockData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SequenceLockData::load, SequenceLockData::new, FILE_ID);
    }

    private static SequenceLockData load(CompoundTag tag) {
        SequenceLockData data = new SequenceLockData();
        data.lockedSequence = tag.contains(KEY) ? tag.getInt(KEY) : NO_LOCK;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(KEY, this.lockedSequence);
        return tag;
    }

    public int getLockedSequence() {
        return this.lockedSequence;
    }

    public void setLockedSequence(int sequence) {
        this.lockedSequence = sequence;
        setDirty();
    }

    public boolean isLocked() {
        return this.lockedSequence != NO_LOCK;
    }

    /** True when a potion that would grant {@code potionSequence} is out of reach under the current lock. */
    public boolean blocks(int potionSequence) {
        return isLocked() && potionSequence < this.lockedSequence;
    }

    /**
     * The lock that stops this player from taking a potion for {@code potionSequence}, or {@link #NO_LOCK}
     * when they are free to drink it. Returns the level rather than a boolean so callers can name it in the
     * message they show the player.
     */
    public static int blockingLockFor(Player player, int potionSequence) {
        if (!PTDServerConfig.SEQUENCE_LOCK_ENABLED.get()) {
            return NO_LOCK;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return NO_LOCK;
        }
        SequenceLockData data = get(server);
        return data.blocks(potionSequence) ? data.getLockedSequence() : NO_LOCK;
    }
}
