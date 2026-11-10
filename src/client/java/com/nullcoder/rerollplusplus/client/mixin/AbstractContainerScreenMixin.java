package com.nullcoder.rerollplusplus.client.mixin;

import com.nullcoder.rerollplusplus.RerollTradesPayload;
import com.nullcoder.rerollplusplus.client.RerollTradesPlusClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void rerollTradesPlus$onKeyPressed(final KeyEvent event, final CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof MerchantScreen)) {
			return;
		}
		if (!ClientPlayNetworking.canSend(RerollTradesPayload.TYPE)) {
			return;
		}
		if (!RerollTradesPlusClient.REROLL_KEY.matches(event)) {
			return;
		}

		ClientPlayNetworking.send(RerollTradesPayload.INSTANCE);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		cir.setReturnValue(true);
	}

}
