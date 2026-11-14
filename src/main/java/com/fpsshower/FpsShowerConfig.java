package com.fpsshower;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Налаштування мода: обраний режим і кнопка відкриття меню.
 * Зберігається у config/fps_shower.properties.
 * Кнопка НЕ реєструється в ванільному меню "Керування" - змінити її можна тільки в меню мода.
 */
public final class FpsShowerConfig {
	public enum Mode {
		CROSSBOW,
		PEARL,
		SHIELD
	}

	public static int menuKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
	public static Mode mode = Mode.PEARL;

	private FpsShowerConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fps_shower.properties");
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}

		Properties props = new Properties();
		try (Reader reader = Files.newBufferedReader(path)) {
			props.load(reader);
			int key = Integer.parseInt(props.getProperty("menuKey", String.valueOf(menuKey)).trim());
			if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
				menuKey = key;
			}
			mode = Mode.valueOf(props.getProperty("mode", mode.name()).trim());
		} catch (IOException | IllegalArgumentException ignored) {
			// битий файл - лишаємо значення за замовчуванням
		}
	}

	public static void save() {
		Properties props = new Properties();
		props.setProperty("menuKey", String.valueOf(menuKey));
		props.setProperty("mode", mode.name());

		try (Writer writer = Files.newBufferedWriter(file())) {
			props.store(writer, "FPS shower");
		} catch (IOException ignored) {
			// не вдалося зберегти - не критично
		}
	}
}
