package com.derniere.chance;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée commun (serveur + client) du mod "Dernière Chance".
 *
 * Le mod ajoute une troisième option à l'écran de mort qui lance un
 * mini-jeu façon Undertale (esquive de projectiles avec un cœur).
 * - Victoire  -> le joueur réapparaît normalement.
 * - Défaite   -> le joueur est marqué comme "mort définitivement" et ne
 *                pourra plus jamais réapparaître dans cette partie tant
 *                qu'un administrateur ne l'aura pas réinitialisé.
 */
public class DerniereChanceMod implements ModInitializer {

	public static final String MOD_ID = "derniere_chance";
	public static final Logger LOGGER = LoggerFactory.getLogger("DerniereChance");

	/** Paquet envoyé par le client quand le combat "Dernière Chance" se termine. */
	public static final Identifier COMBAT_RESULT_PACKET = new Identifier(MOD_ID, "combat_result");

	/** Paquet envoyé par le serveur au client à la mort, pour synchroniser la difficulté. */
	public static final Identifier ATTEMPTS_SYNC_PACKET = new Identifier(MOD_ID, "attempts_sync");

	@Override
	public void onInitialize() {
		LOGGER.info("[Dernière Chance] Initialisation du mod...");

		ServerPlayNetworking.registerGlobalReceiver(COMBAT_RESULT_PACKET, (server, player, handler, buf, responseSender) -> {
			boolean won = buf.readBoolean();

			// On revient sur le thread principal du serveur avant de toucher au monde/joueur.
			server.execute(() -> handleCombatResult(player, won));
		});

		// À chaque mort d'un joueur, on lui envoie son nombre de tentatives actuel
		// pour que l'écran de combat client sache quelle difficulté appliquer.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return;

			LastChanceState state = LastChanceState.get(player.getServerWorld());
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeInt(state.getAttempts(player.getUuid()));
			buf.writeBoolean(state.isPermanentlyDead(player.getUuid()));
			ServerPlayNetworking.send(player, ATTEMPTS_SYNC_PACKET, buf);
		});
	}

	private static void handleCombatResult(ServerPlayerEntity player, boolean won) {
		LastChanceState state = LastChanceState.get(player.getServerWorld());

		if (won) {
			LOGGER.info("[Dernière Chance] {} a survécu au combat (tentative n°{}).", player.getName().getString(), state.getAttempts(player.getUuid()));
			// Réapparition normale, comme si le joueur avait cliqué sur "Réapparaître".
			ServerPlayerEntity respawned = server(player).getPlayerManager().respawnPlayer(player, false);
			state.registerAttempt(player.getUuid());
			state.markDirty();
		} else {
			LOGGER.info("[Dernière Chance] {} a échoué le combat. Mort définitive.", player.getName().getString());
			state.setPermanentlyDead(player.getUuid(), true);
			state.registerAttempt(player.getUuid());
			state.markDirty();
			// Le joueur reste sur l'écran "Game Over" côté client (aucune réapparition possible,
			// voir ServerPlayNetworkHandlerMixin qui bloque toute tentative future de respawn).
		}
	}

	private static net.minecraft.server.MinecraftServer server(ServerPlayerEntity player) {
		return player.getServer();
	}

	public static PacketByteBuf createResultBuffer(boolean won) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(won);
		return buf;
	}
}
