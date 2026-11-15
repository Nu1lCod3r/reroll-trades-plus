package com.nullcoder.rerollplusplus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Fired after every successful reroll. Boolean conditions in the instance mean "the reroll
 * must have this flag set"; unset conditions accept anything, so a single trigger serves the
 * whole advancement tree.
 */
public class RerollTrigger extends SimpleCriterionTrigger<RerollTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(final ServerPlayer player, final boolean golden, final boolean usedToken, final boolean retargetedMap, final int level, final int totalCount) {
		this.trigger(player, t -> t.matches(golden, usedToken, retargetedMap, level, totalCount));
	}

	public record TriggerInstance(
		Optional<Holder<LootItemCondition>> player,
		boolean golden,
		boolean usedToken,
		boolean retargetedMap,
		MinMaxBounds.Ints level,
		MinMaxBounds.Ints totalCount
	) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
			i -> i.group(
					LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
					Codec.BOOL.optionalFieldOf("golden", false).forGetter(TriggerInstance::golden),
					Codec.BOOL.optionalFieldOf("used_token", false).forGetter(TriggerInstance::usedToken),
					Codec.BOOL.optionalFieldOf("retargeted_map", false).forGetter(TriggerInstance::retargetedMap),
					MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::level),
					MinMaxBounds.Ints.CODEC.optionalFieldOf("total_count", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::totalCount)
				)
				.apply(i, TriggerInstance::new)
		);

		public boolean matches(final boolean golden, final boolean usedToken, final boolean retargetedMap, final int level, final int totalCount) {
			return (!this.golden || golden)
				&& (!this.usedToken || usedToken)
				&& (!this.retargetedMap || retargetedMap)
				&& this.level.matches(level)
				&& this.totalCount.matches(totalCount);
		}
	}
}
