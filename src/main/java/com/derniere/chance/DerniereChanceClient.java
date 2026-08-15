package com.derniere.chance;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class DerniereChanceClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DerniereChanceMod.ATTEMPTS_SYNC_PACKET, (client, handler, buf, responseSender) -> {
			int attempts = buf.readInt();
			boolean permanentlyDead = buf.readBoolean();

			client.execute(() -> ClientCombatData.update(attempts, permanentlyDead));
		});
	}
}
