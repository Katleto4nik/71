package com.fpsshower;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FpsShowerClient implements ClientModInitializer {
	public static final String MOD_ID = "fps_shower";

	// --- лічильник FPS (рахуємо реально відмальовані кадри) ---
	private static int frames = 0;
	private static int fps = 0;
	private static long lastSample = System.nanoTime();

	// --- клавіші ---
	private static boolean menuKeyWasDown = false;
	private static final boolean[] PREV_KEYS = new boolean[GLFW.GLFW_KEY_LAST + 1];

	@Override
	public void onInitializeClient() {
		FpsShowerConfig.load();

		// Мод не додає жодних KeyBinding, тому в "Керування" його немає.
		ClientTickEvents.END_CLIENT_TICK.register(FpsShowerClient::onTick);

		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.of(MOD_ID, "fps_counter"),
			(context, tickCounter) -> renderFps(context)
		);
	}

	private static void renderFps(DrawContext context) {
		frames++;
		long now = System.nanoTime();
		long elapsed = now - lastSample;
		if (elapsed >= 500_000_000L) {
			fps = (int) Math.round(frames * 1_000_000_000.0 / elapsed);
			frames = 0;
			lastSample = now;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		context.drawText(client.textRenderer, "FPS: " + fps, 4, 4, 0xFFFFFFFF, true);
	}

	private static boolean isDown(long handle, int key) {
		return key >= GLFW.GLFW_KEY_SPACE && GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
	}

	/** Оновлює PREV_KEYS і повертає першу щойно натиснуту клавішу (або -1). */
	private static int scanKeys(long handle) {
		int found = -1;
		for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
			boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
			if (down && !PREV_KEYS[key] && found == -1) {
				found = key;
			}
			PREV_KEYS[key] = down;
		}
		return found;
	}

	private static void onTick(MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		long handle = client.getWindow().getHandle();

		boolean menuDown = isDown(handle, FpsShowerConfig.menuKey);
		boolean menuPressed = menuDown && !menuKeyWasDown;

		if (client.currentScreen instanceof FpsShowerScreen screen) {
			int pressed = scanKeys(handle);
			if (screen.isRebinding()) {
				if (pressed != -1) {
					screen.finishRebind(pressed);
				}
			} else if (menuPressed) {
				screen.close();
			}
		} else if (client.currentScreen == null && client.player != null && menuPressed) {
			client.setScreen(new FpsShowerScreen());
		}

		// Перераховуємо вже з актуальною клавішею (після перебінду), щоб меню не закрилось саме
		menuKeyWasDown = isDown(handle, FpsShowerConfig.menuKey);

		SwordSwap.tick(client);
	}
}
