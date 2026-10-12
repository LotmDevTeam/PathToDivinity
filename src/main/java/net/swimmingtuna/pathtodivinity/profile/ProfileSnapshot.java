package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.capabilities.acting_data.ActingDataProvider;
import net.swimmingtuna.lotm.capabilities.compat.Capability;
import net.swimmingtuna.lotm.capabilities.compat.INBTSerializable;
import net.swimmingtuna.lotm.capabilities.concealed_data.ConcealedDataProvider;
import net.swimmingtuna.lotm.capabilities.concealed_space.ConcealedSpaceProvider;
import net.swimmingtuna.lotm.capabilities.criminal_pathway_data.CriminalPathwayDataProvider;
import net.swimmingtuna.lotm.capabilities.doll_data.DollDataProvider;
import net.swimmingtuna.lotm.capabilities.grazed_abilities.GrazedAbilitiesProvider;
import net.swimmingtuna.lotm.capabilities.honorific_name.HonorificNameProvider;
import net.swimmingtuna.lotm.capabilities.is_concealed_data.IsConcealedProvider;
import net.swimmingtuna.lotm.capabilities.manipulated_player_data.ManipulatedEntityProvider;
import net.swimmingtuna.lotm.capabilities.potion_recipes_data.PotionRecipesProvider;
import net.swimmingtuna.lotm.capabilities.replicated_entity.ReplicatedEntityProvider;
import net.swimmingtuna.lotm.capabilities.ritual_data.RitualDataProvider;
import net.swimmingtuna.lotm.capabilities.sanity_data.SanityDataProvider;
import net.swimmingtuna.lotm.capabilities.scribed_abilities.ScribedAbilitiesProvider;
import net.swimmingtuna.lotm.capabilities.sealed_data.SealedDataProvider;
import net.swimmingtuna.lotm.capabilities.sleep_data.SleepDataProvider;
import net.swimmingtuna.lotm.capabilities.spirit_data.SpiritHolderProvider;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.pathtodivinity.PTD;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfileSnapshot {

    private static final String KEY_HOLDER = "beyonderHolder";
    private static final String KEY_HOLDER_CLASS = "currentClass";
    private static final String KEY_HOLDER_SEQUENCE = "currentSequence";
    private static final String KEY_CAPABILITIES = "lotmCapabilities";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_ENDER_CHEST = "enderChest";
    private static final String KEY_XP_LEVEL = "xpLevel";
    private static final String KEY_XP_PROGRESS = "xpProgress";
    private static final String KEY_XP_TOTAL = "xpTotal";
    private static final String KEY_SELECTED_SLOT = "selectedSlot";
    private static final String KEY_CURIOS = "curios";

    /**
     * Whether Curios is installed. Neither this mod nor LOTM requires it, so every curios call has to go
     * through {@link CuriosProfileBridge} behind this flag — that class is the only one that names Curios
     * types, and it is never loaded when the flag is false.
     *
     * <p>Safe to resolve in a static initialiser: this class is first touched during a profile switch, long
     * after mod loading has finished.
     */
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    private static final Map<String, Capability<?>> LOTM_PLAYER_CAPS = new LinkedHashMap<>();

    static {
        LOTM_PLAYER_CAPS.put("acting", ActingDataProvider.ACTING_DATA);
        LOTM_PLAYER_CAPS.put("ritual", RitualDataProvider.RITUAL_DATA);
        LOTM_PLAYER_CAPS.put("potionRecipes", PotionRecipesProvider.POTION_RECIPES);
        LOTM_PLAYER_CAPS.put("sealed", SealedDataProvider.SEALED_DATA);
        LOTM_PLAYER_CAPS.put("honorificName", HonorificNameProvider.HONORIFIC_NAME);
        LOTM_PLAYER_CAPS.put("spiritHolder", SpiritHolderProvider.SPIRIT_HOLDER);
        LOTM_PLAYER_CAPS.put("grazedAbilities", GrazedAbilitiesProvider.GRAZED_ABILITIES);
        LOTM_PLAYER_CAPS.put("scribedAbilities", ScribedAbilitiesProvider.SCRIBED_ABILITIES);
        LOTM_PLAYER_CAPS.put("sanity", SanityDataProvider.SANITY_DATA);
        LOTM_PLAYER_CAPS.put("sleep", SleepDataProvider.SLEEP_DATA);
        LOTM_PLAYER_CAPS.put("concealed", ConcealedDataProvider.CONCEALED_DATA);
        LOTM_PLAYER_CAPS.put("concealedSpace", ConcealedSpaceProvider.CONCEALED_SPACE);
        LOTM_PLAYER_CAPS.put("isConcealed", IsConcealedProvider.IS_CONCEALED);
        LOTM_PLAYER_CAPS.put("criminal", CriminalPathwayDataProvider.CRIMINAL_DATA);
        LOTM_PLAYER_CAPS.put("doll", DollDataProvider.DOLL_DATA);
        LOTM_PLAYER_CAPS.put("manipulatedPlayer", ManipulatedEntityProvider.MANIPULATED_PLAYER);
        LOTM_PLAYER_CAPS.put("replicatedEntity", ReplicatedEntityProvider.REPLICATED_ENTITY);
    }

    private ProfileSnapshot() {
    }

    public static CompoundTag capture(ServerPlayer player) {
        CompoundTag snapshot = new CompoundTag();

        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        snapshot.put(KEY_HOLDER, holder.serializeNBT(false));

        CompoundTag capabilities = new CompoundTag();
        for (Map.Entry<String, Capability<?>> entry : LOTM_PLAYER_CAPS.entrySet()) {
            entry.getValue().getFrom(player).ifPresent(instance -> {
                INBTSerializable<CompoundTag> serializable = asSerializable(instance);
                if (serializable != null) {
                    capabilities.put(entry.getKey(), serializable.serializeNBT());
                }
            });
        }
        snapshot.put(KEY_CAPABILITIES, capabilities);

        snapshot.put(KEY_INVENTORY, player.getInventory().save(new ListTag()));
        snapshot.putInt(KEY_SELECTED_SLOT, player.getInventory().selected);
        snapshot.put(KEY_ENDER_CHEST, player.getEnderChestInventory().createTag(player.registryAccess()));

        if (CURIOS_LOADED) {
            // Deliberately unguarded: a failure here has to abort the switch rather than quietly write a
            // snapshot with the player's curios missing from it.
            ListTag curios = CuriosProfileBridge.capture(player);
            if (curios != null) {
                snapshot.put(KEY_CURIOS, curios);
            }
        }

        snapshot.putInt(KEY_XP_LEVEL, player.experienceLevel);
        snapshot.putFloat(KEY_XP_PROGRESS, player.experienceProgress);
        snapshot.putInt(KEY_XP_TOTAL, player.totalExperience);

        return snapshot;
    }

    public static String describe(@Nullable CompoundTag snapshot) {
        if (snapshot == null) {
            return "never used";
        }
        try {
            CompoundTag holder = snapshot.getCompound(KEY_HOLDER);
            String className = holder.getString(KEY_HOLDER_CLASS);
            int sequence = holder.getInt(KEY_HOLDER_SEQUENCE);
            if (className.isEmpty() || sequence < 0) {
                return "not a Beyonder";
            }
            BeyonderClass pathway = BeyonderClassInit.getRegistry().get(ResourceLocation.parse(className));
            if (pathway == null) {
                return "saved";
            }
            List<Component> names = pathway.sequenceNames();
            String title = sequence < names.size() ? names.get(sequence).getString() : "Unknown";
            return title + ", Sequence " + sequence;
        } catch (Exception exception) {
            PTD.LOGGER.warn("Could not describe a stored profile", exception);
            return "saved";
        }
    }

    public static void restore(ServerPlayer player, CompoundTag snapshot) {
        restoreHolder(player, snapshot.getCompound(KEY_HOLDER));
        restoreCapabilities(player, snapshot.getCompound(KEY_CAPABILITIES));
        restoreInventoryAndExperience(player, snapshot);
        if (CURIOS_LOADED) {
            // After the inventory, because Curios hands back anything it cannot re-seat — a slot type that no
            // longer exists, say — and loading the inventory afterwards would throw those items away. A missing
            // key gives an empty list, which the bridge reads as "this profile wears nothing".
            CuriosProfileBridge.restore(player, snapshot.getList(KEY_CURIOS, Tag.TAG_COMPOUND));
        }
    }

    private static void restoreHolder(ServerPlayer player, CompoundTag holderTag) {
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);

        holder.removePathway();

        if (holderTag.isEmpty()) {
            return;
        }
        holder.deserializeNBT(holderTag, false);

        BeyonderClass pathway = holder.getCurrentClass();
        int sequence = holder.getSequence();
        if (pathway == null || sequence < 0) {
            holder.removePathway();
            return;
        }

        double spirituality = holder.getSpirituality();
        holder.setPathwayAndSequenceNoSpirituality(pathway, sequence);
        holder.setSpirituality(Math.min(spirituality, holder.getMaxSpirituality()));
    }

    private static void restoreCapabilities(ServerPlayer player, CompoundTag capabilities) {
        for (Map.Entry<String, Capability<?>> entry : LOTM_PLAYER_CAPS.entrySet()) {
            entry.getValue().getFrom(player).ifPresent(instance -> {
                INBTSerializable<CompoundTag> serializable = asSerializable(instance);
                if (serializable == null) {
                    return;
                }
                String key = entry.getKey();
                CompoundTag stored = capabilities.contains(key, Tag.TAG_COMPOUND)
                        ? capabilities.getCompound(key)
                        : null;
                restoreCapability(key, serializable, stored);
            });
        }
    }
    
    private static void restoreCapability(String name, INBTSerializable<CompoundTag> live,
                                          @Nullable CompoundTag stored) {
        CompoundTag tag = stored != null ? stored : defaultsFor(live);
        try {
            live.deserializeNBT(tag);
        } catch (Exception exception) {
            PTD.LOGGER.warn("Could not restore LOTM capability '{}'; leaving it as it was", name, exception);
        }
    }

    private static CompoundTag defaultsFor(INBTSerializable<CompoundTag> live) {
        try {
            Constructor<?> constructor = live.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            INBTSerializable<CompoundTag> fresh = asSerializable(constructor.newInstance());
            if (fresh != null) {
                return fresh.serializeNBT();
            }
        } catch (Exception exception) {
            PTD.LOGGER.warn("Could not build defaults for LOTM capability {}",
                    live.getClass().getName(), exception);
        }
        return new CompoundTag();
    }

    private static void restoreInventoryAndExperience(ServerPlayer player, CompoundTag snapshot) {
        player.getInventory().load(snapshot.getList(KEY_INVENTORY, Tag.TAG_COMPOUND));
        player.getInventory().selected = snapshot.getInt(KEY_SELECTED_SLOT);
        player.getEnderChestInventory().fromTag(snapshot.getList(KEY_ENDER_CHEST, Tag.TAG_COMPOUND), player.registryAccess());

        player.experienceLevel = snapshot.getInt(KEY_XP_LEVEL);
        player.experienceProgress = snapshot.getFloat(KEY_XP_PROGRESS);
        player.totalExperience = snapshot.getInt(KEY_XP_TOTAL);
        player.connection.send(new ClientboundSetExperiencePacket(
                player.experienceProgress, player.totalExperience, player.experienceLevel));

        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static INBTSerializable<CompoundTag> asSerializable(Object instance) {
        return instance instanceof INBTSerializable<?>
                ? (INBTSerializable<CompoundTag>) instance
                : null;
    }
}
