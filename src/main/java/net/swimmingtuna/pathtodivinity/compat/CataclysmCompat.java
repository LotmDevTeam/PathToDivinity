package net.swimmingtuna.pathtodivinity.compat;

import com.github.L_Ender.cataclysm.entity.effect.Sandstorm_Entity;
import com.github.L_Ender.cataclysm.entity.effect.Void_Vortex_Entity;
import com.github.L_Ender.cataclysm.entity.effect.Wave_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Cursed_Sandstorm_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Phantom_Halberd_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Tidal_Tentacle_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Wither_Howitzer_Entity;
import com.github.L_Ender.cataclysm.entity.projectile.Wither_Missile_Entity;
import com.github.L_Ender.cataclysm.init.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

/** Only call when {@link ModCompat#CATACLYSM} is loaded. */
public final class CataclysmCompat {

    private CataclysmCompat() {
    }

    /** Damage multiplier for player-owned Cataclysm weapon projectiles, 1.0 for anything else. */
    public static float projectileDamageMultiplier(Entity directSource) {
        if (directSource instanceof Tidal_Tentacle_Entity tidalTentacleEntity) {
            if (tidalTentacleEntity.getCreatorEntity() != null && tidalTentacleEntity.getCreatorEntity() instanceof Player) {
                return PTDBalance.TIDAL_TENTACLE_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Wither_Howitzer_Entity projectile) {
            if (projectile.getOwner() != null && projectile.getOwner() instanceof Player player) {
                boolean hasVoidAssault = false;
                for (ItemStack itemStack : player.getInventory().items) {
                    if (itemStack.getItem() == ModItems.VOID_ASSULT_SHOULDER_WEAPON.get()) {
                        hasVoidAssault = true;
                        break;
                    }
                }
                return (hasVoidAssault ? PTDBalance.WITHER_HOWITZER_VOID_ASSAULT_HIT_MULTIPLIER : PTDBalance.WITHER_HOWITZER_HIT_MULTIPLIER).scale(1.0f);
            }
        } else if (directSource instanceof Wither_Missile_Entity projectile) {
            if (projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return PTDBalance.WITHER_MISSILE_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Sandstorm_Entity projectile) {
            if (projectile.getCaster() != null && projectile.getCaster() instanceof Player) {
                return PTDBalance.SANDSTORM_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Phantom_Halberd_Entity projectile) {
            if (projectile.getCaster() != null && projectile.getCaster() instanceof Player) {
                return PTDBalance.PHANTOM_HALBERD_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Wave_Entity projectile) {
            if (projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return PTDBalance.WAVE_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Void_Vortex_Entity projectile) {
            if (projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return PTDBalance.VOID_VORTEX_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (directSource instanceof Cursed_Sandstorm_Entity projectile) {
            if (projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return PTDBalance.CURSED_SANDSTORM_HIT_MULTIPLIER.scale(1.0f);
            }
        }
        return 1.0f;
    }
}
