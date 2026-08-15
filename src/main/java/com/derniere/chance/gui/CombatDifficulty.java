package com.derniere.chance.gui;

/**
 * Calcule les paramètres de difficulté du combat en fonction du nombre de
 * fois où le joueur a déjà utilisé "Dernière Chance".
 *
 * Comme demandé : la première utilisation démarre déjà sur le pattern le
 * plus complexe qu'on puisse concevoir (BASE_HARDEST_PATTERNS vagues), et
 * chaque utilisation supplémentaire ajoute encore une vague, avec vitesse
 * et densité de projectiles qui augmentent très rapidement (courbe
 * volontairement punitive : mieux vaut garder cette option pour un vrai
 * cas de dernier recours).
 */
public class CombatDifficulty {

	/** Les 6 patterns "de base", déjà pensés pour être le combat le plus dur possible. */
	public static final int BASE_HARDEST_PATTERNS = 6;

	private final int attemptIndex; // 0 = première utilisation
	private final int waveCount;
	private final float speedMultiplier;
	private final float densityMultiplier;
	private final float arenaShrink;
	private final int contactDamage;

	public CombatDifficulty(int attemptIndex) {
		this.attemptIndex = Math.max(0, attemptIndex);

		// Une vague de plus à chaque utilisation, en plus des 6 vagues de base.
		this.waveCount = BASE_HARDEST_PATTERNS + this.attemptIndex;

		// Montée en puissance volontairement rapide et non-linéaire.
		this.speedMultiplier = 1.0f + (this.attemptIndex * 0.4f) + (this.attemptIndex * this.attemptIndex * 0.02f);
		this.densityMultiplier = 1.0f + (this.attemptIndex * 0.35f);

		// L'arène rétrécit progressivement (jusqu'à un minimum) pour laisser
		// de moins en moins de place pour esquiver.
		this.arenaShrink = Math.max(0.45f, 1.0f - this.attemptIndex * 0.05f);

		// Les dégâts par contact augmentent aussi, jusqu'à un plafond.
		this.contactDamage = Math.min(5, 1 + this.attemptIndex / 3);
	}

	public int getWaveCount() {
		return waveCount;
	}

	public float getSpeedMultiplier() {
		return speedMultiplier;
	}

	public float getDensityMultiplier() {
		return densityMultiplier;
	}

	public float getArenaShrink() {
		return arenaShrink;
	}

	public int getContactDamage() {
		return contactDamage;
	}

	public int getAttemptIndex() {
		return attemptIndex;
	}

	/** Nombre de cœurs (points de vie) du joueur pendant le combat. Reste volontairement bas. */
	public int getMaxHeartHp() {
		return 10;
	}
}
