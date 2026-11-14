package com.fpsshower;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;

/**
 * Логіка режимів. Спрацьовує ТІЛЬКИ коли в основній руці меч і ти натискаєш ПКМ.
 *
 * ПЕРЛ     - слот з перлом -> використати (кинути) -> назад на меч.
 * АРБАЛЕТ  - якщо заряджений: слот -> вистрілити -> назад на меч.
 *            якщо не заряджений: слот -> заряджати поки тримаєш ПКМ -> відпустив -> назад на меч.
 * ЩИТ      - слот зі щитом -> тримається поки тримаєш ПКМ -> відпустив -> назад на меч.
 */
public final class SwordSwap {
	private static boolean useWasDown = false;
	/** Слот предмета, який ми тимчасово тримаємо (-1 = нічого). */
	private static int heldSlot = -1;
	/** Слот меча, на який треба повернутись. */
	private static int returnSlot = -1;

	private SwordSwap() {
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		boolean useDown = client.options.useKey.isPressed();

		if (player == null || client.interactionManager == null) {
			heldSlot = -1;
			returnSlot = -1;
			useWasDown = useDown;
			return;
		}

		// Тимчасово тримаємо щит/арбалет - чекаємо, поки відпустиш ПКМ
		if (returnSlot >= 0) {
			if (!useDown || !player.isAlive() || client.currentScreen != null) {
				restore(player);
			}
			useWasDown = useDown;
			return;
		}

		boolean pressedNow = useDown && !useWasDown;
		useWasDown = useDown;

		if (!pressedNow || client.currentScreen != null || !player.isAlive() || player.isSpectator()) {
			return;
		}

		// Тільки якщо в руці меч
		if (!player.getMainHandStack().isIn(ItemTags.SWORDS)) {
			return;
		}

		PlayerInventory inventory = player.getInventory();

		switch (FpsShowerConfig.mode) {
			case PEARL -> {
				int slot = findInHotbar(inventory, Items.ENDER_PEARL);
				if (slot >= 0) {
					useOnce(client, player, slot);
				}
			}
			case CROSSBOW -> {
				int slot = findInHotbar(inventory, Items.CROSSBOW);
				if (slot < 0) {
					return;
				}
				if (CrossbowItem.isCharged(inventory.getStack(slot))) {
					useOnce(client, player, slot);
				} else {
					startHolding(client, player, slot);
				}
			}
			case SHIELD -> {
				int slot = findInHotbar(inventory, Items.SHIELD);
				if (slot >= 0) {
					startHolding(client, player, slot);
				}
			}
		}
	}

	private static int findInHotbar(PlayerInventory inventory, Item item) {
		for (int i = 0; i < 9; i++) {
			if (inventory.getStack(i).isOf(item)) {
				return i;
			}
		}
		return -1;
	}

	/** Вибрати слот, один раз використати предмет і одразу повернутись на меч. */
	private static void useOnce(MinecraftClient client, ClientPlayerEntity player, int slot) {
		PlayerInventory inventory = player.getInventory();
		int old = inventory.getSelectedSlot();

		inventory.setSelectedSlot(slot);
		client.interactionManager.interactItem(player, Hand.MAIN_HAND);
		player.swingHand(Hand.MAIN_HAND);
		inventory.setSelectedSlot(old);
	}

	/** Вибрати слот і почати використання (щит / зарядка арбалета). Далі ванільна гра сама тримає, поки ПКМ затиснутий. */
	private static void startHolding(MinecraftClient client, ClientPlayerEntity player, int slot) {
		PlayerInventory inventory = player.getInventory();
		returnSlot = inventory.getSelectedSlot();
		heldSlot = slot;

		inventory.setSelectedSlot(slot);
		client.interactionManager.interactItem(player, Hand.MAIN_HAND);
	}

	private static void restore(ClientPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		// Якщо гравець сам перемкнув слот під час утримання - не чіпаємо
		if (inventory.getSelectedSlot() == heldSlot) {
			inventory.setSelectedSlot(returnSlot);
		}
		heldSlot = -1;
		returnSlot = -1;
	}
}
