package com.nullcoder.rerollplusplus.client;

import com.nullcoder.rerollplusplus.RerollTradesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Loader-side networking helpers for NeoForge.
 */
public final class NeoForgeNetwork {
	private NeoForgeNetwork() {
	}

	/**
	 * True when the connected server registered the reroll payload — i.e. it runs this mod.
	 * On a vanilla server no payload setup is negotiated and this returns false, so the
	 * client never shows the buttons and never sends anything (Fabric canSend equivalent).
	 */
	public static boolean canSendReroll() {
		ClientPacketListener listener = Minecraft.getInstance().getConnection();
		return listener != null
			&& NetworkRegistry.hasChannel(listener.getConnection(), null, RerollTradesPayload.TYPE.id());
	}
}
