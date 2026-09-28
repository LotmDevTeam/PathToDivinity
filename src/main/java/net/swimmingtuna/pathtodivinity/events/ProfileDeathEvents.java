package net.swimmingtuna.pathtodivinity.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.entity.Mobs.PlayerMobEntity;
import net.swimmingtuna.lotm.events.NewEventLoop.LayerClasses.SequenceRegressionProtectionLayer;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.init.ItemInit;
import net.swimmingtuna.lotm.item.BeyonderPotions.BeyonderCharacteristic;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.profile.PlayerProfileData;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

import javax.annotation.Nullable;

/**
 * Owns sequence regression and the Beyonder Characteristic drop on death while the profile system is enabled.
 *
 * <p>Normal profiles drop a sequence when another player kills them; Safemode profiles never do, on either side
 * of the kill. A Safemode player neither loses a sequence when killed nor takes one when killing, which is the
 * whole trade: Safemode gives up Sequence 0, half its ability damage against players and its own progression in
 * exchange for staying out of the regression game entirely. Regression by other means is untouched — notably
 * the Sun/Bard Sequence 4 purification, which regresses through {@code BeyonderHolder.setSequence} directly
 * rather than through death, and so keeps working against Safemode players. The same is true of any future
 * ability that states it can regress someone.
 *
 * <p>The characteristic drop lives inside the regression branch rather than beside it, so a Normal player killed
 * by a Normal player drops exactly one characteristic on exactly the deaths that cost them a sequence. Nothing
 * drops inside the ten minute grace window and nothing drops when {@code Normal Profile Regresses On Death} is
 * off — LOTM puts its own drop ahead of the equivalent check, which would let two players trade kills inside the
 * window and farm characteristics for free.
 *
 * <p>Runs at {@link EventPriority#HIGH}, which is a deliberately narrow window. LOTM cancels the death outright
 * in three {@code HIGHEST} handlers — {@code pallorDeathPrevention}, {@code wraithDeathToUnderworld} and
 * {@code inextinguishableLightDeathPrevention} — so regressing at {@code HIGHEST} would race them and could
 * take a sequence from a player who then survives (a Sequence 0 Sun using Inextinguishable Light, for
 * instance). Sitting at {@code HIGH} puts this after every canceller and still ahead of LOTM's own
 * {@code deathEvent}, which runs at {@code NORMAL}. Because {@code LivingDeathEvent} is cancellable and this
 * listener does not opt into receiving cancelled events, Forge skips it entirely when the death was prevented.
 *
 * <p>The Safemode-victim and Normal-regression branches finish by calling
 * {@link SequenceRegressionProtectionLayer#putRegressionTimer}, which does double duty: LOTM's handler skips its
 * regression block whenever that timer is non-zero, so this suppresses LOTM's path — preventing a double
 * regression if an operator ever turns on {@code Should Drop Characteristic} — while also granting the usual
 * grace window. The Safemode-killer branch deliberately does not, for the reason spelled out at its call site.
 *
 * <p>Always use LOTM's helper rather than writing the {@code sequenceRegressionProtectionTimer} NBT key by hand:
 * the helper also registers the tick layer that counts the timer back down. Setting the raw tag alone would
 * grant permanent immunity.
 *
 * <p>That LOTM timer cannot carry the grace window on its own, which is why the Normal-regression branch also
 * stamps one of its own through {@link ProfileManager#startRegressionGrace}. The timer lives in the player's
 * persistent data, and Forge carries only the {@code PlayerPersisted} subtag across a respawn — so the respawn
 * that follows a death erases the window that death just opened, and the victim can be killed again
 * immediately. Before the second stamp existed that was measured at eighteen seconds between regressions,
 * which made the drop farmable at one characteristic per kill. PTD's stamp lives in {@link PlayerProfileData}
 * instead, so it survives respawn, logout and restart. Both are checked before regressing.
 *
 * <p>The window's length is {@code Regression Grace Minutes}. Setting it to 0 turns it off outright — no
 * stamp, no check, every player kill regresses and drops — which is meant for testing, since it restores
 * exactly the farm the window exists to prevent. Safemode is unaffected either way; its suppression timer is
 * the constant below, not this setting.
 */
@Mod.EventBusSubscriber(modid = PTD.MOD_ID)
public class ProfileDeathEvents {

