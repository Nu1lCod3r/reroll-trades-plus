package com.nullcoder.rerolltradesplus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Empty serverbound payload: the server derives the merchant from the menu
 * the player currently has open, so there is nothing to encode.
 */
public record RerollTradesPayload() implements CustomPacketPayload {
	public static final RerollTradesPayload INSTANCE = new RerollTradesPayload();
	public static final CustomPacketPayload.Type<RerollTradesPayload> TYPE = new CustomPacketPayload.Type<>(RerollTradesPlus.id("reroll_trades"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RerollTradesPayload> CODEC = new StreamCodec<>() {
		@Override
		public void encode(final RegistryFriendlyByteBuf buf, final RerollTradesPayload payload) {
		}

		@Override
		public RerollTradesPayload decode(final RegistryFriendlyByteBuf buf) {
			return INSTANCE;
		}
	};

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
