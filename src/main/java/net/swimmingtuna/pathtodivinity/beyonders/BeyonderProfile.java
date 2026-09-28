package net.swimmingtuna.pathtodivinity.beyonders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One boss profile, from {@code data/<namespace>/ptd_beyonders/<path>.json}. Every field except
 * {@code match} is optional; an absent field means "PtD changes nothing about that".
 *
 * @param id              the file's id, e.g. {@code pathtodivinity:cataclysm/the_harbinger}
 * @param packIndex       position of the datapack that supplied it (higher = loaded later); breaks priority ties
 * @param sequence        the sequence LOTM reports for this mob (scaling, spirituality, damage), or null
 * @param abilitySequence the sequence the mob picks abilities at via {@code /beyonderentity}; defaults to {@code sequence}.
 *                        {@link #NO_ABILITIES} (-1): a Beyonder of its pathway that uses no abilities and gets none of
 *                        LOTM's pathway stat modifiers or events
 * @param pathways        one pathway, or a pool to pick from; empty = no LOTM registration
 */
public record BeyonderProfile(
        ResourceLocation id,
        String packId,
        int packIndex,
        EntityMatcher match,
        int priority,
        List<ResourceLocation> pathways,
        Reroll reroll,
        @Nullable Integer sequence,
        @Nullable Integer abilitySequence,
        @Nullable Double healthMultiplier,
        @Nullable Double damageMultiplier,
        boolean boss,
        List<Effect> effects,
        @Nullable Allies allies,
        List<Drop> drops,
        CompoundTag persistentData,
        @Nullable PhaseTwo phaseTwo,
        @Nullable ResourceLocation special) {

    public static final int FORMAT = 1;
    /** {@code "ability_sequence": -1}: registered with LOTM as a Beyonder of its pathway, but inert. */
    public static final int NO_ABILITIES = -1;

    private static final Set<String> KNOWN_KEYS = Set.of("format", "conditions", "match", "priority", "disabled",
            "pathway", "reroll", "sequence", "ability_sequence", "health_multiplier", "damage_multiplier", "boss",
            "effects", "allies", "drops", "persistent_data", "phase_two", "special");

    public enum Reroll {
        /** Pick from the pool once per server start. */
        NEVER,
        /** Also pick again on each refresh while none of this mob is alive. */
        WHEN_NONE_ALIVE
    }

    public record Effect(ResourceLocation effect, int amplifier) {
    }

    /**
     * @param entities       mob types this boss forces into alliance with it
     * @param allMobs        every nearby mob instead (LOTM player mobs excepted)
     * @param radius         blocks around the boss
     * @param intervalTicks  how often the alliance is re-applied
     */
    public record Allies(List<ResourceLocation> entities, boolean allMobs, double radius, int intervalTicks) {
    }

    public record Drop(ResourceLocation item, int count) {
    }

    public record PhaseTwo(double healthMultiplier, double damageMultiplier) {
    }

    /**
     * The sequence the mob is registered with in LOTM (which fixes what LOTM reports as its pathway and
     * sequence id), or null when the profile registers nothing. With {@code ability_sequence} -1 this is
     * the profile's {@code sequence}: the mob is a Beyonder of that sequence that uses no abilities.
     */
    @Nullable
    public Integer registrationSequence() {
        if (abilitySequence == null || abilitySequence == NO_ABILITIES) {
            return sequence;
        }
        return abilitySequence;
    }

    /** False for {@code "ability_sequence": -1}: no abilities, no LOTM pathway stat modifiers or events. */
    public boolean usesAbilities() {
        return abilitySequence == null || abilitySequence != NO_ABILITIES;
    }

    // ---- Parsing ----

    /**
     * Parses a profile. Throws {@link JsonParseException} for anything that makes it unusable; problems
     * that only lose part of it are reported through {@code warnings} and skipped.
     */
    public static BeyonderProfile parse(ResourceLocation id, String packId, int packIndex, JsonObject json, List<String> warnings) {
        int format = GsonHelper.getAsInt(json, "format", FORMAT);
        if (format > FORMAT) {
            warnings.add("format " + format + " is newer than this version of Path to Divinity understands (" + FORMAT + "); reading what it can");
        }
        for (String key : json.keySet()) {
            if (!KNOWN_KEYS.contains(key)) {
                warnings.add("unknown field \"" + key + "\" ignored");
            }
        }

        EntityMatcher match = parseMatch(GsonHelper.getAsJsonObject(json, "match"));

        List<ResourceLocation> pathways = new ArrayList<>();
        if (json.has("pathway")) {
            JsonElement pathway = json.get("pathway");
            if (pathway.isJsonArray()) {
                for (JsonElement element : pathway.getAsJsonArray()) {
                    pathways.add(location(element, "pathway"));
                }
            } else {
                pathways.add(location(pathway, "pathway"));
            }
        }
        Reroll reroll = switch (GsonHelper.getAsString(json, "reroll", "never")) {
            case "never" -> Reroll.NEVER;
            case "when_none_alive" -> Reroll.WHEN_NONE_ALIVE;
            default -> throw new JsonParseException("reroll must be \"never\" or \"when_none_alive\"");
        };

        Integer sequence = optionalSequence(json, "sequence");
        Integer abilitySequence = json.has("ability_sequence") && GsonHelper.getAsInt(json, "ability_sequence") == NO_ABILITIES
                ? Integer.valueOf(NO_ABILITIES) : optionalSequence(json, "ability_sequence");
        if (abilitySequence != null && pathways.isEmpty()) {
            warnings.add("ability_sequence has no effect without a pathway");
        }
        if (abilitySequence != null && abilitySequence == NO_ABILITIES && sequence == null && !pathways.isEmpty()) {
            warnings.add("ability_sequence -1 needs a sequence to register the mob with; it will not be registered");
        }

        Double health = optionalMultiplier(json, "health_multiplier");
        Double damage = optionalMultiplier(json, "damage_multiplier");

        List<Effect> effects = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "effects", new JsonArray())) {
            JsonObject effect = GsonHelper.convertToJsonObject(element, "effect");
            effects.add(new Effect(location(effect.get("effect"), "effect"), GsonHelper.getAsInt(effect, "amplifier", 0)));
        }

        Allies allies = null;
        if (json.has("allies")) {
            JsonObject object = GsonHelper.getAsJsonObject(json, "allies");
            List<ResourceLocation> entities = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "entities", new JsonArray())) {
                entities.add(location(element, "allies.entities"));
            }
            allies = new Allies(entities, GsonHelper.getAsBoolean(object, "all_mobs", false),
                    GsonHelper.getAsDouble(object, "radius", 25.0), Math.max(1, GsonHelper.getAsInt(object, "interval_ticks", 40)));
        }

        List<Drop> drops = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "drops", new JsonArray())) {
            JsonObject drop = GsonHelper.convertToJsonObject(element, "drop");
            drops.add(new Drop(location(drop.get("item"), "drops.item"), Math.max(1, GsonHelper.getAsInt(drop, "count", 1))));
        }

        CompoundTag persistentData = new CompoundTag();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "persistent_data", new JsonObject()).entrySet()) {
            if (!(entry.getValue() instanceof JsonPrimitive primitive)) {
                warnings.add("persistent_data." + entry.getKey() + " must be a number, boolean or string; skipped");
            } else if (primitive.isBoolean()) {
                persistentData.putBoolean(entry.getKey(), primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                persistentData.putDouble(entry.getKey(), primitive.getAsDouble());
            } else {
                persistentData.putString(entry.getKey(), primitive.getAsString());
            }
        }

        PhaseTwo phaseTwo = null;
        if (json.has("phase_two")) {
            JsonObject object = GsonHelper.getAsJsonObject(json, "phase_two");
            phaseTwo = new PhaseTwo(GsonHelper.getAsDouble(object, "health_multiplier", 1.0), GsonHelper.getAsDouble(object, "damage_multiplier", 1.0));
        }

        ResourceLocation special = json.has("special") ? location(json.get("special"), "special") : null;

        return new BeyonderProfile(id, packId, packIndex, match, GsonHelper.getAsInt(json, "priority", 0), List.copyOf(pathways), reroll,
                sequence, abilitySequence, health, damage, GsonHelper.getAsBoolean(json, "boss", false), List.copyOf(effects), allies,
                List.copyOf(drops), persistentData, phaseTwo, special);
    }

    private static EntityMatcher parseMatch(JsonObject match) {
        List<EntityMatcher> found = new ArrayList<>();
        if (match.has("entity")) {
            found.add(new EntityMatcher(EntityMatcher.Kind.ENTITY, location(match.get("entity"), "match.entity").toString()));
        }
        if (match.has("tag")) {
            String tag = GsonHelper.getAsString(match, "tag");
            found.add(new EntityMatcher(EntityMatcher.Kind.TAG, parseLocation(tag.startsWith("#") ? tag.substring(1) : tag, "match.tag").toString()));
        }
        if (match.has("name_contains")) {
            String fragment = GsonHelper.getAsString(match, "name_contains");
            if (fragment.isBlank()) {
                throw new JsonParseException("match.name_contains must not be empty");
            }
            found.add(EntityMatcher.nameContains(fragment));
        }
        if (match.has("class_name")) {
            found.add(new EntityMatcher(EntityMatcher.Kind.CLASS_NAME, GsonHelper.getAsString(match, "class_name")));
        }
        if (found.size() != 1) {
            throw new JsonParseException("match needs exactly one of entity, tag, name_contains or class_name");
        }
        return found.get(0);
    }

    @Nullable
    private static Integer optionalSequence(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        int value = GsonHelper.getAsInt(json, key);
        if (value < 0 || value > 9) {
            throw new JsonParseException(key + " must be between 0 and 9");
        }
        return value;
    }

    @Nullable
    private static Double optionalMultiplier(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        double value = GsonHelper.getAsDouble(json, key);
        if (value < 0) {
            throw new JsonParseException(key + " must not be negative");
        }
        return value;
    }

    private static ResourceLocation location(@Nullable JsonElement element, String field) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new JsonParseException(field + " must be a resource id such as \"minecraft:zombie\"");
        }
        return parseLocation(element.getAsString(), field);
    }

    private static ResourceLocation parseLocation(String text, String field) {
        ResourceLocation location = ResourceLocation.tryParse(text);
        if (location == null) {
            throw new JsonParseException(field + " is not a valid resource id: " + text);
        }
        return location;
    }
}
