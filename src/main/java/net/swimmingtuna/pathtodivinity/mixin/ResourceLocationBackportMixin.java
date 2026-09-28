package net.swimmingtuna.pathtodivinity.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds the {@code ResourceLocation} factory methods that Forge 47.4 backported from Minecraft 1.21
 * ({@code parse}, {@code fromNamespaceAndPath}, {@code withDefaultNamespace}) to older Forge 47.x builds.
 * Some mods built against 47.4 call them (Cataclysm 3.31 does while rendering its armor) and crash with
 * {@code NoSuchMethodError} on anything older. Implemented exactly as Forge does.
 *
 * <p>{@code PTDMixinPlugin} only applies this when the running Forge lacks the methods, so on 47.4+
 * nothing changes. Mixin won't add non-private static methods, so they are declared private here and
 * the plugin makes them public once they are in {@code ResourceLocation}.
 */
@Mixin(ResourceLocation.class)
public abstract class ResourceLocationBackportMixin {

    private static ResourceLocation parse(String location) {
        return new ResourceLocation(location);
    }

    private static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    private static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
