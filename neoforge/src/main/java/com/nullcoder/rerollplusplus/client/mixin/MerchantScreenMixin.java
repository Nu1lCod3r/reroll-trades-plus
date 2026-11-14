package com.nullcoder.rerollplusplus.client.mixin;

import com.nullcoder.rerollplusplus.client.NeoForgeNetwork;
import com.nullcoder.rerollplusplus.client.RerollButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
	public MerchantScreenMixin(final MerchantMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, 276, 166);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void rerollTradesPlus$addRerollButton(final CallbackInfo ci) {
		// Only add the button when the server has the mod installed.
		if (!NeoForgeNetwork.canSendReroll()) {
			return;
		}
		// leftPos/topPos live on AbstractContainerScreen, so access them through the
		// accessor mixin targeting the declaring class (@Shadow cannot see inherited fields).
		AbstractContainerScreenAccessor self = (AbstractContainerScreenAccessor) this;
		this.addRenderableWidget(new RerollButton(
			this.menu,
			self.rerollTradesPlus$getLeftPos() + RerollButton.X_OFFSET,
			self.rerollTradesPlus$getTopPos() + RerollButton.Y_OFFSET
		));
	}
}
