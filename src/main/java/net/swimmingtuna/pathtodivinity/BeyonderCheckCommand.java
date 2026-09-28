package net.swimmingtuna.pathtodivinity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfiles;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Extends LOTM's {@code /beyonder check <player>} to any entity:
 * <ul>
 *   <li>{@code /beyonder check} - the entity you are looking at (up to {@value #LOOK_RANGE} blocks)</li>
 *   <li>{@code /beyonder check <targets>} - any entity selector, like {@code /kill}</li>
 * </ul>
 *
 * <p>Brigadier merges this into LOTM's {@code beyonder -> check} node. When a plain player name
 * parses for both our {@code targets} and LOTM's {@code player} argument, the argument registered
 * first wins, so {@link PTDCommands} registers at HIGHEST priority; the output here is a superset
 * of LOTM's (name and sequence name). The root {@code beyonder} node keeps whichever requirement
 * was registered first, so this uses the same op level (2) as LOTM to avoid changing it.
 */
public class BeyonderCheckCommand {

    private static final double LOOK_RANGE = 64.0;
    private static final SimpleCommandExceptionType NOT_LOOKING_AT_ENTITY = new SimpleCommandExceptionType(
            Component.literal("You are not looking at an entity (range " + (int) LOOK_RANGE + " blocks)"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyonder")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("check")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Entity target = lookedAtEntity(player);
                            if (target == null) {
                                throw NOT_LOOKING_AT_ENTITY.create();
                            }
                            return check(context.getSource(), List.of(target));
                        })
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> check(context.getSource(), EntityArgument.getEntities(context, "targets"))))));
    }

    private static int check(CommandSourceStack source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        for (Entity target : targets) {
            source.sendSuccess(() -> describe(target), false);
        }
        return targets.size();
    }

    @Nullable
    private static Entity lookedAtEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(LOOK_RANGE));
        // Stop at the first block in the way so entities behind walls aren't picked.
        HitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(LOOK_RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(end));
        return entityHit == null ? null : entityHit.getEntity();
    }

    private static Component describe(Entity entity) {
        MutableComponent message = Component.literal(entity.getName().getString()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (" + EntityType.getKey(entity.getType()) + ")").withStyle(ChatFormatting.GRAY));
        if (!(entity instanceof LivingEntity living)) {
            return message.append(Component.literal("\n  Not a living entity").withStyle(ChatFormatting.GRAY));
        }

        BeyonderClass pathway = BeyonderUtil.getPathway(living);
        BeyonderProfile profile = BeyonderProfiles.find(living);
        if (pathway == null) {
            message.append(Component.literal("\n  Not a Beyonder").withStyle(ChatFormatting.RED));
            if (profile != null && profile.sequence() != null) {
                message.append(Component.literal(" (PtD sequence " + profile.sequence() + ", no LOTM pathway in its profile)")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            int sequence = BeyonderUtil.getSequence(living);
            List<Component> sequenceNames = pathway.sequenceNames();
            MutableComponent line = Component.literal("\n  Pathway: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(BeyonderUtil.getPathwayName(pathway)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  Sequence: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.valueOf(sequence)).withStyle(ChatFormatting.WHITE));
            if (sequence >= 0 && sequence < sequenceNames.size()) {
                line.append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
                        .append(sequenceNames.get(sequence).copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
            }
            message.append(line);
            if (profile != null && !profile.usesAbilities()) {
                message.append(Component.literal("\n  Abilities: off").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" (ability_sequence -1 in " + profile.id() + ")").withStyle(ChatFormatting.GRAY)));
            }
            message.append(Component.literal("\n  Spirituality: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(BeyonderUtil.getSpirituality(living) + " / " + BeyonderUtil.getMaxSpirituality(living)).withStyle(ChatFormatting.WHITE)));
        }

        message.append(Component.literal("\n  Health: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.format("%.1f / %.1f", living.getHealth(), living.getMaxHealth())).withStyle(ChatFormatting.WHITE)));

        if (living instanceof Mob) {
            boolean boss = PTDUtil.isBeyonderEntity(living);
            MutableComponent line = Component.literal("\n  PtD boss: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(boss ? "yes" : "no").withStyle(boss ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            if (living.getPersistentData().contains("PTDDamageMultiplier")) {
                line.append(Component.literal("  Damage multiplier: ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(String.format("%.2fx", living.getPersistentData().getDouble("PTDDamageMultiplier"))).withStyle(ChatFormatting.WHITE));
            }
            message.append(line);
        }
        return message;
    }
}
