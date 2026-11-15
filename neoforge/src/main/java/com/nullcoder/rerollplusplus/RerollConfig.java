package com.nullcoder.rerollplusplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side config: gameplay numbers live here and apply on the side that owns the world.
 * Loaded once at mod init from {@code <game dir>/config/reroll_plus_plus.json}; on first run
 * the file is written with the defaults. Editing requires a restart.
 *
 * <p>The client loads its own copy, but all gameplay logic reads the config of the side that
 * processes the reroll (the integrated or dedicated server).
 */
public final class RerollConfig {
	public static final RerollConfig INSTANCE = new RerollConfig();

	/** Master switches. */
	public boolean enableVillagerReroll = true;
	public boolean enableWanderingTraderReroll = true;
	public boolean enableEnchantReroll = true;
	public boolean enableGoldenReroll = true;
	public boolean enableMerchantFatigue = true;
	public boolean enableRerollToken = true;
	public boolean enableMapReroll = true;

	/** Base price multiplier applied to every emerald reroll price. */
	public double priceMultiplier = 1.0;
	/** Scale applied to every refusal chance (1.0 = vanilla-mod default odds). */
	public double refusalChanceScale = 1.0;
	/** Chance that a reroll turns golden: no refusal and one offer becomes an elite 1-emerald deal. */
	public double goldenRerollChance = 0.05;
	/** Price increase per extra reroll of the same merchant within one game day (0.5 = +50% each). */
	public double fatiguePriceStep = 0.5;
	/** Upper bound for the fatigue multiplier (5.0 = rerolls never cost more than 5x the base price). */
	public double fatigueMaxMultiplier = 5.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private RerollConfig() {
	}

	public static void load(final Path configDir) {
		Path file = configDir.resolve("reroll_plus_plus.json");
		if (Files.isRegularFile(file)) {
			try {
				RerollConfig loaded = GSON.fromJson(Files.readString(file), RerollConfig.class);
				if (loaded != null) {
					copy(loaded, INSTANCE);
					return;
				}
			} catch (IOException | com.google.gson.JsonParseException e) {
				RerollTradesPlus.LOGGER.error("Failed to read reroll_plus_plus.json, using defaults", e);
				return;
			}
		}
		save(configDir);
	}

	public static void save(final Path configDir) {
		try {
			Files.createDirectories(configDir);
			Files.writeString(configDir.resolve("reroll_plus_plus.json"), GSON.toJson(INSTANCE));
		} catch (IOException e) {
			RerollTradesPlus.LOGGER.error("Failed to save reroll_plus_plus.json", e);
		}
	}

	private static void copy(final RerollConfig from, final RerollConfig to) {
		to.enableVillagerReroll = from.enableVillagerReroll;
		to.enableWanderingTraderReroll = from.enableWanderingTraderReroll;
		to.enableEnchantReroll = from.enableEnchantReroll;
		to.enableGoldenReroll = from.enableGoldenReroll;
		to.enableMerchantFatigue = from.enableMerchantFatigue;
		to.enableRerollToken = from.enableRerollToken;
		to.enableMapReroll = from.enableMapReroll;
		to.priceMultiplier = from.priceMultiplier;
		to.refusalChanceScale = from.refusalChanceScale;
		to.goldenRerollChance = from.goldenRerollChance;
		to.fatiguePriceStep = from.fatiguePriceStep;
		to.fatigueMaxMultiplier = from.fatigueMaxMultiplier;
	}
}
