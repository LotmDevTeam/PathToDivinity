package net.swimmingtuna.pathtodivinity.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.compat.TerramityCompat;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Behaviour too specific for profile JSON, referenced by a profile's {@code "special"} field.
 */
public final class SpecialHandlers {

    public interface SpecialHandler {
        /** Replaces the standard health scaling; return false to use it. */
        default boolean applyHealth(LivingEntity living, double multiplier) {
            return false;
        }

        default void tick(LivingEntity living) {
        }
    }

    private static final Map<ResourceLocation, SpecialHandler> HANDLERS = Map.of(
            new ResourceLocation(PTD.MOD_ID, "ultra_sniffer"), new UltraSniffer(),
            new ResourceLocation(PTD.MOD_ID, "super_sniffer"), new SuperSniffer());

    private SpecialHandlers() {
    }

    @Nullable
    public static SpecialHandler get(@Nullable ResourceLocation id) {
        return id == null ? null : HANDLERS.get(id);
    }

    public static boolean exists(ResourceLocation id) {
        return HANDLERS.containsKey(id);
    }

    /**
     * Nearest player within 50 blocks who isn't in creative or spectator, or null. Only searches once a
     * second: the query isn't cheap, and a one second acquisition delay is imperceptible.
     */
    @Nullable
    static Player nearestTargetablePlayer(Mob mob) {
        if (mob.tickCount % 20 != 0) {
            return null;
        }
        return mob.level().getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 50, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    /** Terramity's Super Sniffer hunts the nearest player instead of waiting to be found. */
    static final class SuperSniffer implements SpecialHandler {
        @Override
        public void tick(LivingEntity living) {
            if (living instanceof Mob sniffer && sniffer.getTarget() == null) {
                Player target = nearestTargetablePlayer(sniffer);
                if (target != null) {
                    sniffer.setTarget(target);
                }
            }
        }
    }

    /**
     * Terramity's Ultra Sniffer: hunts the nearest player, is fully healed while spawning and for the
     * first 3 ticks of its second phase, and is tuned per LOTM pathway (its pathway is rolled from the
     * profile's pool).
     */
    static final class UltraSniffer implements SpecialHandler {
        @Override
        public boolean applyHealth(LivingEntity living, double multiplier) {
            if (!living.getPersistentData().getBoolean("maxHealthMultiplied")) {
                float multiplierAmount = (float) (multiplier * PTDServerConfig.HEALTH_MULTIPLIER.get());
                AttributeInstance maxHealthAttribute = living.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttribute != null) {
                    double newMaxHealth = maxHealthAttribute.getBaseValue() * multiplierAmount;
                    maxHealthAttribute.setBaseValue(newMaxHealth + 1);
                    living.setHealth((float) newMaxHealth);
                }
                living.getPersistentData().putBoolean("maxHealthMultiplied", true);
                LOTM.LOGGER.info("Multiplied {}'s health by {}", living.getName().getString(), multiplier);
            }
            return true;
        }

        @Override
        public void tick(LivingEntity living) {
            if (!(living instanceof Mob ultraSniffer)) {
                return;
            }
            if (ultraSniffer.getTarget() == null) {
                Player target = nearestTargetablePlayer(ultraSniffer);
                if (target != null) {
                    ultraSniffer.setTarget(target);
                }
            }
            float health = ultraSniffer.getHealth();
            if (Float.isNaN(health) || health < 0.0F) {
                ultraSniffer.setHealth(0.0F);
            }
            if (TerramityCompat.isUltraSnifferPhaseTwo(ultraSniffer)) {
                int fullHeal = ultraSniffer.getPersistentData().getInt("PtDFullHeal");
                if (fullHeal < 3) {
                    ultraSniffer.getPersistentData().putInt("PtDFullHeal", fullHeal + 1);
                    ultraSniffer.setHealth(ultraSniffer.getMaxHealth());
                    ultraSniffer.getPersistentData().putInt("age", 0);
                }
            } else if (ultraSniffer.tickCount <= 100) {
                ultraSniffer.setHealth(ultraSniffer.getMaxHealth());
                ultraSniffer.getPersistentData().putInt("age", 0);
            }
            if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.SPECTATOR.get())) {
                Scaling.multiplyDamage(living, 0.8);
            } else if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.SAILOR.get())) {
                Scaling.multiplyDamage(living, 0.9);
            } else if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.MONSTER.get())) {
                if (!ultraSniffer.getPersistentData().getBoolean("PtDGaveLuck")) {
                    ultraSniffer.getPersistentData().putDouble("luck", 5000);
                    ultraSniffer.getPersistentData().putBoolean("PtDGaveLuck", true);
                }
            }
        }
    }
}
