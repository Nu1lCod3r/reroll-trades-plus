package com.nullcoder.rerollplusplus;

/**
 * Odds of a merchant refusing a reroll: the higher the trade level, the likelier a refusal.
 */
public final class RerollOdds {
	/** Refusal chance added per trade level above Novice (Novice itself never refuses). */
	public static final double REFUSAL_PER_LEVEL = 0.05;
	/** Wandering traders have no trade level — they use a flat chance. */
	public static final double WANDERING_TRADER_REFUSAL = 0.10;
	/** Hard cap so a Master never refuses for sure. */
	public static final double MAX_REFUSAL = 0.30;

	private RerollOdds() {
	}

	public static double refusalChance(final int level) {
		return Math.min(MAX_REFUSAL, Math.max(0, level - 1) * REFUSAL_PER_LEVEL);
	}
}
