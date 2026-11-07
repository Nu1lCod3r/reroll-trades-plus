package com.nullcoder.rerolltradesplus.client;

import com.nullcoder.rerolltradesplus.RerollCost;
import com.nullcoder.rerolltradesplus.RerollTradesPayload;
import com.nullcoder.rerolltradesplus.RerollTradesPlus;
import java.util.Collections;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

public class RerollButton extends AbstractButton {
	public static final int WIDTH = 16;
	public static final int HEIGHT = 16;
	/** Top-right corner of the trade list column, right of the "Trades" title. */
	public static final int X_OFFSET = 84;
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
		ClientPlayNetworking.send(RerollTradesPayload.INSTANCE);
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), frame, 0, WIDTH, HEIGHT, 48, 16);

		if (this.isHovered) {
			graphics.setTooltipForNextFrame(
				Minecraft.getInstance().font,
				Collections.singletonList(Component.translatable("tooltip." + RerollTradesPlus.MOD_ID + ".reroll", cost).getVisualOrderText()),
				mouseX, mouseY
			);
		}
	}

	@Override
	public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("button." + RerollTradesPlus.MOD_ID + ".reroll"));
	}
}
