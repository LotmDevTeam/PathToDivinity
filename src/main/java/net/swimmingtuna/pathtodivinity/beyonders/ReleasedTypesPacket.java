package net.swimmingtuna.pathtodivinity.beyonders;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swimmingtuna.pathtodivinity.network.PTDNetwork;

import java.util.HashSet;
import java.util.Set;

/**
 * Sends clients the {@link ReleasedTypes}: on join (with {@link ProfileSyncPacket}) and to everyone whenever an admin's
 * {@code /beyonderentity} change releases a type or PtD takes one back.
 */
public final class ReleasedTypesPacket implements CustomPacketPayload {

    public static final Type<ReleasedTypesPacket> TYPE = PTDNetwork.type("released_types");
    public static final StreamCodec<FriendlyByteBuf, ReleasedTypesPacket> STREAM_CODEC =
            StreamCodec.ofMember(ReleasedTypesPacket::encode, ReleasedTypesPacket::decode);

    private final Set<ResourceLocation> released;

    public ReleasedTypesPacket(Set<ResourceLocation> released) {
        this.released = released;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(released.size());
        for (ResourceLocation id : released) {
            buf.writeResourceLocation(id);
        }
    }

    public static ReleasedTypesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<ResourceLocation> released = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            released.add(buf.readResourceLocation());
        }
        return new ReleasedTypesPacket(released);
    }

    @Override
    public Type<ReleasedTypesPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        ReleasedTypes.setClient(released);
    }
}
