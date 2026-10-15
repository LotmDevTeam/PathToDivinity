package net.swimmingtuna.pathtodivinity.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.beyonders.ProfileSyncPacket;

/**
 * This mod's packets.
 *
 * <p>The server owns every rule, so the client is sent finished pictures to draw and sends back nothing but
 * which profile the player clicked: the profile screen's contents, and the roster of who is on which profile
 * that name tags are marked from. It also carries the datapack boss profiles to clients (see
 * {@link ProfileSyncPacket}).
 *
 * <p>Bump {@link #PROTOCOL_VERSION} whenever the packet set changes, so a mismatched client is refused at
 * connect rather than mis-decoding a payload whose layout has shifted.
 */
@EventBusSubscriber(modid = PTD.MOD_ID)
public final class PTDNetwork {

    private static final String PROTOCOL_VERSION = "4";

    private PTDNetwork() {
    }

    /** Payload ids live under this mod's namespace, e.g. {@code pathtodivinity:profile_screen}. */
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PTD.MOD_ID, path));
    }

    // Handlers run on the main thread (NeoForge's default), so none of them enqueue work themselves.
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(ProfileScreenS2CPacket.TYPE, ProfileScreenS2CPacket.STREAM_CODEC, ProfileScreenS2CPacket::handle);
        registrar.playToServer(ProfileActionC2SPacket.TYPE, ProfileActionC2SPacket.STREAM_CODEC, ProfileActionC2SPacket::handle);
        registrar.playToClient(ProfileVisibilityS2CPacket.TYPE, ProfileVisibilityS2CPacket.STREAM_CODEC, ProfileVisibilityS2CPacket::handle);
        registrar.playToClient(ProfileSyncPacket.TYPE, ProfileSyncPacket.STREAM_CODEC, ProfileSyncPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAll(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
