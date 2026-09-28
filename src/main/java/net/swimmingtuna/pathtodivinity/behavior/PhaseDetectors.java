package net.swimmingtuna.pathtodivinity.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.pathtodivinity.compat.ModCompat;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import net.swimmingtuna.pathtodivinity.compat.SoulsWeaponryCompat;
import net.swimmingtuna.pathtodivinity.compat.TerramityCompat;

import java.util.Set;

/**
 * Which bosses have a second phase PtD can detect; a profile's {@code phase_two} only works for these.
 * Each check reads the mob's own mod classes, so it is only called once the entity id (and so its mod)
 * is known to be present.
 */
public final class PhaseDetectors {

    private static final Set<ResourceLocation> SUPPORTED = Set.of(
            PTDEntities.DAY_STALKER, PTDEntities.NIGHT_PROWLER, PTDEntities.ULTRA_SNIFFER);

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
        return switch (id.getNamespace()) {
            case ModCompat.SOULS_WEAPONRY -> SoulsWeaponryCompat.isPhaseTwo(living);
            case ModCompat.TERRAMITY -> TerramityCompat.isUltraSnifferPhaseTwo(living);
            default -> false;
        };
    }
}
