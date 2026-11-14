package com.nullcoder.rerollplusplus.client;

import com.nullcoder.rerollplusplus.RerollCost;
import com.nullcoder.rerollplusplus.RerollTradesPayload;
import com.nullcoder.rerollplusplus.RerollTradesPlus;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class RerollButton extends AbstractButton {
	public static final int WIDTH = 20;
	public static final int HEIGHT = 20;
	/** Outside the panel, next to its left edge at the top — a side-tab button. */
	public static final int X_OFFSET = -24;
	public static final int Y_OFFSET = 2;

	private static final Identifier TEXTURE = RerollTradesPlus.id("textures/gui/reroll_button.png");
	// Texture frames (48x16): 0 normal, 16 hovered, 32 disabled.
	private static final int FRAME_NORMAL = 0;
	private static final int FRAME_HOVERED = 16;
	private static final int FRAME_DISABLED = 32;

	private final MerchantMenu menu;

	public RerollButton(final MerchantMenu menu, final int x, final int y) {
		super(x, y, WIDTH, HEIGHT, Component.translatable("button." + RerollTradesPlus.MOD_ID + ".reroll"));
		this.menu = menu;
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		ClientPacketDistributor.sendToServer(RerollTradesPayload.INSTANCE);
	}

	@Override
	public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
		int cost = RerollCost.forLevel(this.menu.getTraderLevel());
		this.active = RerollCost.countEmeralds(Minecraft.getInstance().player.getInventory()) >= cost;

		int frame;
		if (!this.active) {
			frame = FRAME_DISABLED;
		} else if (this.isHovered) {
			frame = FRAME_HOVERED;
		} else {
			frame = FRAME_NORMAL;
		}
		// Vanilla button background (handles normal/hovered/disabled sprites), ring icon inset on top.
		this.extractDefaultSprite(graphics);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX() + 2, this.getY() + 2, frame, 0, WIDTH - 4, HEIGHT - 4, 48, 16);

		if (this.isHovered) {
			graphics.setTooltipForNextFrame(
				Minecraft.getInstance().font,
				Collections.singletonList(Component.translatable("tooltip." + RerollTradesPlus.MOD_ID + ".reroll", cost).getVisualOrderText()),
				mouseX, mouseY
			);
		}

		// Stack-count style cost label in the icon's corner.
		Component costLabel = Component.literal(String.valueOf(cost));
		int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
		graphics.text(
			Minecraft.getInstance().font,
			costLabel,
			this.getX() + 19 - Minecraft.getInstance().font.width(costLabel),
			this.getY() + 10,
			textColor, true
		);
	}

	@Override
	public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("button." + RerollTradesPlus.MOD_ID + ".reroll"));
	}
}
