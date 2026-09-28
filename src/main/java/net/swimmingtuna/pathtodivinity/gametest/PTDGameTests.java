package net.swimmingtuna.pathtodivinity.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import net.swimmingtuna.pathtodivinity.combat.CombatTag;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.gating.SequenceGates;

import java.util.Optional;

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
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
        helper.succeed();
    }
}
