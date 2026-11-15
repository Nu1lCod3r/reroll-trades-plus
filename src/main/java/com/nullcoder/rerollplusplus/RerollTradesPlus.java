package com.nullcoder.rerollplusplus;

import com.nullcoder.rerollplusplus.mixin.AbstractVillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.MerchantMenuAccessor;
import com.nullcoder.rerollplusplus.mixin.MerchantOfferAccessor;
import com.nullcoder.rerollplusplus.mixin.VillagerInvoker;
import com.nullcoder.rerollplusplus.mixin.WanderingTraderInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerollTradesPlus implements ModInitializer {
	public static final String MOD_ID = "reroll_plus_plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Minimum interval between reroll attempts per player: held keys or macros must not spam packets, sounds and particles. */
	private static final long REROLL_COOLDOWN_NANOS = 500_000_000L;
	private static final WeakHashMap<ServerPlayer, Long> LAST_REROLL_AT = new WeakHashMap<>();

	@Override
	public void onInitialize() {
		RerollConfig.load(FabricLoader.getInstance().getConfigDir());
		RerollItems.register();

		PayloadTypeRegistry.serverboundPlay().register(RerollTradesPayload.TYPE, RerollTradesPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RerollPriceSyncPayload.TYPE, RerollPriceSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RerollTradesPayload.TYPE, (payload, context) -> rerollTrades(context.player()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
			.register(entries -> entries.accept(RerollItems.REROLL_TOKEN));

		LOGGER.info("Reroll Plus Plus initialized");
	}

	/**
	 * Regenerates the offers of the merchant the player is currently trading with, charging
	 * emeralds based on the merchant's trade level. Supports villagers and wandering traders.
	 * 3.0 extras: golden rerolls (jackpot: no refusal + one elite 1-emerald offer), merchant
	 * fatigue (repeated rerolls of one merchant in a day cost more), the reroll token (rescues
	 * a failing reroll), explorer map re-targeting for cartographers, and an advancement/stat trail.
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
		if (isVillager && !RerollConfig.INSTANCE.enableVillagerReroll) {
			return;
		}
		if (!isVillager) {
			if (!(merchant instanceof WanderingTrader) || !RerollConfig.INSTANCE.enableWanderingTraderReroll) {
				return;
			}
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

		RerollConfig config = RerollConfig.INSTANCE;

		// Golden reroll: the jackpot. The merchant cannot refuse, and one fresh offer becomes elite.
		boolean golden = config.enableGoldenReroll && entity.getRandom().nextDouble() < config.goldenRerollChance;
		boolean usedToken = false;

		// The merchant may refuse: the higher their trade level, the likelier a refusal.
		// A reroll token overrules the refusal (and is consumed instead of emeralds).
		if (!golden) {
			double refusal = (isVillager ? RerollOdds.refusalChance(level) : RerollOdds.WANDERING_TRADER_REFUSAL)
				* config.refusalChanceScale;
			if (entity.getRandom().nextDouble() < refusal) {
				if (config.enableRerollToken && consumeToken(player)) {
					usedToken = true;
					player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".token_refusal"), true);
				} else {
					serverLevel.playSound(null, entity.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
					serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY() + 1.6, entity.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
					player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".refused"), true);
					return;
				}
			}
		}

		// Fatigue: rerolling the same merchant within one game day smoothly raises the price.
		int basePrice = RerollCost.forLevel(level);
		int price = RerollFatigue.price(serverLevel, merchant, basePrice);
		if (!usedToken && !RerollCost.tryCharge(player, price)) {
			if (config.enableRerollToken && consumeToken(player)) {
				usedToken = true;
				player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".token_used"), true);
			} else {
				player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".not_enough_emeralds", price), true);
				return;
			}
		}

		boolean retargetedMap = false;
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

			// Cartographer reroll extra: explorer maps lead to a different structure instance.
			if (config.enableMapReroll) {
				retargetedMap = ExplorerMaps.retarget(serverLevel, villager.getOffers(), entity.blockPosition(), entity.getRandom());
			}

			villager.setTradingPlayer(player);
			// Re-applies reputation/hero discounts and syncs the new offers to the client.
			((VillagerInvoker) villager).rerollTradesPlus$invokeUpdateSpecialPrices(player);
			if (golden) {
				makeElite(villager.getOffers(), entity.getRandom());
			}
		} else {
			WanderingTrader trader = (WanderingTrader) merchant;
			List<MerchantOffer> oldOffers = List.copyOf(trader.getOffers());
			trader.getOffers().clear();
			((WanderingTraderInvoker) trader).rerollTradesPlus$invokeUpdateTrades(serverLevel);
			carryTradeState(oldOffers, trader.getOffers());
			if (golden) {
				makeElite(trader.getOffers(), entity.getRandom());
			}
			player.sendMerchantOffers(menu.containerId, trader.getOffers(), 1, trader.getVillagerXp(), menu.showProgressBar(), menu.canRestock());
		}

		// Fatigue and the button price for the NEXT reroll.
		RerollFatigue.bump(serverLevel, merchant);
		int nextPrice = RerollFatigue.price(serverLevel, merchant, basePrice);
		ServerPlayNetworking.send(player, new RerollPriceSyncPayload(menu.containerId, nextPrice));

		// Statistics and advancement trail.
		RerollItems.awardRerollStat(player);
		int totalCount = RerollItems.rerollCount(player);
		RerollItems.REROLL.trigger(player, golden, usedToken, retargetedMap, level, totalCount);

		// Celebration: villager voice + particles around the merchant; gold gets the jackpot treatment.
		if (golden) {
			serverLevel.playSound(null, entity.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0F, 1.0F);
			serverLevel.sendParticles(ParticleTypes.WAX_ON, entity.getX(), entity.getY() + 1.2, entity.getZ(), 40, 0.8, 0.6, 0.8, 0.05);
			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
			player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".golden"), true);
		} else {
			serverLevel.playSound(null, entity.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
		}
	}

	/**
	 * Golden reroll jackpot: one fresh offer is re-priced down to a single emerald, regardless
	 * of level or reputation. Applied after updateSpecialPrices so it wins over reputation pricing.
	 */
	private static void makeElite(final MerchantOffers offers, final RandomSource random) {
		List<MerchantOffer> candidates = new ArrayList<>();
		for (MerchantOffer offer : offers) {
			if (offer.getBaseCostA().getCount() >= 2) {
				candidates.add(offer);
			}
		}
		if (candidates.isEmpty()) {
			return;
		}
		MerchantOffer elite = candidates.get(random.nextInt(candidates.size()));
		elite.setSpecialPriceDiff(1 - elite.getBaseCostA().getCount());
	}

	/** Finds and consumes one reroll token from anywhere in the player's inventory (incl. offhand). */
	private static boolean consumeToken(final ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(RerollItems.REROLL_TOKEN)) {
				stack.consume(1, player);
				return true;
			}
		}
		return false;
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
