package com.nullcoder.rerollplusplus.client;

import com.nullcoder.rerollplusplus.RerollTradesPlus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.EnchantmentMenu;

public class EnchantRerollButton extends AbstractButton {
	public static final int WIDTH = 20;
	public static final int HEIGHT = 20;

	/** Menu button id understood by EnchantmentMenuMixin (vanilla enchant buttons are 0..2). */
	public static final int REROLL_BUTTON_ID = 3;

	private static final Identifier TEXTURE = RerollTradesPlus.id("textures/gui/reroll_button.png");
	private static final int FRAME_NORMAL = 0;
	private static final int FRAME_HOVERED = 16;
	private static final int FRAME_DISABLED = 32;

	private final EnchantmentMenu menu;

	public EnchantRerollButton(final EnchantmentMenu menu, final int x, final int y) {
		super(x, y, WIDTH, HEIGHT, Component.translatable("button." + RerollTradesPlus.MOD_ID + ".enchant"));
		this.menu = menu;
		this.setTooltip(Tooltip.create(Component.translatable("tooltip." + RerollTradesPlus.MOD_ID + ".enchant")));
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		// Vanilla container-button packet: the server-side EnchantmentMenuMixin handles id 3.
		Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, REROLL_BUTTON_ID);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
		// Usable only when something enchantable sits in the table and lapis is present; vanilla validates the rest.
		this.active = this.menu.getSlot(0).hasItem() && this.menu.getSlot(1).hasItem();

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
	}

	@Override
	public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("button." + RerollTradesPlus.MOD_ID + ".enchant"));
	}
}
