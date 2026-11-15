package com.nullcoder.rerollplusplus;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Cartographer reroll extra: explorer maps are otherwise deterministic — the rerolled offer
 * would always point at the same nearest structure. This re-creates every map offer in the
 * fresh set around a random offset origin, so the map leads to a different instance of the
 * same structure type (another mansion, monument, village, trial chambers...).
 */
public final class ExplorerMaps {
	private static final int SEARCH_RADIUS = 100;
	/** Distance between the old and the new target below which we consider them the same structure. */
	private static final double SAME_STRUCTURE_DISTANCE = 128.0;

	/** Explorer map item → its structure tag and map decoration, mirroring vanilla VillagerTrades. */
	private record MapKind(Item item, TagKey<Structure> tag, Holder<MapDecorationType> decoration) {
	}

	private static final List<MapKind> KINDS = List.of(
		new MapKind(Items.OCEAN_MONUMENT_MAP, StructureTags.ON_OCEAN_MONUMENT_MAPS, MapDecorationTypes.OCEAN_MONUMENT),
		new MapKind(Items.BURIED_TRIAL_CHAMBERS_MAP, StructureTags.ON_BURIED_TRIAL_CHAMBERS_MAPS, MapDecorationTypes.TRIAL_CHAMBERS),
		new MapKind(Items.WOODLAND_MANSION_MAP, StructureTags.ON_WOODLAND_MANSION_MAPS, MapDecorationTypes.WOODLAND_MANSION),
		new MapKind(Items.TAIGA_VILLAGE_MAP, StructureTags.ON_TAIGA_VILLAGE_MAPS, MapDecorationTypes.TAIGA_VILLAGE),
		new MapKind(Items.SWAMP_HUT_MAP, StructureTags.ON_SWAMP_HUT_MAPS, MapDecorationTypes.SWAMP_HUT),
		new MapKind(Items.SNOWY_VILLAGE_MAP, StructureTags.ON_SNOWY_VILLAGE_MAPS, MapDecorationTypes.SNOWY_VILLAGE),
		new MapKind(Items.SAVANNA_VILLAGE_MAP, StructureTags.ON_SAVANNA_VILLAGE_MAPS, MapDecorationTypes.SAVANNA_VILLAGE),
		new MapKind(Items.PLAINS_VILLAGE_MAP, StructureTags.ON_PLAINS_VILLAGE_MAPS, MapDecorationTypes.PLAINS_VILLAGE),
		new MapKind(Items.JUNGLE_PYRAMID_MAP, StructureTags.ON_JUNGLE_PYRAMID_MAPS, MapDecorationTypes.JUNGLE_TEMPLE),
		new MapKind(Items.DESERT_VILLAGE_MAP, StructureTags.ON_DESERT_VILLAGE_MAPS, MapDecorationTypes.DESERT_VILLAGE)
	);

	private ExplorerMaps() {
	}

	/**
	 * Re-targets every explorer map offer of the freshly generated set. Uses vanilla map
	 * creation (zoom 2, radius 100 chunks) exactly like the cartographer trade modifier.
	 *
	 * @return true when at least one map now points at a different structure instance
	 */
	public static boolean retarget(final ServerLevel level, final MerchantOffers offers, final BlockPos origin, final RandomSource random) {
		final HolderGetter<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		boolean anyRetargeted = false;

		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			ItemStack result = offer.getResult();
			MapKind kind = kindOf(result);
			if (kind == null) {
				continue;
			}

			BlockPos target = findDifferentStructure(level, structures.getOrThrow(kind.tag()), origin, oldTarget(result), random);
			if (target == null) {
				continue;
			}

			ItemStack newMap = new ItemStack(kind.item());
			MapItem.applyNewSavedData(level, newMap, target.getX(), target.getZ(), (byte) 2, true, true);
			MapItem.renderBiomePreviewMap(level, newMap);
			MapItemSavedData.addTargetDecoration(newMap, target, "+", kind.decoration());

			offers.set(i, new MerchantOffer(
				offer.getItemCostA(), offer.getItemCostB(), newMap,
				offer.getUses(), offer.getMaxUses(), offer.getPriceMultiplier()
			));
			anyRetargeted = true;
		}
		return anyRetargeted;
	}

	private static MapKind kindOf(final ItemStack result) {
		for (MapKind kind : KINDS) {
			if (result.is(kind.item())) {
				return kind;
			}
		}
		return null;
	}

	/** World position the old map points at, if any (from the map_decorations component). */
	private static BlockPos oldTarget(final ItemStack oldMap) {
		MapDecorations decorations = oldMap.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
		for (MapDecorations.Entry entry : decorations.decorations().values()) {
			return BlockPos.containing(entry.x(), 0, entry.z());
		}
		return null;
	}

	/**
	 * Searches for the structure type around a random offset origin — far enough that the
	 * nearest instance differs from the one the old map points at. Returns null when nothing
	 * suitable is found (the original map is then kept untouched).
	 */
	private static BlockPos findDifferentStructure(final ServerLevel level, final HolderSet<Structure> destination,
		final BlockPos origin, final BlockPos oldTarget, final RandomSource random) {
		for (int attempt = 0; attempt < 4; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 600 + random.nextDouble() * 400;
			BlockPos offsetOrigin = origin.offset(
				(int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance)
			);
			BlockPos candidate = level.findNearestMapStructure(destination, offsetOrigin, SEARCH_RADIUS, false);
			if (candidate == null) {
				continue;
			}
			if (oldTarget == null || !isSameStructure(candidate, oldTarget)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean isSameStructure(final BlockPos a, final BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz <= SAME_STRUCTURE_DISTANCE * SAME_STRUCTURE_DISTANCE;
	}
}
