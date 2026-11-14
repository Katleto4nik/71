package com.fpsshower;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * Просте меню (відкривається на Right Shift): вибір режиму і зміна кнопки відкриття.
 * Перебінд ловиться в FpsShowerClient.onTick (опитуванням GLFW), тому тут не перевизначаємо
 * методи вводу - так менше ризику поламатись між версіями.
 */
public class FpsShowerScreen extends Screen {
	private static final int BUTTON_W = 220;
	private static final int BUTTON_H = 20;
	private static final int GAP = 4;

	private final FpsShowerConfig.Mode[] modes = FpsShowerConfig.Mode.values();
	private final ButtonWidget[] modeButtons = new ButtonWidget[modes.length];
	private ButtonWidget keyButton;
	private boolean rebinding = false;

	public FpsShowerScreen() {
		super(Text.literal("FPS shower"));
	}

	@Override
	protected void init() {
		int x = this.width / 2 - BUTTON_W / 2;
		int y = this.height / 2 - 55;

		for (int i = 0; i < modes.length; i++) {
			FpsShowerConfig.Mode mode = modes[i];
			modeButtons[i] = this.addDrawableChild(
				ButtonWidget.builder(modeLabel(mode), button -> {
					FpsShowerConfig.mode = mode;
					FpsShowerConfig.save();
					refreshLabels();
				}).dimensions(x, y + i * (BUTTON_H + GAP), BUTTON_W, BUTTON_H).build()
			);
		}

		int keyY = y + modes.length * (BUTTON_H + GAP) + 8;
		keyButton = this.addDrawableChild(
			ButtonWidget.builder(keyLabel(), button -> {
				rebinding = true;
				refreshLabels();
			}).dimensions(x, keyY, BUTTON_W, BUTTON_H).build()
		);

		this.addDrawableChild(
			ButtonWidget.builder(Text.literal("Готово"), button -> this.close())
				.dimensions(x, keyY + BUTTON_H + GAP, BUTTON_W, BUTTON_H).build()
		);
	}

	private static Text modeLabel(FpsShowerConfig.Mode mode) {
		String name = switch (mode) {
			case CROSSBOW -> "Арбалет";
			case PEARL -> "Ендер-перл";
			case SHIELD -> "Щит";
		};
		if (FpsShowerConfig.mode == mode) {
			return Text.literal("» " + name + " «").formatted(Formatting.GREEN);
		}
		return Text.literal(name);
	}

	private Text keyLabel() {
		if (rebinding) {
			return Text.literal("Натисни клавішу (Esc - скасувати)").formatted(Formatting.YELLOW);
		}
		Text keyName = InputUtil.Type.KEYSYM.createFromCode(FpsShowerConfig.menuKey).getLocalizedText();
		return Text.literal("Кнопка меню: ").append(keyName);
	}

	private void refreshLabels() {
		for (int i = 0; i < modes.length; i++) {
			if (modeButtons[i] != null) {
				modeButtons[i].setMessage(modeLabel(modes[i]));
			}
		}
		if (keyButton != null) {
			keyButton.setMessage(keyLabel());
		}
	}

	public boolean isRebinding() {
		return rebinding;
	}

	/** Викликається з тіку, коли в режимі перебінду натиснули клавішу. */
	public void finishRebind(int key) {
		rebinding = false;
		if (key != GLFW.GLFW_KEY_ESCAPE) {
			FpsShowerConfig.menuKey = key;
			FpsShowerConfig.save();
		}
		refreshLabels();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int top = this.height / 2 - 55;

		Text title = Text.literal("FPS shower").formatted(Formatting.BOLD);
		context.drawText(this.textRenderer, title,
			(this.width - this.textRenderer.getWidth(title)) / 2, top - 30, 0xFFFFFFFF, true);

		Text hint = Text.literal("ПКМ з мечем у руці = обраний режим");
		context.drawText(this.textRenderer, hint,
			(this.width - this.textRenderer.getWidth(hint)) / 2, top - 16, 0xFFAAAAAA, true);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// Під час перебінду Esc лише скасовує вибір клавіші
		return !rebinding;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
