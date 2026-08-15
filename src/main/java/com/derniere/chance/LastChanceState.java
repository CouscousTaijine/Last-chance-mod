package com.derniere.chance;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stocke, pour chaque joueur (par UUID), le nombre de fois où il a utilisé
 * "Dernière Chance" (ce qui pilote la difficulté du combat suivant) ainsi
 * que le fait qu'il soit définitivement mort ou non.
 *
 * Les données sont sauvegardées dans le fichier de données du monde
 * (data/derniere_chance.dat) et persistent entre les sessions.
 */
public class LastChanceState extends PersistentState {

	private static final String STORAGE_KEY = DerniereChanceMod.MOD_ID;

	private final Map<UUID, Integer> attempts = new HashMap<>();
	private final Map<UUID, Boolean> permanentlyDead = new HashMap<>();

	public static LastChanceState get(ServerWorld world) {
		// On utilise toujours l'overworld comme "source de vérité" pour que les
		// données restent cohérentes même si le joueur meurt dans une autre dimension.
		ServerWorld overworld = world.getServer().getOverworld();
		PersistentStateManager manager = overworld.getPersistentStateManager();
		return manager.getOrCreate(LastChanceState::fromNbt, LastChanceState::new, STORAGE_KEY);
	}

	/** Nombre de tentatives déjà effectuées par ce joueur (0 = jamais utilisé). */
	public int getAttempts(UUID uuid) {
		return attempts.getOrDefault(uuid, 0);
	}

	/** Incrémente le compteur de tentatives pour ce joueur. */
	public void registerAttempt(UUID uuid) {
		attempts.merge(uuid, 1, Integer::sum);
	}

	public boolean isPermanentlyDead(UUID uuid) {
		return permanentlyDead.getOrDefault(uuid, false);
	}

	public void setPermanentlyDead(UUID uuid, boolean dead) {
		permanentlyDead.put(uuid, dead);
	}

	public static LastChanceState fromNbt(NbtCompound tag) {
		LastChanceState state = new LastChanceState();

		NbtList list = tag.getList("Players", 10); // 10 = NbtCompound
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			UUID uuid = entry.getUuid("Uuid");
			state.attempts.put(uuid, entry.getInt("Attempts"));
			state.permanentlyDead.put(uuid, entry.getBoolean("PermanentlyDead"));
		}

		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound tag) {
		NbtList list = new NbtList();

		for (UUID uuid : attempts.keySet()) {
			NbtCompound entry = new NbtCompound();
			entry.putUuid("Uuid", uuid);
			entry.putInt("Attempts", attempts.getOrDefault(uuid, 0));
			entry.putBoolean("PermanentlyDead", permanentlyDead.getOrDefault(uuid, false));
			list.add(entry);
		}

		tag.put("Players", list);
		return tag;
	}
}
