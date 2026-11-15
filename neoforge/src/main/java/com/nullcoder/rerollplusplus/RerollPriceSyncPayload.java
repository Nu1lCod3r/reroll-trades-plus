package com.nullcoder.rerollplusplus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Clientbound: the actual emerald price of the next reroll for the currently open merchant
 * (base price × config multiplier × merchant fatigue). The client shows this on the button.
 */
public record RerollPriceSyncPayload(int containerId, int price) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RerollPriceSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(RerollTradesPlus.id("reroll_price_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RerollPriceSyncPayload> CODEC = new StreamCodec<>() {
		@Override
		public void encode(final RegistryFriendlyByteBuf buf, final RerollPriceSyncPayload payload) {
			buf.writeVarInt(payload.containerId);
			buf.writeVarInt(payload.price);
		}

		@Override
		public RerollPriceSyncPayload decode(final RegistryFriendlyByteBuf buf) {
			return new RerollPriceSyncPayload(buf.readVarInt(), buf.readVarInt());
		}
	};

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
