package com.nullcoder.rerollplusplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nullcoder.rerollplusplus.RerollCost;
import com.nullcoder.rerollplusplus.RerollTradesPlus;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = RerollTradesPlus.MOD_ID, value = Dist.CLIENT)
public class RerollTradesPlusClient {
	/**
	 * Reroll hotkey, usable while the trading screen is open (handled by
	 * AbstractContainerScreenMixin — keybindings never fire on their own while a screen is open).
	 * RegisterKeyMappingsEvent is a mod-bus event, so this class is only registered on the client.
	 */
	public static final KeyMapping REROLL_KEY = new KeyMapping(
		"key." + RerollTradesPlus.MOD_ID + ".reroll", InputConstants.KEY_R, KeyMapping.Category.INVENTORY
	);

	/** Server-synced price for the currently open merchant menu (containerId → price). */
	private static final Map<Integer, Integer> REROLL_PRICES = new HashMap<>();

	@SubscribeEvent
	public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
		event.register(REROLL_KEY);
		// The reroll button itself is added by MerchantScreenMixin when the trade screen opens.
	}

	/** Called from the price-sync payload handler, already on the client thread (MAIN). */
	public static void acceptPrice(final int containerId, final int price) {
		REROLL_PRICES.put(containerId, price);
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
