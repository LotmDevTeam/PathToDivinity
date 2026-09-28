package net.swimmingtuna.pathtodivinity.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The combat timer, stored in each entity's persistent data. Players in combat can't use the
 * commands in {@code [combat] blocked_commands}; bosses in combat don't regenerate.
 */
public final class CombatTag {

    private static final String TIMER_KEY = "PTDCombatTimer";

    private CombatTag() {
    }

    public static int remainingTicks(Entity entity) {
        return entity.getPersistentData().getInt(TIMER_KEY);
    }

    public static boolean isInCombat(Entity entity) {
        return remainingTicks(entity) > 0;
    }

    public static void tag(Entity entity) {
        entity.getPersistentData().putInt(TIMER_KEY, PTDServerConfig.COMBAT_DURATION_TICKS.get());
    }

    /** Counts the timer down by one tick. */
    public static void tick(Entity entity) {
        int remaining = remainingTicks(entity);
        if (remaining > 0) {
            entity.getPersistentData().putInt(TIMER_KEY, remaining - 1);
        }
    }

    public static void onHurt(LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker != null || PTDServerConfig.COMBAT_TAG_ON_ENVIRONMENTAL_DAMAGE.get()) {
            tag(victim);
        }
        if (PTDServerConfig.COMBAT_TAG_ATTACKER.get() && attacker instanceof LivingEntity && attacker != victim) {
            tag(attacker);
        }
    }

    /**
     * The blocked command {@code command} starts with, or null.
     *
     * @param command lower case, without the leading slash
     */
    @Nullable
    public static String blockedCommand(String command) {
        if (!PTDServerConfig.COMBAT_LOCKS_COMMANDS.get()) {
            return null;
        }
        String name = command.split(" ", 2)[0];
        // "essentials:home" is the same command as "home".
        String unqualified = name.substring(name.indexOf(':') + 1);
        for (String blocked : PTDServerConfig.COMBAT_BLOCKED_COMMANDS.get()) {
            String normalized = blocked.toLowerCase(Locale.ROOT).trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (name.equals(normalized) || unqualified.equals(normalized)) {
                return unqualified;
            }
        }
        return null;
    }
}
