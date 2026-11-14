package com.nullcoder.rerollplusplus.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

	@Accessor("enchantSlots")
	Container rerollTradesPlus$getEnchantSlots();

	@Accessor("enchantmentSeed")
	DataSlot rerollTradesPlus$getEnchantmentSeed();

}
