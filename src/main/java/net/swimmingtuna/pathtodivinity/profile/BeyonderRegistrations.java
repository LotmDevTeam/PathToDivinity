package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.PTD;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps LOTM's {@code /beyonderentity} registrations (which pathway and sequence a mob fights with)
 * in step with the boss profiles.
 *
 * <p>PtD remembers, per world, which entity types it registered. A registration whose profile is gone
 * or no longer sets a pathway is removed again; registrations an admin made by hand for entities PtD
 * never touched are left alone.
 */
public final class BeyonderRegistrations {

    private BeyonderRegistrations() {
    }

    /**
     * @param firstRun true at server start: pathway pools pick a pathway. Afterwards a pool only
     *                 re-picks with {@code "reroll": "when_none_alive"} while none of that mob is loaded.
     */
    public static void apply(MinecraftServer server, boolean firstRun) {
        ServerLevel overworld = server.overworld();
        BeyonderEntityData lotmData = BeyonderEntityData.getInstance(overworld);
        OwnedRegistrations owned = OwnedRegistrations.get(overworld);
        Set<ResourceLocation> aliveTypes = firstRun ? Set.of() : loadedEntityTypes(server);

        Set<ResourceLocation> current = new HashSet<>();
        for (BeyonderProfile profile : BeyonderProfiles.server().exactProfiles()) {
            Integer sequence = profile.effectiveAbilitySequence();
            if (profile.pathways().isEmpty() || sequence == null) {
                continue;
            }
            ResourceLocation entityId = profile.match().entityId();
            EntityType<?> type = entityType(entityId);
            if (type == null) {
                continue; // its mod isn't installed
            }
            current.add(entityId);

            String registered = lotmData.getStringForEntity(type);
            String wanted = null;
            if (profile.pathways().size() == 1) {
                wanted = sequenceId(profile, profile.pathways().get(0), sequence);
            } else {
                boolean reroll = firstRun || registered == null
                        || (profile.reroll() == BeyonderProfile.Reroll.WHEN_NONE_ALIVE && !aliveTypes.contains(entityId));
                if (reroll) {
                    List<ResourceLocation> pool = profile.pathways();
                    wanted = sequenceId(profile, pool.get(ThreadLocalRandom.current().nextInt(pool.size())), sequence);
                } else {
                    wanted = registered;
                }
            }
            if (wanted != null && !wanted.equals(registered)) {
                lotmData.setEntityString(type, wanted);
            }
            owned.add(entityId);
        }

        for (ResourceLocation entityId : Set.copyOf(owned.entityIds)) {
            if (!current.contains(entityId)) {
                EntityType<?> type = entityType(entityId);
                if (type != null) {
                    lotmData.removeEntity(type);
                    PTD.LOGGER.info("Removed LOTM registration for {}: its boss profile no longer sets a pathway", entityId);
                }
                owned.remove(entityId);
            }
        }
    }

    @Nullable
    private static String sequenceId(BeyonderProfile profile, ResourceLocation pathwayId, int sequence) {
        BeyonderClass pathway = BeyonderClassInit.getRegistry().containsKey(pathwayId)
                ? BeyonderClassInit.getRegistry().getValue(pathwayId) : null;
        if (pathway == null) {
            PTD.LOGGER.warn("Boss profile {}: unknown pathway {}", profile.id(), pathwayId);
            return null;
        }
        return pathway.sequenceIds().get(sequence);
    }

    @Nullable
    private static EntityType<?> entityType(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    private static Set<ResourceLocation> loadedEntityTypes(MinecraftServer server) {
        Set<ResourceLocation> types = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                types.add(EntityType.getKey(entity.getType()));
            }
        }
        return types;
    }

    /** Entity types PtD registered with LOTM, saved with the overworld. */
    static final class OwnedRegistrations extends SavedData {

        private static final String NAME = "pathtodivinity_beyonder_registrations";
        private final Set<ResourceLocation> entityIds = new HashSet<>();

        static OwnedRegistrations get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(OwnedRegistrations::load, OwnedRegistrations::new, NAME);
        }

        private static OwnedRegistrations load(CompoundTag tag) {
            OwnedRegistrations data = new OwnedRegistrations();
            for (Tag element : tag.getList("entities", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    data.entityIds.add(id);
                }
            }
            return data;
        }

        void add(ResourceLocation id) {
            if (entityIds.add(id)) {
                setDirty();
            }
        }

        void remove(ResourceLocation id) {
            if (entityIds.remove(id)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            entityIds.stream().sorted().forEach(id -> list.add(StringTag.valueOf(id.toString())));
            tag.put("entities", list);
            return tag;
        }
    }

    /** For tests and {@code /ptd}: what LOTM currently has registered for each entity PtD manages. */
    public static Map<ResourceLocation, String> managedRegistrations(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BeyonderEntityData lotmData = BeyonderEntityData.getInstance(overworld);
        Map<ResourceLocation, String> result = new HashMap<>();
        for (ResourceLocation id : OwnedRegistrations.get(overworld).entityIds) {
            EntityType<?> type = entityType(id);
            if (type != null) {
                result.put(id, lotmData.getStringForEntity(type));
            }
        }
        return result;
    }
}
