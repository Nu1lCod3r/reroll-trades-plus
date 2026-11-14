package com.nullcoder.rerollplusplus.client.mixin;

import com.nullcoder.rerollplusplus.client.EnchantRerollButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	public EnchantmentScreenMixin(final EnchantmentMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void rerollTradesPlus$addRerollButton(final CallbackInfo ci) {
		AbstractContainerScreenAccessor self = (AbstractContainerScreenAccessor) this;
		// Outside the panel, next to its left edge at the top — a side-tab button.
		this.addRenderableWidget(new EnchantRerollButton(
			this.menu,
			self.rerollTradesPlus$getLeftPos() - 24,
			self.rerollTradesPlus$getTopPos() + 2
		));
	}
}
