package com.nullcoder.rerollplusplus;

import com.nullcoder.rerollplusplus.mixin.AbstractVillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.MerchantMenuAccessor;
import com.nullcoder.rerollplusplus.mixin.MerchantOfferAccessor;
import com.nullcoder.rerollplusplus.mixin.VillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.WanderingTraderInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
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
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RerollTradesPlus.MOD_ID)
public class RerollTradesPlus {
	public static final String MOD_ID = "reroll_plus_plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Minimum interval between reroll attempts per player: held keys or macros must not spam packets, sounds and particles. */
	private static final long REROLL_COOLDOWN_NANOS = 500_000_000L;
	private static final WeakHashMap<ServerPlayer, Long> LAST_REROLL_AT = new WeakHashMap<>();

	public RerollTradesPlus(final IEventBus modEventBus) {
		modEventBus.addListener(RegisterPayloadHandlersEvent.class, RerollTradesPlus::onRegisterPayloads);
		LOGGER.info("Reroll Plus Plus initialized");
	}

	/**
	 * Registers the serverbound reroll payload. The handler runs on the server thread
	 * (MAIN), mirroring the Fabric ServerPlayNetworking behavior.
	 */
	private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
		event.registrar("1")
			.executesOn(HandlerThread.MAIN)
			.playToServer(RerollTradesPayload.TYPE, RerollTradesPayload.CODEC,
				(payload, context) -> rerollTrades((ServerPlayer) context.player()));
	}

	/**
	 * Regenerates the offers of the merchant the player is currently trading with,
	 * charging emeralds based on the merchant's trade level. Supports villagers and
	 * wandering traders. Mirrors vanilla offer generation: a fresh villager accumulates
	 * its trades level by level, so we replay every trade set up to its level.
	 */
	private static void rerollTrades(final ServerPlayer player) {
		// Rate limit first: rerolling is a deliberate action, so a held hotkey or macro must not
		// hammer the server with packets (each one broadcasts sounds and particles to nearby players).
		long now = System.nanoTime();
		Long lastRerollAt = LAST_REROLL_AT.get(player);
		if (lastRerollAt != null && now - lastRerollAt < REROLL_COOLDOWN_NANOS) {
			return;
		}
		LAST_REROLL_AT.put(player, now);

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
		if (player.isSpectator() || !entity.isAlive() || !menu.stillValid(player)) {
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

			// Snapshot before wiping: identical trades must keep their sold-out state (see carryTradeState).
			List<MerchantOffer> oldOffers = List.copyOf(villager.getOffers());
			villager.setOffers(new MerchantOffers());
			for (int lvl = 1; lvl <= villagerData.level(); lvl++) {
				ResourceKey<TradeSet> tradeSet = profession.getTrades(lvl);
				if (tradeSet != null) {
					((AbstractVillagerInvoker) villager).rerollTradesPlus$invokeAddOffersFromTradeSet(serverLevel, villager.getOffers(), tradeSet);
				}
			}
			carryTradeState(oldOffers, villager.getOffers());

			villager.setTradingPlayer(player);
			// Re-applies reputation/hero discounts and syncs the new offers to the client.
			((VillagerInvoker) villager).rerollTradesPlus$invokeUpdateSpecialPrices(player);
		} else {
			WanderingTrader trader = (WanderingTrader) merchant;
			List<MerchantOffer> oldOffers = List.copyOf(trader.getOffers());
			trader.getOffers().clear();
			((WanderingTraderInvoker) trader).rerollTradesPlus$invokeUpdateTrades(serverLevel);
			carryTradeState(oldOffers, trader.getOffers());
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

	/**
	 * Preserves trade state across a reroll: an offer identical to a previous one (same costs
	 * and result) keeps its uses and demand, so a reroll changes the set of trades but never
	 * restocks them. Without this, rerolling would unlock trades the player has sold out and
	 * reset demand-driven price increases — an infinite restock for the price of one reroll.
	 * Offers new to the set start fresh, as vanilla restock never applies to them either.
	 */
	private static void carryTradeState(final List<MerchantOffer> oldOffers, final MerchantOffers newOffers) {
		List<MerchantOffer> unmatched = new ArrayList<>(oldOffers);
		for (MerchantOffer offer : newOffers) {
			for (int i = 0; i < unmatched.size(); i++) {
				MerchantOffer old = unmatched.get(i);
				if (sameTrade(old, offer)) {
					unmatched.remove(i);
					((MerchantOfferAccessor) offer).rerollTradesPlus$setUses(Math.min(old.getUses(), offer.getMaxUses()));
					((MerchantOfferAccessor) offer).rerollTradesPlus$setDemand(old.getDemand());
					break;
				}
			}
		}
	}

	private static boolean sameTrade(final MerchantOffer a, final MerchantOffer b) {
		return a.getItemCostA().equals(b.getItemCostA())
			&& a.getItemCostB().equals(b.getItemCostB())
			&& a.getResult().equals(b.getResult());
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
