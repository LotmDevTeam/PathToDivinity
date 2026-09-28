package net.swimmingtuna.pathtodivinity.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.swimmingtuna.pathtodivinity.PTD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads boss profiles from {@code data/<namespace>/ptd_beyonders/**.json} on server start and on
 * {@code /reload}. A later datapack with the same file path replaces a profile; {@code "disabled": true}
 * removes one. A bad file is logged and skipped, never fatal.
 */
@Mod.EventBusSubscriber(modid = PTD.MOD_ID)
public class ProfileLoader extends SimpleJsonResourceReloadListener {

    public static final String FOLDER = "ptd_beyonders";
    private static final Gson GSON = new GsonBuilder().create();

    private final ICondition.IContext conditionContext;

    private ProfileLoader(ICondition.IContext conditionContext) {
        super(GSON, FOLDER);
        this.conditionContext = conditionContext;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ProfileLoader(event.getConditionContext()));
    }

    /** Tag-matched profiles can only be resolved once the new tags are bound. */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            BeyonderProfiles.server().clearCache();
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Integer> packOrder = new HashMap<>();
        List<PackResources> packs = resourceManager.listPacks().toList();
        for (int i = 0; i < packs.size(); i++) {
            packOrder.put(packs.get(i).packId(), i);
        }

        List<BeyonderProfile> loaded = new ArrayList<>();
        int skipped = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            ResourceLocation id = entry.getKey();
            ResourceLocation file = id.withPath(path -> FOLDER + "/" + path + ".json");
            String packId = resourceManager.getResource(file).map(Resource::sourcePackId).orElse("unknown");
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "profile");
                if (!CraftingHelper.processConditions(json, "conditions", conditionContext)
                        || GsonHelper.getAsBoolean(json, "disabled", false)) {
                    skipped++;
                    continue;
                }
                List<String> warnings = new ArrayList<>();
                loaded.add(BeyonderProfile.parse(id, packId, packOrder.getOrDefault(packId, -1), json, warnings));
                for (String warning : warnings) {
                    PTD.LOGGER.warn("Boss profile {} (from {}): {}", id, packId, warning);
                }
            } catch (JsonParseException | IllegalArgumentException e) {
                PTD.LOGGER.error("Skipping boss profile {} (from {}): {}", id, packId, e.getMessage());
            }
        }

        ProfileSnapshot snapshot = new ProfileSnapshot(loaded);
        for (BeyonderProfile profile : snapshot.all()) {
            if (profile.match().kind() == EntityMatcher.Kind.ENTITY && snapshot.forEntityId(profile.match().entityId()) != profile) {
                PTD.LOGGER.info("Boss profile {} is overridden for {} by {}", profile.id(), profile.match(),
                        snapshot.forEntityId(profile.match().entityId()).id());
            }
        }
        BeyonderProfiles.setServer(snapshot);
        PTD.LOGGER.info("Loaded {} boss profiles ({} skipped by conditions or disabled)", loaded.size(), skipped);
    }
}
