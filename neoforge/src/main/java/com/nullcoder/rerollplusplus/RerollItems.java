package com.nullcoder.rerollplusplus;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Mod-wide registrations that use plain vanilla registries, so the same code works on both
 * loaders: the reroll token item, the advancement trigger and the per-player reroll counter.
 */
public final class RerollItems {
	/** Craftable consumable: rescues a reroll that would fail (refusal or not enough emeralds). */
	public static final Item REROLL_TOKEN = Registry.register(BuiltInRegistries.ITEM,
		RerollTradesPlus.id("reroll_token"), new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

	public static final RerollTrigger REROLL = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
		RerollTradesPlus.id("reroll"), new RerollTrigger());

	/** Custom stat: total successful rerolls per player (persists in the player stats). */
	public static final Identifier REROLL_TRADES_STAT = RerollTradesPlus.id("reroll_trades");

	static {
		// CUSTOM_STAT holds Identifiers; the StatType entry (with formatter) is created via get(id, formatter).
		Registry.register(BuiltInRegistries.CUSTOM_STAT, REROLL_TRADES_STAT, REROLL_TRADES_STAT);
		Stats.CUSTOM.get(REROLL_TRADES_STAT, StatFormatter.DEFAULT);
	}

	private RerollItems() {
	}

	/** Must be called from both loader entry points so the constants above register. */
	public static void register() {
	}

	public static int rerollCount(final net.minecraft.server.level.ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(REROLL_TRADES_STAT));
	}

	public static void awardRerollStat(final net.minecraft.server.level.ServerPlayer player) {
		player.awardStat(Stats.CUSTOM.get(REROLL_TRADES_STAT));
	}
}
