package net.swimmingtuna.pathtodivinity.beyonders;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Where code asks "which boss profile applies to this entity?". The server's profiles come from
 * datapacks ({@link ProfileLoader}); the client gets a copy of what it needs from
 * {@link ProfileSyncPacket}, so both sides agree on e.g. a mob's sequence.
 */
public final class BeyonderProfiles {

    private static volatile ProfileSnapshot server = ProfileSnapshot.EMPTY;
    private static volatile ProfileSnapshot client = ProfileSnapshot.EMPTY;

    private BeyonderProfiles() {
    }

    @Nullable
    public static BeyonderProfile find(Entity entity) {
        return (entity.level().isClientSide() ? client : server).find(entity);
    }

    /** A Beyonder boss: regenerates out of combat, breaks blocks to reach players, holds its target. */
    public static boolean isBoss(Entity entity) {
        BeyonderProfile profile = find(entity);
        return profile != null && profile.boss();
    }

    public static ProfileSnapshot server() {
        return server;
    }

    static void setServer(ProfileSnapshot snapshot) {
        server = snapshot;
    }

    static void setClient(ProfileSnapshot snapshot) {
        client = snapshot;
    }
}
