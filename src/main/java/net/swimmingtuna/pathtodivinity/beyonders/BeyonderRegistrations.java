package net.swimmingtuna.pathtodivinity.beyonders;

import net.minecraft.core.HolderLookup;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.network.PTDNetwork;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.Level;
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
 * <p>PtD remembers, per world, which entity types it registered and the exact mapping it wrote. A registration
 * whose profile is gone or no longer sets a pathway is removed again; registrations an admin made by hand for
 * entities PtD never touched are left alone.
 *
 * <p>A hand-made {@code /beyonderentity add} beats a profile (LOTM #203). When LOTM's mapping for a type no longer
 * matches what PtD wrote (or a mapping was already there before PtD took the type), the type is <em>released</em>:
 * PtD stops overwriting it, and the profile's sequence and inert ({@code "ability_sequence": -1}) rules stop
 * applying to it ({@link ReleasedTypes}). The boss parts of the profile stay. {@code /beyonderentity remove <type>}
 * hands the type back to PtD at the next refresh.
 *
 * <p>Profiles matched by entity id register at server start. Profiles matched by tag, name or class
 * can't know their entity types up front, so each type is registered the first time a matching mob
 * joins the world ({@link #registerRuleMatch}) and re-checked on every refresh.
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
            ResourceLocation entityId = profile.match().entityId();
            EntityType<?> type = entityType(entityId);
            if (type != null && register(lotmData, owned, type, profile, firstRun, aliveTypes)) { // null: its mod isn't installed
                current.add(entityId);
                owned.add(entityId);
            }
        }
        // Types registered through a tag/name/class profile: keep them while such a profile still claims them.
        for (ResourceLocation entityId : Set.copyOf(owned.ruleEntityIds)) {
            EntityType<?> type = entityType(entityId);
            Entity probe = type == null ? null : type.create(overworld);
            BeyonderProfile profile = probe == null ? null : BeyonderProfiles.server().find(probe);
            if (profile != null && profile.match().kind() != EntityMatcher.Kind.ENTITY
                    && register(lotmData, owned, type, profile, firstRun, aliveTypes)) {
                current.add(entityId);
            } else {
                owned.removeRule(entityId);
            }
        }

        for (ResourceLocation entityId : Set.copyOf(owned.entityIds)) {
            if (!current.contains(entityId)) {
                EntityType<?> type = entityType(entityId);
                // A released type's mapping is the admin's: leave it, just stop tracking the type.
                if (type != null && !owned.released.contains(entityId) && ownedByPtd(owned, entityId, lotmData.getStringForEntity(type))) {
                    write(() -> lotmData.removeEntity(type));
                    PTD.LOGGER.info("Removed LOTM registration for {}: its boss profile no longer sets a pathway", entityId);
                }
                owned.remove(entityId);
            }
        }
        publish(server, owned);
    }

    /**
     * Called after LOTM's {@code /beyonderentity} table changes (BeyonderEntityDataMixin), unless PtD made the change.
     * A type whose mapping now differs from what PtD wrote is released straight away, so a mob summoned right after
     * {@code /beyonderentity add} already fights with the hand mapping. Taking a type back after
     * {@code /beyonderentity remove} waits for {@link #apply}, which re-registers it.
     */
    public static void onLotmMappingsChanged() {
        if (writing) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        BeyonderEntityData lotmData = BeyonderEntityData.getInstance(overworld);
        OwnedRegistrations owned = OwnedRegistrations.get(overworld);
        for (ResourceLocation entityId : Set.copyOf(owned.entityIds)) {
            String written = owned.written.get(entityId);
            EntityType<?> type = entityType(entityId);
            if (written == null || type == null || owned.released.contains(entityId)) {
                continue;
            }
            String now = lotmData.getStringForEntity(type);
            if (now != null && !now.equals(written)) {
                release(owned, entityId, now);
            }
        }
        publish(server, owned);
    }

    /** Whether PtD still decides this type's sequence and inert rule (server side). */
    public static boolean isReleased(MinecraftServer server, ResourceLocation entityId) {
        return OwnedRegistrations.get(server.overworld()).released.contains(entityId);
    }

    /** Sends the released types to clients when they changed. */
    private static void publish(MinecraftServer server, OwnedRegistrations owned) {
        if (ReleasedTypes.setServer(owned.released) && server.getPlayerList() != null) {
            PTDNetwork.sendToAll(new ReleasedTypesPacket(ReleasedTypes.server()));
        }
    }

    private static void release(OwnedRegistrations owned, ResourceLocation entityId, String mapping) {
        owned.release(entityId);
        PTD.LOGGER.info("{} is mapped by hand to {} with /beyonderentity, so its PtD boss profile no longer sets its pathway,"
                + " sequence or abilities. /beyonderentity remove {} gives it back to PtD.", entityId, mapping, entityId);
    }

    /** True if LOTM's current mapping for the type is one PtD wrote (or PtD wrote nothing yet that it could compare). */
    private static boolean ownedByPtd(OwnedRegistrations owned, ResourceLocation entityId, @Nullable String mapping) {
        if (mapping == null) {
            return true;
        }
        String written = owned.written.get(entityId);
        if (written != null) {
            return written.equals(mapping);
        }
        // Saves from before PtD recorded what it wrote: a type it already owned is taken to be its own, as before.
        return owned.entityIds.contains(entityId);
    }

    /** Set while PtD itself edits LOTM's table, so BeyonderEntityDataMixin doesn't take it for an admin's change. */
    private static boolean writing;

    private static void write(Runnable change) {
        boolean was = writing;
        writing = true;
        try {
            change.run();
        } finally {
            writing = was;
        }
    }

    /** Forgets the server's released types when it stops (an integrated server can open another world). */
    public static void onServerStopped() {
        ReleasedTypes.setServer(Set.of());
    }

    /**
     * Registers a mob matched by a tag, name or class profile, the first time one joins the world.
     */
    public static void registerRuleMatch(MinecraftServer server, EntityType<?> type, BeyonderProfile profile) {
        ServerLevel overworld = server.overworld();
        OwnedRegistrations owned = OwnedRegistrations.get(overworld);
        if (register(BeyonderEntityData.getInstance(overworld), owned, type, profile, false, Set.of())) {
            ResourceLocation entityId = EntityType.getKey(type);
            owned.add(entityId);
            owned.addRule(entityId);
            publish(server, owned);
        }
    }

    /**
     * Makes LOTM's registration for {@code type} match the profile, unless an admin mapped the type by hand;
     * false when the profile registers nothing.
     */
    private static boolean register(BeyonderEntityData lotmData, OwnedRegistrations owned, EntityType<?> type,
                                    BeyonderProfile profile, boolean firstRun, Set<ResourceLocation> aliveTypes) {
        Integer sequence = profile.registrationSequence();
        if (profile.pathways().isEmpty() || sequence == null) {
            return false;
        }
        ResourceLocation entityId = EntityType.getKey(type);
        String registered = lotmData.getStringForEntity(type);
        if (owned.released.contains(entityId)) {
            if (registered != null) {
                return true; // the admin's mapping stands
            }
            owned.unrelease(entityId);
            PTD.LOGGER.info("{} has no /beyonderentity mapping any more: its PtD boss profile {} sets it again", entityId, profile.id());
        } else if (!ownedByPtd(owned, entityId, registered)) {
            release(owned, entityId, registered);
            return true;
        }
        String wanted;
        if (profile.pathways().size() == 1) {
            wanted = sequenceId(profile, profile.pathways().get(0), sequence);
        } else {
            boolean reroll = firstRun || registered == null
                    || (profile.reroll() == BeyonderProfile.Reroll.WHEN_NONE_ALIVE && !aliveTypes.contains(EntityType.getKey(type)));
            if (reroll) {
                List<ResourceLocation> pool = profile.pathways();
                wanted = sequenceId(profile, pool.get(ThreadLocalRandom.current().nextInt(pool.size())), sequence);
            } else {
                wanted = registered;
            }
        }
        if (wanted != null && !wanted.equals(registered)) {
            write(() -> lotmData.setEntityString(type, wanted));
        }
        if (wanted != null) {
            owned.setWritten(entityId, wanted);
        }
        return true;
    }

    @Nullable
    private static String sequenceId(BeyonderProfile profile, ResourceLocation pathwayId, int sequence) {
        BeyonderClass pathway = BeyonderClassInit.getRegistry().get(pathwayId);
        if (pathway == null) {
            PTD.LOGGER.warn("Boss profile {}: unknown pathway {}", profile.id(), pathwayId);
            return null;
        }
        return pathway.sequenceIds().get(sequence);
    }

    @Nullable
    private static EntityType<?> entityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
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
        /** Subset registered through tag/name/class profiles. */
        private final Set<ResourceLocation> ruleEntityIds = new HashSet<>();
        /** The exact LOTM mapping PtD last wrote for each type, to tell an admin's change from its own. */
        private final Map<ResourceLocation, String> written = new HashMap<>();
        /** Types mapped by hand over their profile; see {@link ReleasedTypes}. */
        private final Set<ResourceLocation> released = new HashSet<>();

        static OwnedRegistrations get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(OwnedRegistrations::new, OwnedRegistrations::load, null), NAME);
        }

        private static OwnedRegistrations load(CompoundTag tag, HolderLookup.Provider registries) {
            OwnedRegistrations data = new OwnedRegistrations();
            for (Tag element : tag.getList("entities", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    data.entityIds.add(id);
                }
            }
            for (Tag element : tag.getList("rule_entities", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    data.ruleEntityIds.add(id);
                }
            }
            CompoundTag written = tag.getCompound("written");
            for (String key : written.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    data.written.put(id, written.getString(key));
                }
            }
            for (Tag element : tag.getList("released", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    data.released.add(id);
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
            boolean changed = entityIds.remove(id);
            changed |= written.remove(id) != null;
            changed |= released.remove(id);
            if (changed) {
                setDirty();
            }
        }

        void setWritten(ResourceLocation id, String mapping) {
            if (!mapping.equals(written.put(id, mapping))) {
                setDirty();
            }
        }

        void release(ResourceLocation id) {
            entityIds.add(id);
            if (released.add(id)) {
                setDirty();
            }
        }

        void unrelease(ResourceLocation id) {
            if (released.remove(id)) {
                setDirty();
            }
        }

        void addRule(ResourceLocation id) {
            if (ruleEntityIds.add(id)) {
                setDirty();
            }
        }

        void removeRule(ResourceLocation id) {
            if (ruleEntityIds.remove(id)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            entityIds.stream().sorted().forEach(id -> list.add(StringTag.valueOf(id.toString())));
            tag.put("entities", list);
            ListTag rules = new ListTag();
            ruleEntityIds.stream().sorted().forEach(id -> rules.add(StringTag.valueOf(id.toString())));
            tag.put("rule_entities", rules);
            CompoundTag writtenTag = new CompoundTag();
            written.forEach((id, mapping) -> writtenTag.putString(id.toString(), mapping));
            tag.put("written", writtenTag);
            ListTag releasedList = new ListTag();
            released.stream().sorted().forEach(id -> releasedList.add(StringTag.valueOf(id.toString())));
            tag.put("released", releasedList);
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
