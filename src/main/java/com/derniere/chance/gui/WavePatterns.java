package com.derniere.chance.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Contient les 6 patterns d'attaque "de base" (déjà pensés comme le combat
 * le plus dur possible) ainsi que la logique qui les combine entre eux
 * pour les vagues au-delà de la 6e (une vague de plus = un pattern de plus
 * ajouté à celui déjà en cours, joué en simultané).
 *
 * Toutes les coordonnées sont relatives au centre de l'arène (0,0).
 */
public class WavePatterns {

	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int COLOR_YELLOW = 0xFFFFE066;
	private static final int COLOR_CYAN = 0xFF66E0FF;

	private final Random random = new Random();

	/**
	 * Génère les projectiles à spawn pour ce tick, pour la vague donnée.
	 *
	 * @param waveIndex   index de la vague (0-based)
	 * @param tickInWave  numéro du tick depuis le début de la vague
	 * @param arenaHalfW  demi-largeur de l'arène
	 * @param arenaHalfH  demi-hauteur de l'arène
	 * @param diff        difficulté courante (vitesse/densité)
	 * @param heartX      position X du cœur (relative au centre) au moment du spawn
	 * @param heartY      position Y du cœur (relative au centre) au moment du spawn
	 */
	public List<Projectile> generate(int waveIndex, int tickInWave, float arenaHalfW, float arenaHalfH,
			CombatDifficulty diff, float heartX, float heartY) {

		List<Projectile> spawned = new ArrayList<>();

		// Le pattern "principal" de cette vague : on boucle sur les 6 patterns de base.
		int basePattern = waveIndex % CombatDifficulty.BASE_HARDEST_PATTERNS;
		spawnPattern(basePattern, spawned, tickInWave, arenaHalfW, arenaHalfH, diff, heartX, heartY);

		// Au-delà de la 6e vague, on superpose un pattern supplémentaire par vague
		// en plus (une vague de plus = un pattern de plus, joué en même temps).
		int extraLayers = Math.max(0, (waveIndex / CombatDifficulty.BASE_HARDEST_PATTERNS));
		for (int layer = 1; layer <= extraLayers; layer++) {
			int extraPattern = (basePattern + layer * 2) % CombatDifficulty.BASE_HARDEST_PATTERNS;
			spawnPattern(extraPattern, spawned, tickInWave, arenaHalfW, arenaHalfH, diff, heartX, heartY);
		}

		return spawned;
	}

	private void spawnPattern(int pattern, List<Projectile> out, int t, float hw, float hh,
			CombatDifficulty diff, float heartX, float heartY) {
		switch (pattern) {
			case 0 -> rain(out, t, hw, hh, diff);
			case 1 -> walls(out, t, hw, hh, diff);
			case 2 -> ring(out, t, hw, hh, diff);
			case 3 -> cross(out, t, hw, hh, diff);
			case 4 -> spiral(out, t, hw, hh, diff);
			case 5 -> aimed(out, t, hw, hh, diff, heartX, heartY);
		}
	}

	/** Pattern 0 : pluie de projectiles depuis le haut. */
	private void rain(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff) {
		int interval = Math.max(2, Math.round(6 / diff.getDensityMultiplier()));
		if (t % interval != 0) return;
		float x = (random.nextFloat() * 2 - 1) * hw;
		float speed = 1.4f * diff.getSpeedMultiplier();
		out.add(new Projectile(x, -hh, 0, speed, 3.0f, COLOR_WHITE));
	}

