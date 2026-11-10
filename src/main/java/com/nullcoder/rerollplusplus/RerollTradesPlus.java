package com.nullcoder.rerollplusplus;

import com.nullcoder.rerollplusplus.mixin.AbstractVillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.MerchantMenuAccessor;
import com.nullcoder.rerollplusplus.mixin.VillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.WanderingTraderInvoker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerollTradesPlus implements ModInitializer {
	public static final String MOD_ID = "reroll_plus_plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.TYPE, RerollTradesPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.TYPE, (payload, context) -> rerollTrades(context.player()));
		LOGGER.info("Reroll Trades Plus initialized");
	}

	/**
	 * Regenerates the offers of the merchant the player is currently trading with,
	 * charging emeralds based on the merchant's trade level. Supports villagers and
	 * wandering traders. Mirrors vanilla offer generation: a fresh villager accumulates
	 * its trades level by level, so we replay every trade set up to its level.
	 */
	private static void rerollTrades(final ServerPlayer player) {
		AbstractContainerMenu openMenu = player.containerMenu;
		if (!(openMenu instanceof MerchantMenu menu)) {
			return;
		}

		Merchant merchant = ((MerchantMenuAccessor) menu).rerollTradesPlus$getTrader();
		boolean isVillager = merchant instanceof Villager;
		if (!isVillager && !(merchant instanceof WanderingTrader)) {
			return;
		}
		Entity entity = (Entity) merchant;
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (player.isSpectator() || !menu.stillValid(player)) {
			return;
		}

		int level = isVillager ? ((Villager) merchant).getVillagerData().level() : 1;

		// Villagers without any trade sets (e.g. nitwits) would reroll into nothing — refuse without charging.
		if (isVillager) {
			VillagerData data = ((Villager) merchant).getVillagerData();
			if (!hasTradeSets(data.profession().value(), data.level())) {
				player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".no_trades"), true);
				return;
			}
		}

		// The merchant may refuse: the higher their trade level, the likelier a refusal. Nothing is charged.
		double refusal = isVillager ? RerollOdds.refusalChance(level) : RerollOdds.WANDERING_TRADER_REFUSAL;
		if (entity.getRandom().nextDouble() < refusal) {
			serverLevel.playSound(null, entity.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
			serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY() + 1.6, entity.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
			player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".refused"), true);
			return;
		}

		int cost = RerollCost.forLevel(level);
		if (!RerollCost.tryCharge(player, cost)) {
			player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".not_enough_emeralds", cost), true);
			return;
		}

		if (isVillager) {
			Villager villager = (Villager) merchant;
			VillagerData villagerData = villager.getVillagerData();
			VillagerProfession profession = villagerData.profession().value();

			villager.setOffers(new MerchantOffers());
			for (int lvl = 1; lvl <= villagerData.level(); lvl++) {
				ResourceKey<TradeSet> tradeSet = profession.getTrades(lvl);
				if (tradeSet != null) {
					((AbstractVillagerInvoker) villager).rerollTradesPlus$invokeAddOffersFromTradeSet(serverLevel, villager.getOffers(), tradeSet);
				}
			}

			villager.setTradingPlayer(player);
			// Re-applies reputation/hero discounts and syncs the new offers to the client.
			((VillagerInvoker) villager).rerollTradesPlus$invokeUpdateSpecialPrices(player);
		} else {
			WanderingTrader trader = (WanderingTrader) merchant;
			trader.getOffers().clear();
			((WanderingTraderInvoker) trader).rerollTradesPlus$invokeUpdateTrades(serverLevel);
			player.sendMerchantOffers(menu.containerId, trader.getOffers(), 1, trader.getVillagerXp(), menu.showProgressBar(), menu.canRestock());
		}

		// Celebration: villager voice + happy particles around the merchant.
		serverLevel.playSound(null, entity.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
		serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
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
