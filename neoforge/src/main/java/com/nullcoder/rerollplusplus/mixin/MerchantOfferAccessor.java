package com.nullcoder.rerollplusplus.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {

	@Accessor("uses")
	void rerollTradesPlus$setUses(int uses);

	@Accessor("demand")
	void rerollTradesPlus$setDemand(int demand);

}
