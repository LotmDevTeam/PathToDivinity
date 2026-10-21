package net.swimmingtuna.pathtodivinity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.commands.BeyonderClassArgument;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /beyonder summon <pathway> <sequence> <entity> <location>}: spawns one mob that fights as a Beyonder
 * of that pathway and sequence, leaving every other mob of its type alone (unlike {@code /beyonderentity},
 * which registers the whole type).
 *
 * <p>The pathway is stored on the mob itself, so it survives unloading and restarts. LOTM reads it through
 * {@code BeyonderEntityDataMixin} and {@code BeyonderUtilMixin}. Server side only: clients are not told, so
 * anything LOTM draws from the client's copy of the type registrations won't show it.
 */
public class BeyonderSummonCommand {

    public static final String PATHWAY_KEY = "ptdBeyonderPathway";
    public static final String SEQUENCE_KEY = "ptdBeyonderSequence";

    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
            Component.translatable("commands.summon.invalidPosition"));
    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
            Component.translatable("commands.summon.failed"));
    private static final SimpleCommandExceptionType NOT_LIVING = new SimpleCommandExceptionType(
            Component.literal("Only living entities can be Beyonders."));
    private static final SimpleCommandExceptionType DUPLICATE_UUID = new SimpleCommandExceptionType(
            Component.translatable("commands.summon.failed.uuid"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("beyonder")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon")
                        .then(Commands.argument("pathway", BeyonderClassArgument.beyonderClass())
                                .then(Commands.argument("sequence", IntegerArgumentType.integer(0, 9))
                                        .then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                .then(Commands.argument("location", Vec3Argument.vec3())
                                                        .executes(context -> summon(context.getSource(),
                                                                BeyonderClassArgument.getBeyonderClass(context, "pathway"),
                                                                IntegerArgumentType.getInteger(context, "sequence"),
                                                                ResourceArgument.getSummonableEntityType(context, "entity"),
                                                                Vec3Argument.getVec3(context, "location")))))))));
    }

    /** Mirrors vanilla {@code /summon}, with the pathway written before the mob joins the world. */
    private static int summon(CommandSourceStack source, BeyonderClass pathway, int sequence,
                              Holder.Reference<EntityType<?>> type, Vec3 pos) throws CommandSyntaxException {
        if (!Level.isInSpawnableBounds(BlockPos.containing(pos))) {
            throw INVALID_POSITION.create();
        }
        ServerLevel level = source.getLevel();
        CompoundTag tag = new CompoundTag();
        tag.putString("id", type.key().location().toString());
        Entity entity = EntityType.loadEntityRecursive(tag, level, loaded -> {
            loaded.moveTo(pos.x, pos.y, pos.z, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (entity == null) {
            throw FAILED.create();
        }
        if (!(entity instanceof LivingEntity living)) {
            throw NOT_LIVING.create();
        }
        living.getPersistentData().putString(PATHWAY_KEY, BeyonderClassInit.getRegistry().getKey(pathway).toString());
        living.getPersistentData().putInt(SEQUENCE_KEY, sequence);
        if (living instanceof Mob mob) {
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
        }
        if (!level.tryAddFreshEntityWithPassengers(living)) {
            throw DUPLICATE_UUID.create();
        }
        source.sendSuccess(() -> Component.literal("Summoned ").append(living.getDisplayName())
                .append(" as a " + BeyonderUtil.getPathwayName(pathway) + " Beyonder, Sequence " + sequence), true);
        return 1;
    }

    /** The pathway and sequence {@code /beyonder summon} gave this entity, or null for any other entity. */
    @Nullable
    public static BeyonderUtil.PathwayAndSequence summonedAs(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(PATHWAY_KEY)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.getString(PATHWAY_KEY));
        BeyonderClass pathway = id == null ? null : BeyonderClassInit.getRegistry().get(id);
        return pathway == null ? null : new BeyonderUtil.PathwayAndSequence(pathway, data.getInt(SEQUENCE_KEY));
    }
}
