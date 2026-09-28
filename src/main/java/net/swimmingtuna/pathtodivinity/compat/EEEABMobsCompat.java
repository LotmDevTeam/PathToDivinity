package net.swimmingtuna.pathtodivinity.compat;

import com.eeeab.eeeabsmobs.sever.entity.effect.EntityGuardianLaser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

/** Only call when {@link ModCompat#EEEABS_MOBS} is loaded. */
public final class EEEABMobsCompat {

    private EEEABMobsCompat() {
    }

    /** Weakens the Nameless Guardian's laser when fired by a mob, 1.0 for anything else. */
    public static float projectileDamageMultiplier(Entity directSource) {
        if (directSource instanceof EntityGuardianLaser projectile) {
            if (projectile.getOwner() != null && !(projectile.getOwner() instanceof Player)) {
                return PTDBalance.GUARDIAN_LASER_MOB_HIT_MULTIPLIER.scale(1.0f);
            }
        }
        return 1.0f;
    }
}
