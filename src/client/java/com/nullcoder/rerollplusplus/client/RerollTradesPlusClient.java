package com.nullcoder.rerollplusplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nullcoder.rerollplusplus.RerollTradesPlus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class RerollTradesPlusClient implements ClientModInitializer {
	/**
	 * Reroll hotkey, usable while the trading screen is open (handled by MerchantScreenMixin —
	 * keybindings never fire on their own while a screen is open).
	 */
	public static final KeyMapping REROLL_KEY = new KeyMapping(
		"key." + RerollTradesPlus.MOD_ID + ".reroll", InputConstants.KEY_R, KeyMapping.Category.INVENTORY
	);

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(REROLL_KEY);
		// The reroll button itself is added by MerchantScreenMixin when the trade screen opens.
	}
}