	/** Pattern 1 : murs horizontaux avec une brèche, alternant du haut et du bas. */
	private void walls(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff) {
		int period = Math.max(20, Math.round(50 / diff.getSpeedMultiplier()));
		if (t % period != 0) return;

		boolean fromTop = (t / period) % 2 == 0;
		float gapCenter = (random.nextFloat() * 2 - 1) * hw * 0.6f;
		float gapWidth = Math.max(10f, 26f / diff.getDensityMultiplier());
		float step = Math.max(6f, 12f / diff.getDensityMultiplier());

		for (float x = -hw; x <= hw; x += step) {
			if (Math.abs(x - gapCenter) < gapWidth) continue;
			float speed = 1.1f * diff.getSpeedMultiplier();
			out.add(new Projectile(x, fromTop ? -hh : hh, 0, fromTop ? speed : -speed, 2.5f, COLOR_CYAN));
		}
	}

	/** Pattern 2 : anneau qui se referme depuis les bords vers le centre. */
	private void ring(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff) {
		int period = Math.max(30, Math.round(70 / diff.getSpeedMultiplier()));
		if (t % period != 0) return;

		int count = Math.round(10 * diff.getDensityMultiplier());
		float radius = Math.min(hw, hh);
		for (int i = 0; i < count; i++) {
			double angle = (2 * Math.PI / count) * i;
			float x = (float) (Math.cos(angle) * radius);
			float y = (float) (Math.sin(angle) * radius);
			float speed = 0.9f * diff.getSpeedMultiplier();
			out.add(new Projectile(x, y, (float) (-Math.cos(angle) * speed), (float) (-Math.sin(angle) * speed), 3.0f, COLOR_WHITE));
		}
	}

	/** Pattern 3 : croix qui convergent depuis les 4 côtés. */
	private void cross(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff) {
		int interval = Math.max(8, Math.round(18 / diff.getDensityMultiplier()));
		if (t % interval != 0) return;

		float speed = 1.3f * diff.getSpeedMultiplier();
		float offset = (random.nextFloat() * 2 - 1) * Math.min(hw, hh) * 0.5f;

		out.add(new Projectile(-hw, offset, speed, 0, 2.5f, COLOR_YELLOW));
		out.add(new Projectile(hw, offset, -speed, 0, 2.5f, COLOR_YELLOW));
		out.add(new Projectile(offset, -hh, 0, speed, 2.5f, COLOR_YELLOW));
		out.add(new Projectile(offset, hh, 0, -speed, 2.5f, COLOR_YELLOW));
	}

	/** Pattern 4 : spirale continue depuis le centre. */
	private double spiralAngle = 0;

	private void spiral(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff) {
		int interval = Math.max(1, Math.round(3 / diff.getDensityMultiplier()));
		if (t % interval != 0) return;

		spiralAngle += 0.5;
		float speed = 1.0f * diff.getSpeedMultiplier();
		float x = (float) (Math.cos(spiralAngle) * speed * 2);
		float y = (float) (Math.sin(spiralAngle) * speed * 2);
		out.add(new Projectile(0, 0, x, y, 2.5f, COLOR_CYAN));
	}

	/** Pattern 5 : projectiles tirés depuis un bord aléatoire, visant le cœur au moment du tir. */
	private void aimed(List<Projectile> out, int t, float hw, float hh, CombatDifficulty diff, float heartX, float heartY) {
		int interval = Math.max(10, Math.round(22 / diff.getDensityMultiplier()));
		if (t % interval != 0) return;

		int side = random.nextInt(4);
		float sx, sy;
		switch (side) {
			case 0 -> { sx = -hw; sy = (random.nextFloat() * 2 - 1) * hh; }
			case 1 -> { sx = hw; sy = (random.nextFloat() * 2 - 1) * hh; }
			case 2 -> { sx = (random.nextFloat() * 2 - 1) * hw; sy = -hh; }
			default -> { sx = (random.nextFloat() * 2 - 1) * hw; sy = hh; }
		}

		float dx = heartX - sx;
		float dy = heartY - sy;
		float len = (float) Math.max(0.01, Math.sqrt(dx * dx + dy * dy));
		float speed = 1.5f * diff.getSpeedMultiplier();
		out.add(new Projectile(sx, sy, (dx / len) * speed, (dy / len) * speed, 3.2f, COLOR_YELLOW));
	}
}
