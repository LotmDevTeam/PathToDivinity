package net.swimmingtuna.pathtodivinity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Marks the window in which {@code BeyonderEntityData.selectAndUseAbility} is building the
 * ability list for a mob.
 *
 * <p>While that window is open, {@code BeyonderUtilMixin}'s custom sequence map is bypassed so
 * that LOTM's own resolution runs instead, i.e. the sequence registered for the entity type via
 * {@code /beyonderentity add <entity> <pathway> <sequence>}. Everything else that asks for a
 * sequence (spirituality regen, ability cooldowns, damage scaling, other mods' logic) keeps
 * seeing the mapped sequence from {@code BeyonderUtilMixin}.
 *
 * <p>The entity is stored rather than a plain flag so only queries about the mob whose abilities
 * are being picked are affected - a query about its target (a player, another mob) is untouched.
 */
public final class MobAbilitySequenceContext {

    private static final ThreadLocal<LivingEntity> SELECTING_ABILITIES_FOR = new ThreadLocal<>();

    private MobAbilitySequenceContext() {
    }

    public static void begin(LivingEntity entity) {
        SELECTING_ABILITIES_FOR.set(entity);
    }

    public static void end() {
        SELECTING_ABILITIES_FOR.remove();
    }

    public static boolean isSelectingAbilitiesFor(LivingEntity entity) {
        return entity != null && SELECTING_ABILITIES_FOR.get() == entity;
    }
}
