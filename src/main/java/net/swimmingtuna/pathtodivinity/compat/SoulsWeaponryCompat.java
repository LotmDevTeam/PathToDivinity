package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.world.entity.LivingEntity;
import net.soulsweaponry.entity.mobs.DayStalker;
import net.soulsweaponry.entity.mobs.NightProwler;

/** Only call when {@link ModCompat#SOULS_WEAPONRY} is loaded. */
public final class SoulsWeaponryCompat {

    private SoulsWeaponryCompat() {
    }

    public static boolean isPhaseTwo(LivingEntity living) {
        if (living instanceof DayStalker dayStalker) {
            return dayStalker.isPhaseTwo();
        }
        if (living instanceof NightProwler nightProwler) {
            return nightProwler.isPhaseTwo();
        }
        return false;
    }
}
