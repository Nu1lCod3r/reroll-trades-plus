package com.nullcoder.rerolltradesplus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Emerald price of a reroll: the higher the villager's trade level, the more it costs.
 */
public final class RerollCost {
	/** Emerald cost per villager trade level (index = level - 1). */
	private static final int[] EMERALDS_PER_LEVEL = {1, 2, 4, 8, 16};

	private RerollCost() {
	}

	public static int forLevel(final int level) {
		if (level <= 1) {
			return EMERALDS_PER_LEVEL[0];
		}
		return EMERALDS_PER_LEVEL[Math.min(level, EMERALDS_PER_LEVEL.length) - 1];
	}

	public static int countEmeralds(final Inventory inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(Items.EMERALD)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public static boolean tryCharge(final ServerPlayer player, final int cost) {
		Inventory inventory = player.getInventory();
		if (countEmeralds(inventory) < cost) {
			return false;
		}

		int remaining = cost;
		for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(Items.EMERALD)) {
				int taken = Math.min(stack.getCount(), remaining);
				stack.shrink(taken);
				remaining -= taken;
			}
		}
		return true;
	}
}
