package net.swimmingtuna.pathtodivinity.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;

import java.util.Set;

/**
 * Which bosses have a second phase PtD can detect; a profile's {@code phase_two} only works for these.
 * Each check reads the mob's own mod classes, so it is only called once the entity id (and so its mod)
 * is known to be present.
 *
 * <p>The bosses detected on Forge (Marium's Soulslike Weaponry's Day Stalker and Night Prowler, Terramity's
 * Ultra Sniffer) come from mods with no 1.21.1 NeoForge release, so none are detected yet.
 */
public final class PhaseDetectors {

    private static final Set<ResourceLocation> SUPPORTED = Set.of();

    private PhaseDetectors() {
    }

    public static boolean supports(ResourceLocation entityId) {
        return SUPPORTED.contains(entityId);
    }

    public static boolean isPhaseTwo(LivingEntity living) {
        ResourceLocation id = PTDEntities.idOf(living);
        if (!SUPPORTED.contains(id)) {
            return false;
        }
        return false;
    }
}
