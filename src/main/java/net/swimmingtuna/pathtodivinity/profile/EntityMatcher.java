package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;

import java.util.Locale;

/**
 * What a boss profile applies to: exactly one of an entity id, an entity tag, or (opt-in, for mods
 * this project can't reference by id) a fragment of the entity type's id/name or its class name.
 * Custom names are never consulted, so a name tag can't turn a mob into a boss.
 */
public record EntityMatcher(Kind kind, String value) {

    public enum Kind {
        /** Exact registry id. Beats every other kind. */
        ENTITY(3),
        /** Entity type tag. */
        TAG(2),
        /** Fragment of the type's registry id or name, lower case. */
        NAME_CONTAINS(1),
        /** Simple class name of the entity. */
        CLASS_NAME(1);

        /** Higher wins when several profiles match one entity. */
        final int precedence;

        Kind(int precedence) {
            this.precedence = precedence;
        }
    }

    public static EntityMatcher entity(ResourceLocation id) {
        return new EntityMatcher(Kind.ENTITY, id.toString());
    }

    public ResourceLocation entityId() {
        return new ResourceLocation(value);
    }

    public boolean matches(Entity entity) {
        return switch (kind) {
            case ENTITY -> value.equals(EntityType.getKey(entity.getType()).toString());
            case TAG -> entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(value)));
            case NAME_CONTAINS -> PTDEntities.typeSearchText(entity).contains(value);
            case CLASS_NAME -> entity.getClass().getSimpleName().equals(value);
        };
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeUtf(value);
    }

    public static EntityMatcher read(FriendlyByteBuf buf) {
        return new EntityMatcher(buf.readEnum(Kind.class), buf.readUtf());
    }

    static EntityMatcher nameContains(String fragment) {
        return new EntityMatcher(Kind.NAME_CONTAINS, fragment.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return switch (kind) {
            case ENTITY -> value;
            case TAG -> "#" + value;
            case NAME_CONTAINS -> "name contains \"" + value + "\"";
            case CLASS_NAME -> "class " + value;
        };
    }
}
