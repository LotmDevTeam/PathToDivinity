package net.swimmingtuna.pathtodivinity.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.swimmingtuna.pathtodivinity.PTD;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Sends clients the part of the boss profiles that client-side code reads: which entities each
 * profile matches, in what precedence, and the sequence it gives them. {@code BeyonderUtil.getSequence}
 * also runs on the client, and without this it would disagree with the server.
 */
@Mod.EventBusSubscriber(modid = PTD.MOD_ID)
public final class ProfileSyncPacket {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PTD.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private final List<BeyonderProfile> profiles;

    private ProfileSyncPacket(List<BeyonderProfile> profiles) {
        this.profiles = profiles;
    }

    public static void register() {
        CHANNEL.registerMessage(0, ProfileSyncPacket.class, ProfileSyncPacket::encode, ProfileSyncPacket::decode,
                ProfileSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** On join (one player) and after {@code /reload} (everyone). */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ProfileSyncPacket packet = new ProfileSyncPacket(BeyonderProfiles.server().all());
        ServerPlayer player = event.getPlayer();
        CHANNEL.send(player != null ? PacketDistributor.PLAYER.with(() -> player) : PacketDistributor.ALL.noArg(), packet);
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(profiles.size());
        for (BeyonderProfile profile : profiles) {
            buf.writeResourceLocation(profile.id());
            buf.writeVarInt(profile.packIndex());
            profile.match().write(buf);
            buf.writeVarInt(profile.priority());
            buf.writeVarInt(profile.sequence() == null ? -1 : profile.sequence());
            buf.writeBoolean(profile.boss());
        }
    }

    private static ProfileSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<BeyonderProfile> profiles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int packIndex = buf.readVarInt();
            EntityMatcher match = EntityMatcher.read(buf);
            int priority = buf.readVarInt();
            int sequence = buf.readVarInt();
            boolean boss = buf.readBoolean();
            profiles.add(new BeyonderProfile(id, "server", packIndex, match, priority, List.of(), BeyonderProfile.Reroll.NEVER,
                    sequence < 0 ? null : sequence, null, null, null, boss, List.of(), null, List.of(), new CompoundTag(), null, null));
        }
        return new ProfileSyncPacket(profiles);
    }

    private void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> BeyonderProfiles.setClient(new ProfileSnapshot(profiles)));
        context.get().setPacketHandled(true);
    }
}
