package com.nullcoder.rerollplusplus.mixin;

import com.nullcoder.rerollplusplus.RerollTradesPlus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds menu button id 3 to the enchanting table: re-rolls the three enchantment
 * offers for 1 lapis lazuli (taken from the table's lapis slot). The reroll is a
 * re-seed + vanilla regeneration through {@link EnchantmentMenu#slotsChanged},
 * so the new offers are fully vanilla-generated and auto-synced to the client.
 */
@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
	@Unique
	private static final int rerollTradesPlus$REROLL_BUTTON_ID = 3;
	@Unique
	private static final int rerollTradesPlus$REROLL_LAPIS_COST = 1;

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void rerollTradesPlus$onMenuButton(final Player player, final int buttonId, final CallbackInfoReturnable<Boolean> cir) {
		if (buttonId != rerollTradesPlus$REROLL_BUTTON_ID) {
			return;
		}

		cir.setReturnValue(this.rerollTradesPlus$reroll(player, (EnchantmentMenu) (Object) this));
	}

	@Unique
	private boolean rerollTradesPlus$reroll(final Player player, final EnchantmentMenu menu) {
		if (player.level().isClientSide() || player.isSpectator() || !menu.stillValid(player)) {
			return false;
		}

		Container enchantSlots = ((EnchantmentMenuAccessor) menu).rerollTradesPlus$getEnchantSlots();
		ItemStack item = enchantSlots.getItem(0);
		if (item.isEmpty() || !item.isEnchantable()) {
			return false;
		}

		ItemStack lapis = enchantSlots.getItem(1);
		if (!lapis.is(Items.LAPIS_LAZULI) || lapis.getCount() < rerollTradesPlus$REROLL_LAPIS_COST) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendSystemMessage(Component.translatable("message." + RerollTradesPlus.MOD_ID + ".need_lapis"), true);
			}
			return false;
		}

		// A fresh seed makes slotsChanged() roll brand-new offers; vanilla syncs them via broadcastChanges().
		DataSlot seed = ((EnchantmentMenuAccessor) menu).rerollTradesPlus$getEnchantmentSeed();
		seed.set(player.getRandom().nextInt());
		lapis.consume(rerollTradesPlus$REROLL_LAPIS_COST, player);
		if (lapis.isEmpty()) {
			enchantSlots.setItem(1, ItemStack.EMPTY);
		}

		menu.slotsChanged(enchantSlots);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		return true;
	}
}
