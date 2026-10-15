package net.swimmingtuna.pathtodivinity.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.item.BeyonderAbilities.SimpleAbilityItem;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.PTDMixinPlugin;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import net.swimmingtuna.pathtodivinity.combat.CombatTag;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.gating.SequenceGates;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Behaviour that must hold however the internals change. Run with
 * {@code ./gradlew runGameTestServer} (all mods) or {@code ./gradlew runGameTestServer -PlotmOnly}.
 * Tests only run when the {@code pathtodivinity} GameTest namespace is enabled, as in those runs.
 */
@GameTestHolder(PTD.MOD_ID)
@PrefixGameTestTemplate(false)
public class PTDGameTests {

    private static final String EMPTY = "empty";
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    @GameTest(template = EMPTY)
    public static void combatTagsVictimAndAttacker(GameTestHelper helper) {
        Zombie victim = helper.spawn(EntityType.ZOMBIE, MIDDLE);
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, MIDDLE.east());
        victim.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);
        helper.assertTrue(CombatTag.isInCombat(victim), "victim should be in combat");
        helper.assertTrue(CombatTag.isInCombat(attacker) == PTDServerConfig.COMBAT_TAG_ATTACKER.get(),
                "attacker tagged only when [combat] tag_attacker is on");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void nameTagDoesNotMakeABoss(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE);
        zombie.setCustomName(Component.literal("vessel of calamity"));
        helper.assertFalse(PTDUtil.isBeyonderEntity(zombie), "a name-tagged zombie must not count as a boss");
        helper.assertFalse(BeyonderUtil.getSequence(zombie) == 3, "a name-tagged zombie must not get the Vessel's sequence");
        helper.succeed();
    }

    /** The Elder Guardian is a vanilla boss with a default profile, so it is testable with any mod set. */
    @GameTest(template = EMPTY)
    public static void elderGuardianIsSequence8Boss(GameTestHelper helper) {
        ElderGuardian guardian = helper.spawn(EntityType.ELDER_GUARDIAN, MIDDLE);
        helper.assertTrue(PTDUtil.isBeyonderEntity(guardian), "Elder Guardian should be a boss");
        helper.assertTrue(BeyonderUtil.getSequence(guardian) == 8, "Elder Guardian should be Sequence 8");
        double expectedDamage = 1.3 * PTDServerConfig.DAMAGE_MULTIPLIER.get();
        double actualDamage = guardian.getPersistentData().getDouble("PTDDamageMultiplier");
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1e-6,
                "Elder Guardian damage multiplier should be " + expectedDamage + " but was " + actualDamage);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void profileLoadedFromDatapack(GameTestHelper helper) {
        ElderGuardian guardian = helper.spawn(EntityType.ELDER_GUARDIAN, MIDDLE);
        BeyonderProfile profile = BeyonderProfiles.find(guardian);
        helper.assertTrue(profile != null, "the default Elder Guardian profile should be loaded");
        helper.assertTrue(profile.boss() && Integer.valueOf(8).equals(profile.sequence()), "profile should be a Sequence 8 boss");
        helper.assertTrue(Double.valueOf(1.3).equals(profile.damageMultiplier()), "profile damage multiplier should be 1.3");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE.east());
        helper.assertTrue(BeyonderProfiles.find(zombie) == null, "a zombie has no profile");
        helper.succeed();
    }

    /** Uses any item a datapack gates at Sequence 4; passes trivially when there is none (e.g. -PlotmOnly). */
    @GameTest(template = EMPTY)
    public static void sequenceGateNeedsSequence4(GameTestHelper helper) {
        Optional<Item> gated = BuiltInRegistries.ITEM.stream()
                .filter(item -> SequenceGates.requiredSequence(new ItemStack(item)) == 4)
                .findFirst();
        if (gated.isEmpty()) {
            helper.succeed();
            return;
        }
        ItemStack stack = new ItemStack(gated.get());
        // A FakePlayer, not makeMockServerPlayerInLevel(): the mock has no network channel, and mods
        // that send packets to joining players crash it.
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        helper.assertFalse(SequenceGates.canUse(player, stack), "a player without a pathway must be blocked");
        var holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        holder.setPathwayAndSequenceNoSpirituality(BeyonderClassInit.SAILOR.get(), 5);
        helper.assertFalse(SequenceGates.canUse(player, stack), "Sequence 5 must be blocked");
        holder.setPathwayAndSequenceNoSpirituality(BeyonderClassInit.SAILOR.get(), 4);
        helper.assertTrue(SequenceGates.canUse(player, stack), "Sequence 4 must be allowed");
        player.setGameMode(GameType.CREATIVE);
        holder.setPathwayAndSequenceNoSpirituality(BeyonderClassInit.SAILOR.get(), 9);
        helper.assertTrue(SequenceGates.canUse(player, stack), "creative players bypass gates");
        // The FakePlayer is shared by the level: leave it as it was.
        holder.removeCurrentClass();
        player.setGameMode(GameType.SURVIVAL);
        helper.succeed();
    }

    /** [items] banned_abilities blocks casting and passives, but never deletes the item. */
    @GameTest(template = EMPTY)
    public static void bannedAbilityIsBlockedButKept(GameTestHelper helper) {
        SimpleAbilityItem ability = (SimpleAbilityItem) BeyonderUtil.getAbilities().getFirst();
        List<? extends String> before = PTDServerConfig.BANNED_ABILITIES.get();
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE);
        try {
            PTDServerConfig.BANNED_ABILITIES.set(List.of(BuiltInRegistries.ITEM.getKey(ability).toString()));
            PTDServerConfig.rebuildBannedAbilities();
            helper.assertTrue(PTDUtil.isAbilityBanned(ability), "the ability should be banned");
            for (LivingEntity living : List.of(player, zombie)) {
                helper.assertFalse(BeyonderUtil.canUseAbility(living, ability), "canUseAbility must be false while banned");
                helper.assertFalse(ability.checkIfCanUseAbility(living), "checkIfCanUseAbility must be false while banned");
            }
            // The FakePlayer has no pathway, so without the ban inventoryTick would take the item away.
            ItemStack stack = new ItemStack(ability);
            player.getInventory().setItem(0, stack);
            ability.inventoryTick(stack, helper.getLevel(), player, 0, false);
            helper.assertTrue(player.getInventory().getItem(0).is(ability), "a banned ability must stay in the inventory");
        } finally {
            player.getInventory().setItem(0, ItemStack.EMPTY);
            PTDServerConfig.BANNED_ABILITIES.set(before);
            PTDServerConfig.rebuildBannedAbilities();
        }
        helper.assertFalse(PTDUtil.isAbilityBanned(ability), "unbanning should lift the ban");
        helper.succeed();
    }

    /**
     * Every selected mixin applies to its target. Most targets load lazily, so a mixin whose injection point
     * a mod update removed would otherwise only crash a player's game (as LOTM 2.5.3 did to
     * LotmJoinModifiersMixin); audit() loads them all now.
     */
    @GameTest(template = EMPTY)
    public static void everyMixinApplies(GameTestHelper helper) {
        MixinEnvironment.getCurrentEnvironment().audit();
        helper.assertTrue(PTDMixinPlugin.selectedMixinCount() > 0, "the mixin plugin recorded no mixins");
        PTD.LOGGER.info("Mixin audit: {} selected mixins", PTDMixinPlugin.selectedMixinCount());
        Set<String> missing = PTDMixinPlugin.unappliedMixins();
        helper.assertTrue(missing.isEmpty(), "mixins that did not apply: " + missing);
        helper.succeed();
    }
}