    /**
     * Minutes of LOTM regression suppression handed to a Safemode victim, matching LOTM's own value.
     *
     * <p>Deliberately a constant rather than {@code Regression Grace Minutes}: that setting tunes the Normal
     * profile's grace window and can be turned off for testing, and Safemode's exemption must never depend on
     * it. This is not a grace window at all — it is the timer that stops LOTM's own handler regressing someone
     * this class has just declared exempt.
     */
    private static final int SAFEMODE_SUPPRESSION_MINUTES = 10;

    private static final String PROTECTION_TIMER_KEY = "sequenceRegressionProtectionTimer";

    private static final String MONSTER_REINCARNATION_KEY = "monsterReincarnationCounter";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void applyProfileRegression(LivingDeathEvent event) {
        // Forge already skips cancelled events for this listener; this is belt-and-braces in case a future
        // change opts into receiving them, since regressing a death that never happens is unrecoverable.
        if (event.isCanceled() || !ProfileManager.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim) || victim.level().isClientSide()) {
            return;
        }

        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(victim);
        if (holder.getCurrentClass() == null || holder.getSequence() < 0) {
            return;
        }

        LivingEntity killer = resolvePlayerKiller(event.getSource());
        if (killer == null || killer == victim || BeyonderUtil.areAllies(killer, victim)) {
            return;
        }

        if (ProfileManager.getActiveProfile(victim) == ProfileType.SAFEMODE) {
            // Suppress LOTM's regression too, so the exemption holds no matter how lotm-common.toml is set.
            SequenceRegressionProtectionLayer.putRegressionTimer(victim, SAFEMODE_SUPPRESSION_MINUTES);
            victim.sendSystemMessage(Component.literal(
                            "Your Safemode profile protected your sequence from this death.")
                    .withStyle(ChatFormatting.AQUA));
            return;
        }

        // A Safemode player never takes a sequence, however the kill was delivered. No grace timer here, unlike
        // the branch above: the timer grants ten minutes of blanket immunity and the Normal path below is gated
        // on that same key, so setting it would let a player have a Safemode friend kill them to buy ten minutes
        // of protection from genuine PvP regression. The cost is that LOTM's own regression in
        // ModEvents.deathEvent is left unsuppressed — dead code while lotm-common.toml keeps
        // 'Should Drop Characteristic' false, but it would regress the victim behind this handler's back if an
        // operator ever turned it on.
        Player responsible = resolveResponsiblePlayer(killer);
        if (responsible != null && ProfileManager.isSafemode(responsible)) {
            victim.sendSystemMessage(Component.literal(
                            "You were killed by a Safemode player, so your sequence is safe.")
                    .withStyle(ChatFormatting.AQUA));
            responsible.sendSystemMessage(Component.literal(
                            "Your Safemode profile spared their sequence.")
                    .withStyle(ChatFormatting.AQUA));
            return;
        }

        if (!PTDServerConfig.NORMAL_PROFILE_REGRESSES.get()) {
            return;
        }
        // Still inside the grace window from a previous death. Leave both stamps alone so they keep counting
        // down from when they were set, and let LOTM print its own "saved by the protection timer" message.
        // Two sources, because they cover different things: PTD's own stamp survives the respawn that follows
        // the death that set it, which the LOTM key does not; the LOTM key still honours a timer granted by
        // some other route, such as an ability that hands out immunity. At 0 the operator has asked for no
        // post-death immunity at all, so neither is consulted.
        int graceMinutes = PTDServerConfig.REGRESSION_GRACE_MINUTES.get();
        if (graceMinutes > 0
                && (ProfileManager.getRemainingRegressionGraceTicks(
                        victim.getServer(), victim.getUUID(), graceMinutes) > 0
                || victim.getPersistentData().getInt(PROTECTION_TIMER_KEY) > 0)) {
            return;
        }

