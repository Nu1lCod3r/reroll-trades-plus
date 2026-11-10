package com.nullcoder.rerolltradesplus;

import com.nullcoder.rerolltradesplus.mixin.AbstractVillagerInvoker;
import com.nullcoder.rerolltradesplus.mixin.MerchantMenuAccessor;
import com.nullcoder.rerolltradesplus.mixin.VillagerInvoker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerollTradesPlus implements ModInitializer {
	public static final String MOD_ID = "reroll_trades_plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.TYPE, RerollTradesPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.TYPE, (payload, context) -> rerollTrades(context.player()));
		LOGGER.info("Reroll Trades Plus initialized");
	}

	/**
	 * Regenerates the offers of the villager the player is currently trading with,
	 * charging emeralds based on the villager's trade level.
	 * Mirrors vanilla offer generation: a fresh villager accumulates its trades
	 * level by level, so we replay every trade set up to the villager's level.
	 */
	private static void rerollTrades(final ServerPlayer player) {
		AbstractContainerMenu openMenu = player.containerMenu;
		if (!(openMenu instanceof MerchantMenu menu)) {
			return;
		}

		Merchant merchant = ((MerchantMenuAccessor) menu).rerollTradesPlus$getTrader();
		if (!(merchant instanceof Villager villager)) {
			return;
		}
		if (!(villager.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (player.isSpectator() || !menu.stillValid(player)) {
			return;
		}

		VillagerData villagerData = villager.getVillagerData();
		VillagerProfession profession = villagerData.profession().value();
		int level = villagerData.level();

		// Never charge for villagers that can never have trades (e.g. nitwits).
		if (!hasTradeSets(profession, level)) {
			player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".no_trades"), true);
			return;
		}

		int cost = RerollCost.forLevel(level);
		if (!RerollCost.tryCharge(player, cost)) {
			player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".not_enough_emeralds", cost), true);
			return;
		}

		villager.setOffers(new MerchantOffers());
		for (int lvl = 1; lvl <= level; lvl++) {
			ResourceKey<TradeSet> tradeSet = profession.getTrades(lvl);
			if (tradeSet != null) {
				((AbstractVillagerInvoker) villager).rerollTradesPlus$invokeAddOffersFromTradeSet(serverLevel, villager.getOffers(), tradeSet);
			}
		}

		villager.setTradingPlayer(player);
		// Re-applies reputation/hero discounts and syncs the new offers to the client.
		((VillagerInvoker) villager).rerollTradesPlus$invokeUpdateSpecialPrices(player);
	}

	private static boolean hasTradeSets(final VillagerProfession profession, final int level) {
		for (int lvl = 1; lvl <= level; lvl++) {
			if (profession.getTrades(lvl) != null) {
				return true;
			}
		}
		return false;
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
