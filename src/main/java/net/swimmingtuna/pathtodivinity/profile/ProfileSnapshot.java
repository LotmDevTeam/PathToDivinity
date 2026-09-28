package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An immutable set of loaded profiles and the lookup from an entity to the one that applies.
 *
 * <p>Precedence: an exact {@code entity} match beats a {@code tag}, which beats {@code name_contains}
 * and {@code class_name}. Within a kind, higher {@code priority} wins, then the later datapack, then
 * the id. The winner for each entity type is cached, so a lookup is one hash lookup after the first.
 */
public final class ProfileSnapshot {

    /** Best first. Declared before EMPTY, which uses it. */
    private static final Comparator<BeyonderProfile> BEST_FIRST = Comparator
            .comparingInt((BeyonderProfile p) -> p.match().kind().precedence).reversed()
            .thenComparing(Comparator.comparingInt(BeyonderProfile::priority).reversed())
            .thenComparing(Comparator.comparingInt(BeyonderProfile::packIndex).reversed())
            .thenComparing(p -> p.id().toString());

    public static final ProfileSnapshot EMPTY = new ProfileSnapshot(List.of());

    private final List<BeyonderProfile> profiles;
    private final Map<String, BeyonderProfile> byEntityId = new HashMap<>();
    /** Tag, name and class profiles, best first. */
    private final List<BeyonderProfile> ruleProfiles;
    private final Map<EntityType<?>, Optional<BeyonderProfile>> cache = new ConcurrentHashMap<>();

    public ProfileSnapshot(Collection<BeyonderProfile> loaded) {
        this.profiles = loaded.stream().sorted(BEST_FIRST).toList();
        for (BeyonderProfile profile : profiles) {
            if (profile.match().kind() == EntityMatcher.Kind.ENTITY) {
                byEntityId.putIfAbsent(profile.match().value(), profile);
            }
        }
        this.ruleProfiles = profiles.stream().filter(p -> p.match().kind() != EntityMatcher.Kind.ENTITY).toList();
    }

    /** All profiles, best first; several may target the same entity. */
    public List<BeyonderProfile> all() {
        return profiles;
    }

    /** Profiles that won their entity id, i.e. the ones in effect for exact matches. */
    public Collection<BeyonderProfile> exactProfiles() {
        return byEntityId.values();
    }

    @Nullable
    public BeyonderProfile forEntityId(ResourceLocation id) {
        return byEntityId.get(id.toString());
    }

    @Nullable
    public BeyonderProfile find(Entity entity) {
        return cache.computeIfAbsent(entity.getType(), type -> Optional.ofNullable(resolve(entity))).orElse(null);
    }

    @Nullable
    private BeyonderProfile resolve(Entity entity) {
        BeyonderProfile exact = byEntityId.get(EntityType.getKey(entity.getType()).toString());
        if (exact != null) {
            return exact;
        }
        for (BeyonderProfile profile : ruleProfiles) {
            if (profile.match().matches(entity)) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Tags are bound to the registries after reload listeners run, so a snapshot built during a
     * reload may have cached answers from before; call once tags are ready.
     */
    public void clearCache() {
        cache.clear();
    }
}
