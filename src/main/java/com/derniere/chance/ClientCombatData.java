package com.derniere.chance;

/**
 * Cache côté client des informations synchronisées par le serveur au
 * moment de la mort du joueur : combien de fois "Dernière Chance" a déjà
 * été utilisée (pour calculer la difficulté du prochain combat) et si le
 * joueur est déjà définitivement mort (auquel cas on ne propose plus
 * l'option du tout).
 */
public class ClientCombatData {

	private static int attempts = 0;
	private static boolean permanentlyDead = false;

	public static void update(int newAttempts, boolean newPermanentlyDead) {
		attempts = newAttempts;
		permanentlyDead = newPermanentlyDead;
	}

	public static int getAttempts() {
		return attempts;
	}

	public static boolean isPermanentlyDead() {
		return permanentlyDead;
	}
}
