package net.swimmingtuna.pathtodivinity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

/**
 * A player clicking a profile on the screen.
 *
 * <p>Carries the choice and nothing else. Every rule — the feature flag, the cooldown, combat, the limited
 * Sequence 0/1/2 slots — is checked here, on the server, because a packet is a claim about what the player
 * clicked and never about what they are allowed to do.
 */
public class ProfileActionC2SPacket implements CustomPacketPayload {

    public static final Type<ProfileActionC2SPacket> TYPE = PTDNetwork.type("profile_action");
    public static final StreamCodec<FriendlyByteBuf, ProfileActionC2SPacket> STREAM_CODEC =
            StreamCodec.ofMember(ProfileActionC2SPacket::encode, ProfileActionC2SPacket::new);

    private final ProfileType target;

    public ProfileActionC2SPacket(ProfileType target) {
        this.target = target;
    }

    public ProfileActionC2SPacket(FriendlyByteBuf buffer) {
        this.target = ProfileType.values()[buffer.readByte()];
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(this.target.ordinal());
    }

    @Override
    public Type<ProfileActionC2SPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !ProfileManager.isEnabled()) {
            return;
        }

        Component refusal = ProfileManager.switchTo(player, this.target);
        Component status = refusal != null
                ? refusal
                : Component.literal("You are now playing your " + this.target.getDisplayName() + " profile.");

        // Re-send the whole state rather than telling the client what changed: the switch may have moved
        // the player's pathway, cooldown and stored profile all at once.
        ProfileManager.openScreen(player, status, refusal == null);
    }
}