        int sequence = holder.getSequence();
        // Read before regression, so the characteristic carries the sequence they were killed at rather
        // than the weaker one they wake up on.
        dropCharacteristic(victim, holder.getCurrentClass(), sequence);
        if (sequence >= 9) {
            // resetPathway reaches BeyonderHolder.removePathway, which ends on
            // player.setHealth(player.getMaxHealth()). Landing mid-death that revives the victim: the client
            // already has the combat-kill packet and is showing the death screen, but vanilla's
            // PERFORM_RESPAWN handler ignores the request while getHealth() > 0, so Respawn does nothing and
            // they carry on playing having already dropped their loot. Putting the health back is enough to
            // let the death finish, because ServerPlayer.die never sets a flag of its own - it goes by health
            // alone. Not needed on the branch below; BeyonderUtil.setSequence never touches health.
            float healthAtDeath = victim.getHealth();
            BeyonderUtil.resetPathway(victim);
            victim.setHealth(healthAtDeath);
            victim.sendSystemMessage(Component.literal(
                            "You were killed at Sequence 9 and have lost your pathway.")
                    .withStyle(ChatFormatting.RED));
        } else {
            BeyonderUtil.setSequence(victim, sequence + 1);
            victim.sendSystemMessage(Component.literal(
                            "You were killed and have regressed to Sequence " + (sequence + 1) + ".")
                    .withStyle(ChatFormatting.RED));
        }
        if (graceMinutes > 0) {
            SequenceRegressionProtectionLayer.putRegressionTimer(victim, graceMinutes);
            // The LOTM timer above is what suppresses LOTM's own regression block, but it dies with the body it
            // is written on. This is the stamp that actually holds the window open across the respawn.
            ProfileManager.startRegressionGrace(victim.getServer(), victim.getUUID());
        }
    }

    /**
     * Spawns the victim's Beyonder Characteristic on the ground where they died.
     *
     * <p>Mirrors the drop in LOTM's {@code ModEvents.deathEvent}, including its reincarnation special case: a
     * victim carrying a {@code monsterReincarnationCounter} drops a Sequence 1 Monster characteristic rather than
     * one for the pathway they were nominally on, since that pathway is no longer what they are. The UUID stamped
     * on the stack is the victim's — it records whose characteristic this is, not who took it.
     *
     * <p>Dropped as a loose {@link ItemEntity} rather than handed to the killer, so it can be contested, missed or
     * destroyed like any other loot.
     */
    private static void dropCharacteristic(Player victim, BeyonderClass pathway, int sequence) {
        ItemStack stack = new ItemStack(ItemInit.BEYONDER_CHARACTERISTIC.get());
        if (victim.getPersistentData().getInt(MONSTER_REINCARNATION_KEY) >= 1) {
            BeyonderCharacteristic.setData(stack, BeyonderClassInit.MONSTER.get(), 1, victim.getUUID());
        } else {
            BeyonderCharacteristic.setData(stack, pathway, sequence, victim.getUUID());
        }
        victim.level().addFreshEntity(new ItemEntity(
                victim.level(), victim.getX(), victim.getY(), victim.getZ(), stack));
    }

    /**
     * The player-side attacker behind a death, or {@code null} when the kill was not a player's doing.
     *
     * <p>Mirrors LOTM 2.4.4's inline check: a {@link Player}, a {@link PlayerMobEntity} that has a creator, or a
     * projectile owned by either. LOTM 2.4.9 factors this out into {@code BeyonderUtil.isPlayerAttack}, which
     * does not exist in the version this mod compiles against.
     */
    @Nullable
    private static LivingEntity resolvePlayerKiller(DamageSource source) {
        LivingEntity fromAttacker = asPlayerKiller(source.getEntity());
        return fromAttacker != null ? fromAttacker : asPlayerKiller(source.getDirectEntity());
    }

    /**
     * The player whose profile answers for a kill, or {@code null} when no player does.
     *
     * <p>{@link #resolvePlayerKiller} stops at the {@link PlayerMobEntity} rather than its creator, because LOTM
     * treats the clone as the attacker for ally and damage purposes. Profiles are per-player, so a clone's kill
     * is credited to whoever summoned it.
     */
    @Nullable
    private static Player resolveResponsiblePlayer(LivingEntity killer) {
        if (killer instanceof Player player) {
            return player;
        }
        // getCreator() is typed LivingEntity, so a clone summoned by a mob narrows away here.
        if (killer instanceof PlayerMobEntity playerMob && playerMob.getCreator() instanceof Player creator) {
            return creator;
        }
        return null;
    }

    @Nullable
    private static LivingEntity asPlayerKiller(@Nullable Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof PlayerMobEntity playerMob) {
            return playerMob.getCreator() != null ? playerMob : null;
        }
        if (entity instanceof Projectile projectile) {
            return asPlayerKiller(projectile.getOwner());
        }
        return null;
    }
}
