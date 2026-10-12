package net.swimmingtuna.pathtodivinity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swimmingtuna.pathtodivinity.client.ProfileVisibilityCache;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells the client which players are on which profile, so name tags can be marked.
 *
 * <p>The profile store is server-only, so the client would otherwise have nothing to draw from. One shape
 * covers every case: a full snapshot when a player joins ({@link #replaceAll}), and a single-entry update
 * when someone chooses, switches or logs out.
 *
 * <p>A {@code null} profile means "forget this player" — used on logout so the client's map does not
 * accumulate UUIDs for the whole session.
 */
public class ProfileVisibilityS2CPacket implements CustomPacketPayload {

    public static final Type<ProfileVisibilityS2CPacket> TYPE = PTDNetwork.type("profile_visibility");
    public static final StreamCodec<FriendlyByteBuf, ProfileVisibilityS2CPacket> STREAM_CODEC =
            StreamCodec.ofMember(ProfileVisibilityS2CPacket::encode, ProfileVisibilityS2CPacket::new);

    /** Marks "no profile" on the wire, where {@code null} cannot be written. Matches ProfileScreenS2CPacket. */
    private static final byte NO_PROFILE = -1;

    /** Whether to discard everything the client knows first, rather than merging these entries into it. */
    private final boolean replaceAll;

    private final Map<UUID, ProfileType> entries;

    public ProfileVisibilityS2CPacket(boolean replaceAll, Map<UUID, ProfileType> entries) {
        this.replaceAll = replaceAll;
        this.entries = entries;
    }

    /** One player's profile, or {@code null} to drop them from the client's map. */
    public static ProfileVisibilityS2CPacket single(UUID uuid, @Nullable ProfileType type) {
        Map<UUID, ProfileType> entries = new HashMap<>(1);
        entries.put(uuid, type);
        return new ProfileVisibilityS2CPacket(false, entries);
    }

    public static ProfileVisibilityS2CPacket snapshot(Map<UUID, ProfileType> entries) {
        return new ProfileVisibilityS2CPacket(true, entries);
    }

    public ProfileVisibilityS2CPacket(FriendlyByteBuf buffer) {
        this.replaceAll = buffer.readBoolean();
        int count = buffer.readVarInt();
        Map<UUID, ProfileType> read = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = buffer.readUUID();
            byte id = buffer.readByte();
            // HashMap keeps null values, which is what carries "forget this player" through to the cache.
            read.put(uuid, id == NO_PROFILE ? null : ProfileType.values()[id]);
        }
        this.entries = read;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.replaceAll);
        buffer.writeVarInt(this.entries.size());
        for (Map.Entry<UUID, ProfileType> entry : this.entries.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeByte(entry.getValue() == null ? NO_PROFILE : (byte) entry.getValue().ordinal());
        }
    }

    @Override
    public Type<ProfileVisibilityS2CPacket> type() {
        return TYPE;
    }

    /** Only ever runs on the client: the payload is registered play-to-client. */
    public void handle(IPayloadContext context) {
        ProfileVisibilityCache.apply(this);
    }

    public boolean isReplaceAll() {
        return this.replaceAll;
    }

    /** Entries to apply; a {@code null} value means the player should be forgotten. */
    public Map<UUID, ProfileType> getEntries() {
        return Collections.unmodifiableMap(this.entries);
    }
}
