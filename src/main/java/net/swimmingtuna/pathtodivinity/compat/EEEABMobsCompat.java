package net.swimmingtuna.pathtodivinity.compat;

import com.eeeab.eeeabsmobs.sever.entity.effect.EntityGuardianLaser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Only call when {@link ModCompat#EEEABS_MOBS} is loaded. */
public final class EEEABMobsCompat {

    private EEEABMobsCompat() {
    }

    /** Weakens the Nameless Guardian's laser when fired by a mob, 1.0 for anything else. */
    public static float projectileDamageMultiplier(Entity directSource) {
        if (directSource instanceof EntityGuardianLaser projectile) {
            if (projectile.getOwner() != null && !(projectile.getOwner() instanceof Player)) {
                return 0.6f;
            }
        }
        return 1.0f;
    }
}
