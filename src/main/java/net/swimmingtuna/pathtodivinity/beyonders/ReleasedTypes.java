package net.swimmingtuna.pathtodivinity.beyonders;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.swimmingtuna.lotm.beyonder.Pathways;

import java.util.Set;

/**
 * Entity types whose LOTM {@code /beyonderentity} mapping an admin set by hand over a PtD boss profile (LOTM #203).
 * For these, the hand mapping wins: the profile's {@code sequence} and {@code "ability_sequence": -1} (inert) rules
 * are not applied, so the mob fights with the pathway, sequence, abilities and pathway health it was given. The rest
 * of the profile (boss flag and AI, health and damage multipliers, effects, drops) still applies.
 *
 * <p>The server's set comes from {@link BeyonderRegistrations}; the client gets a copy from
 * {@link ReleasedTypesPacket}, so a mob's sequence reads the same on both sides.
 */
public final class ReleasedTypes {

    private static volatile Set<ResourceLocation> server = Set.of();
    private static volatile Set<ResourceLocation> client = Set.of();

    private ReleasedTypes() {
    }

    /** True when this mob's type is mapped by hand, so PtD's profile no longer decides its sequence or abilities. */
    public static boolean isReleased(Entity entity) {
        Set<ResourceLocation> released = entity.level().isClientSide() ? client : server;
        return !released.isEmpty() && released.contains(EntityType.getKey(entity.getType()));
    }

    public static Set<ResourceLocation> server() {
        return server;
    }

    /** False when nothing changed. */
    static boolean setServer(Set<ResourceLocation> released) {
        if (released.equals(server)) {
            return false;
        }
        server = Set.copyOf(released);
        // SeqMixin's profile sequence lives inside LOTM's cached Seq; drop every cached one.
        Pathways.changedAll();
        return true;
    }

    static void setClient(Set<ResourceLocation> released) {
        client = Set.copyOf(released);
    }
}
