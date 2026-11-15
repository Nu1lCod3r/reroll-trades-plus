package com.nullcoder.rerollplusplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nullcoder.rerollplusplus.RerollCost;
import com.nullcoder.rerollplusplus.RerollPriceSyncPayload;
import com.nullcoder.rerollplusplus.RerollTradesPlus;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

public class RerollTradesPlusClient implements ClientModInitializer {
	/**
	 * Reroll hotkey, usable while the trading screen is open (handled by MerchantScreenMixin —
	 * keybindings never fire on their own while a screen is open).
	 */
	public static final KeyMapping REROLL_KEY = new KeyMapping(
		"key." + RerollTradesPlus.MOD_ID + ".reroll", InputConstants.KEY_R, KeyMapping.Category.INVENTORY
	);

	/** Server-synced price for the currently open merchant menu (containerId → price). */
	private static final Map<Integer, Integer> REROLL_PRICES = new HashMap<>();

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(REROLL_KEY);
		// The reroll button itself is added by MerchantScreenMixin when the trade screen opens.
		ClientPlayNetworking.registerGlobalReceiver(RerollPriceSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> REROLL_PRICES.put(payload.containerId(), payload.price()))
		);
	}

	/**
	 * Actual reroll price for the open menu — includes the fatigue the server knows about.
	 * Falls back to the base price until the first sync for this menu arrives.
	 */
	public static int rerollPrice(final int containerId, final int level) {
		Integer cached = REROLL_PRICES.get(containerId);
		return cached != null ? cached : RerollCost.forLevel(level);
	}
}
