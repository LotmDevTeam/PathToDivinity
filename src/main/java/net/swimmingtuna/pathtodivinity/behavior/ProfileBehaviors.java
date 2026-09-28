package net.swimmingtuna.pathtodivinity.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.swimmingtuna.lotm.entity.Mobs.PlayerMobEntity;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderRegistrations;
import net.swimmingtuna.pathtodivinity.beyonders.EntityMatcher;

/**
 * What a boss profile does to its mob, apart from the lookups other code makes directly
 * (sequence, boss flag, LOTM registration).
 */
public final class ProfileBehaviors {

    /** Effects are refreshed this often, lasting twice as long, so they never lapse. */
    private static final int EFFECT_REFRESH_TICKS = 20;
    private static final int PHASE_CHECK_TICKS = 40;

    private ProfileBehaviors() {
    }

    /** When the mob joins a level, including when it is loaded from disk (scaling applies once). */
    public static void onJoin(LivingEntity living, BeyonderProfile profile) {
        if (profile.match().kind() != EntityMatcher.Kind.ENTITY && !profile.pathways().isEmpty()
                && living.level().getServer() != null) {
            BeyonderRegistrations.registerRuleMatch(living.level().getServer(), living.getType(), profile);
        }
        if (profile.healthMultiplier() != null) {
            SpecialHandlers.SpecialHandler special = SpecialHandlers.get(profile.special());
            if (special == null || !special.applyHealth(living, profile.healthMultiplier())) {
                Scaling.multiplyMaxHealth(living, profile.healthMultiplier());
            }
        }
        if (profile.damageMultiplier() != null) {
            Scaling.multiplyDamage(living, profile.damageMultiplier());
        }
        if (!profile.persistentData().isEmpty()) {
            living.getPersistentData().merge(profile.persistentData().copy());
        }
    }

    public static void onTick(LivingEntity living, BeyonderProfile profile) {
        int tick = living.tickCount;
        if (!profile.effects().isEmpty() && tick % EFFECT_REFRESH_TICKS == 0) {
            for (BeyonderProfile.Effect effect : profile.effects()) {
                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect.effect());
                if (mobEffect != null) {
                    living.addEffect(new MobEffectInstance(mobEffect, EFFECT_REFRESH_TICKS * 2, effect.amplifier(), false, false));
                }
            }
        }
        BeyonderProfile.Allies allies = profile.allies();
        if (allies != null && tick % allies.intervalTicks() == 0) {
            for (Mob mob : living.level().getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(allies.radius()))) {
                if (mob == living) {
                    continue;
                }
                boolean ally = allies.allMobs()
                        ? !(mob instanceof PlayerMobEntity)
                        : allies.entities().contains(EntityType.getKey(mob.getType()));
                if (ally) {
                    BeyonderUtil.forceAlly(mob, living);
                }
            }
        }
        if (profile.phaseTwo() != null && tick % PHASE_CHECK_TICKS == 0 && PhaseDetectors.isPhaseTwo(living)) {
            Scaling.applyPhaseTwoBuff(living, profile.phaseTwo().healthMultiplier(), profile.phaseTwo().damageMultiplier());
        }
        SpecialHandlers.SpecialHandler special = SpecialHandlers.get(profile.special());
        if (special != null) {
            special.tick(living);
        }
    }

    /** Extra drops: never despawn, can be picked up at once. Items of missing mods are skipped. */
    public static void onDeath(LivingEntity living, BeyonderProfile profile) {
        for (BeyonderProfile.Drop drop : profile.drops()) {
            ResourceLocation itemId = drop.item();
            Item item = ForgeRegistries.ITEMS.containsKey(itemId) ? ForgeRegistries.ITEMS.getValue(itemId) : null;
            if (item == null) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(living.level(), living.getX(), living.getY(), living.getZ(), new ItemStack(item, drop.count()));
            itemEntity.setNoPickUpDelay();
            itemEntity.teleportTo(living.getX(), living.getY(), living.getZ());
            itemEntity.setUnlimitedLifetime();
            living.level().addFreshEntity(itemEntity);
        }
    }
}
