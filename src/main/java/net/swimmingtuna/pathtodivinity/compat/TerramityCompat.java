package net.swimmingtuna.pathtodivinity.compat;

import net.mcreator.terramity.entity.UltraSnifferEntity;
import net.minecraft.world.entity.LivingEntity;

/** Only call when {@link ModCompat#TERRAMITY} is loaded. */
public final class TerramityCompat {

    private TerramityCompat() {
    }

    public static boolean isUltraSnifferPhaseTwo(LivingEntity living) {
        return living instanceof UltraSnifferEntity ultraSniffer
                && ultraSniffer.getEntityData().get(UltraSnifferEntity.DATA_phase_two);
    }
}
