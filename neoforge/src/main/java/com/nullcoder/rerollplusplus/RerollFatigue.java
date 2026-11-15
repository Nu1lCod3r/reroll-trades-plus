package com.nullcoder.rerollplusplus;

import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.trading.Merchant;

/**
 * Merchant fatigue: every successful reroll of the same merchant within one game day makes
 * the next reroll cost more (configurable step, capped). The price of the first reroll of a
 * day is the base price; the counter resets when the game day changes and on server restart.
 * Refused attempts do not count.
 */
public final class RerollFatigue {
	/** value = {gameDay, successfulRerollsToday}. */
	private static final WeakHashMap<Merchant, long[]> FATIGUE = new WeakHashMap<>();

	private RerollFatigue() {
	}

	/** Price multiplier for the NEXT reroll of this merchant: 1.0 for the first of the day. */
	public static double multiplier(final ServerLevel level, final Merchant merchant) {
		long[] state = FATIGUE.get(merchant);
		if (state == null || !RerollConfig.INSTANCE.enableMerchantFatigue) {
			return 1.0;
		}
		long day = level.getOverworldClockTime() / 24000L;
		if (state[0] != day) {
			return 1.0;
		}
		return Math.min(RerollConfig.INSTANCE.fatigueMaxMultiplier, 1.0 + state[1] * RerollConfig.INSTANCE.fatiguePriceStep);
	}

	/** Actual price of the NEXT reroll, in whole emeralds (never below 1). */
	public static int price(final ServerLevel level, final Merchant merchant, final int basePrice) {
		double price = basePrice * RerollConfig.INSTANCE.priceMultiplier * multiplier(level, merchant);
		return Math.max(1, (int) Math.round(price));
	}

	/** Counts one successful reroll towards fatigue. */
	public static void bump(final ServerLevel level, final Merchant merchant) {
		if (!RerollConfig.INSTANCE.enableMerchantFatigue) {
			return;
		}
		long day = level.getOverworldClockTime() / 24000L;
		long[] state = FATIGUE.computeIfAbsent(merchant, k -> new long[2]);
		if (state[0] != day) {
			state[0] = day;
			state[1] = 0;
		}
		state[1]++;
	}
}
