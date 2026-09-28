package net.swimmingtuna.pathtodivinity.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.beyonders.ProfileSyncPacket;

/**
 * This mod's packet channel.
 *
 * <p>The server owns every rule, so the client is sent finished pictures to draw and sends back nothing but
 * which profile the player clicked: the profile screen's contents, and the roster of who is on which profile
 * that name tags are marked from. It also carries the datapack boss profiles to clients (see
 * {@link ProfileSyncPacket}).
 *
 * <p>Bump {@link #PROTOCOL_VERSION} whenever the packet set changes, so a mismatched client is refused at
 * connect rather than mis-decoding an id that has shifted.
 */
public final class PTDNetwork {

    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PTD.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId = 0;

    private PTDNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ProfileScreenS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ProfileScreenS2CPacket::new)
                .encoder(ProfileScreenS2CPacket::encode)
                .consumerMainThread(ProfileScreenS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProfileActionC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProfileActionC2SPacket::new)
                .encoder(ProfileActionC2SPacket::encode)
                .consumerMainThread(ProfileActionC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProfileVisibilityS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ProfileVisibilityS2CPacket::new)
                .encoder(ProfileVisibilityS2CPacket::encode)
                .consumerMainThread(ProfileVisibilityS2CPacket::handle)
                .add();

        // Enqueues its own work, so it is consumed on the network thread.
        CHANNEL.messageBuilder(ProfileSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ProfileSyncPacket::decode)
                .encoder(ProfileSyncPacket::encode)
                .consumerNetworkThread(ProfileSyncPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
