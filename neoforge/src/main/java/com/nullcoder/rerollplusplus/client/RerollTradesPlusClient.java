package com.nullcoder.rerollplusplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nullcoder.rerollplusplus.RerollTradesPlus;
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

	@SubscribeEvent
	public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
		event.register(REROLL_KEY);
		// The reroll button itself is added by MerchantScreenMixin when the trade screen opens.
	}
}
