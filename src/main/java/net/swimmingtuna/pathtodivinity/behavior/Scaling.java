package net.swimmingtuna.pathtodivinity.behavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;

/**
 * Health and damage scaling for bosses, stored on each mob so it survives reloads: the
 * {@code maxHealthMultiplied}/{@code damageMultiplied} flags make each apply once per mob, and
 * {@code PTDDamageMultiplier} is read by the damage handler. The global [scaling] multipliers from
 * the server config are applied on top.
 */
public final class Scaling {

    public static final String DAMAGE_MULTIPLIER_KEY = "PTDDamageMultiplier";

    private Scaling() {
    }

    public static void multiplyMaxHealth(LivingEntity living, double multiplier) {
        if (!living.getPersistentData().getBoolean("maxHealthMultiplied")) {
            float multiplierAmount = (float) (multiplier * PTDServerConfig.HEALTH_MULTIPLIER.get());
            AttributeInstance maxHealthAttribute = living.getAttribute(Attributes.MAX_HEALTH);
            // Scale the base value: using getMaxHealth() baked any existing attribute modifiers into
            // the base, and above the 10000 cap the base was left alone but health was still rescaled.
            if (maxHealthAttribute != null && living.getMaxHealth() < 10000) {
                maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() * multiplierAmount);
                living.setHealth(living.getMaxHealth());
            }
            living.getPersistentData().putBoolean("maxHealthMultiplied", true);
            LOTM.LOGGER.info("Multiplied {}'s health by {}", living.getName().getString(), multiplier);
        }
    }

    public static void multiplyDamage(LivingEntity entity, double multiplier) {
        if (!entity.getPersistentData().getBoolean("damageMultiplied")) {
            CompoundTag tag = entity.getPersistentData();
            tag.putDouble(DAMAGE_MULTIPLIER_KEY, multiplier * PTDServerConfig.DAMAGE_MULTIPLIER.get());
            entity.getPersistentData().putBoolean("damageMultiplied", true);
            LOTM.LOGGER.info("Multiplied {}'s damage by {}", entity.getName().getString(), multiplier);
        }
    }

    /**
     * One-time buff when a boss enters its second phase. multiplyMaxHealth/multiplyDamage can't be
     * used here: they only ever apply once per mob, and the join-time scaling has already used that up.
     * Health keeps the same percentage of the new maximum.
     */
    public static void applyPhaseTwoBuff(LivingEntity living, double healthMultiplier, double damageMultiplier) {
        CompoundTag tag = living.getPersistentData();
        if (tag.getBoolean("PTDPhaseTwoBuffed")) {
            return;
        }
        tag.putBoolean("PTDPhaseTwoBuffed", true);
        AttributeInstance maxHealthAttribute = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            float healthFraction = living.getHealth() / living.getMaxHealth();
            maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() * healthMultiplier * PTDServerConfig.HEALTH_MULTIPLIER.get());
            living.setHealth(living.getMaxHealth() * healthFraction);
        }
        tag.putDouble(DAMAGE_MULTIPLIER_KEY, damageMultiplier * PTDServerConfig.DAMAGE_MULTIPLIER.get());
        LOTM.LOGGER.info("Applied phase two buff to {}", living.getName().getString());
    }
}
