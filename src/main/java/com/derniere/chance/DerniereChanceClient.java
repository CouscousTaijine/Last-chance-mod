package com.derniere.chance;

import com.derniere.chance.gui.LastChanceCombatScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

public class DerniereChanceClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DerniereChanceMod.ATTEMPTS_SYNC_PACKET, (client, handler, buf, responseSender) -> {
			int attempts = buf.readInt();
			boolean permanentlyDead = buf.readBoolean();

			client.execute(() -> ClientCombatData.update(attempts, permanentlyDead));
		});

		// Filet de sécurité : si le client se déconnecte pour n'importe quelle
		// raison (perte de connexion, kick, etc.) pendant que l'écran de combat
		// "Dernière Chance" est affiché, on force le retour à l'écran titre au
		// lieu de rester bloqué sur un écran figé sans monde chargé derrière.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			client.execute(() -> {
				if (client.currentScreen instanceof LastChanceCombatScreen) {
					client.setScreen(new TitleScreen());
				}
			});
		});
	}
}
