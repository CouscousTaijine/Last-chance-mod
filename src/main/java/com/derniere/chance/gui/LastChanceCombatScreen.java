package com.derniere.chance.gui;

import com.derniere.chance.DerniereChanceMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * L'écran de combat "Dernière Chance".
 *
 * Le joueur déplace un petit cœur (façon Minecraft) à l'intérieur d'une
 * arène pour esquiver des vagues de projectiles générées par
 * {@link WavePatterns}. S'il survit à toutes les vagues, il réapparaît ;
 * s'il perd tous ses points de vie, c'est la mort définitive.
 */
public class LastChanceCombatScreen extends Screen {

	private enum Phase { INTRO, FIGHTING, WAVE_CLEAR, WON, LOST }

	private static final float HEART_RADIUS = 3.5f;
	private static final float HEART_SPEED = 1.6f;

	private final CombatDifficulty difficulty;
	private final WavePatterns patterns = new WavePatterns();
	private final List<Projectile> projectiles = new ArrayList<>();

	private Phase phase = Phase.INTRO;
	private int phaseTimer = 0;
	private int wave = 0;
	private int tickInWave = 0;
	private int waveDurationTicks;

	private float heartX = 0; // relatif au centre de l'arène
	private float heartY = 0;
	private int hp;

	private boolean resultSent = false;

	public LastChanceCombatScreen(CombatDifficulty difficulty) {
		super(Text.translatable("gui.derniere_chance.combat_title"));
		this.difficulty = difficulty;
		this.hp = difficulty.getMaxHeartHp();
		this.waveDurationTicks = 140; // ~7 secondes par vague à 20 tps
	}

	@Override
	protected void init() {
		super.init();
		this.phase = Phase.INTRO;
		this.phaseTimer = 30;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// On empêche de fuir le combat avec Échap : c'est "Dernière Chance", pas un menu classique.
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private float arenaHalfW() {
		return 90 * difficulty.getArenaShrink();
	}

	private float arenaHalfH() {
		return 60 * difficulty.getArenaShrink();
	}

	private float arenaCenterX() {
		return this.width / 2f;
	}

	private float arenaCenterY() {
		return this.height / 2f + 10;
	}

	@Override
	public void tick() {
		super.tick();

		switch (phase) {
			case INTRO -> tickIntro();
			case FIGHTING -> tickFighting();
			case WAVE_CLEAR -> tickWaveClear();
			case WON, LOST -> tickEnd();
		}
	}

	private void tickIntro() {
		if (--phaseTimer <= 0) {
			phase = Phase.FIGHTING;
			tickInWave = 0;
		}
	}

	private void tickWaveClear() {
		if (--phaseTimer <= 0) {
			wave++;
			if (wave >= difficulty.getWaveCount()) {
				phase = Phase.WON;
				phaseTimer = 50;
			} else {
				projectiles.clear();
				tickInWave = 0;
				phase = Phase.FIGHTING;
			}
		}
	}

	private void tickEnd() {
		if (phase == Phase.WON && !resultSent) {
			sendResult(true);
		} else if (phase == Phase.LOST && !resultSent) {
			sendResult(false);
		}

		if (phase == Phase.WON) {
			if (--phaseTimer <= 0) {
				this.close();
			}
		}
		// En cas de LOST, l'écran reste affiché : seul le bouton "Quitter" permet de sortir.
	}

	private void tickFighting() {
		moveHeart();

		float hw = arenaHalfW();
		float hh = arenaHalfH();

		List<Projectile> spawned = patterns.generate(wave, tickInWave, hw, hh, difficulty, heartX, heartY);
		projectiles.addAll(spawned);

		Iterator<Projectile> it = projectiles.iterator();
		while (it.hasNext()) {
			Projectile p = it.next();
			p.tick();

			if (p.x < -hw - 20 || p.x > hw + 20 || p.y < -hh - 20 || p.y > hh + 20) {
				it.remove();
				continue;
			}

			if (p.collidesWith(heartX, heartY, HEART_RADIUS)) {
				hp -= difficulty.getContactDamage();
				it.remove();

				if (hp <= 0) {
					hp = 0;
					phase = Phase.LOST;
					phaseTimer = Integer.MAX_VALUE;
					return;
				}
			}
		}

		tickInWave++;
		if (tickInWave >= waveDurationTicks) {
			phase = Phase.WAVE_CLEAR;
			phaseTimer = 25;
		}
	}

	private void moveHeart() {
		if (client == null) return;
		var options = client.options;

		float dx = 0, dy = 0;
		if (options.forwardKey.isPressed()) dy -= 1;
		if (options.backKey.isPressed()) dy += 1;
		if (options.leftKey.isPressed()) dx -= 1;
		if (options.rightKey.isPressed()) dx += 1;

		if (dx != 0 && dy != 0) {
			// Normalisation pour ne pas aller plus vite en diagonale.
			dx *= 0.7071f;
			dy *= 0.7071f;
		}

		heartX += dx * HEART_SPEED;
		heartY += dy * HEART_SPEED;

		float hw = arenaHalfW() - HEART_RADIUS;
		float hh = arenaHalfH() - HEART_RADIUS;
		heartX = Math.max(-hw, Math.min(hw, heartX));
		heartY = Math.max(-hh, Math.min(hh, heartY));
	}

	private void sendResult(boolean won) {
		resultSent = true;
		if (client != null && client.getNetworkHandler() != null) {
			PacketByteBuf buf = DerniereChanceMod.createResultBuffer(won);
			ClientPlayNetworking.send(DerniereChanceMod.COMBAT_RESULT_PACKET, buf);
		}

		if (!won) {
			this.clearChildren();
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.derniere_chance.quit"), b -> {
				if (client != null) {
					client.disconnect();
					client.setScreen(new TitleScreen());
				}
			}).dimensions(this.width / 2 - 75, this.height / 2 + 70, 150, 20).build());
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);

		float cx = arenaCenterX();
		float cy = arenaCenterY();
		float hw = arenaHalfW();
		float hh = arenaHalfH();

		// Cadre de l'arène, façon boîte de combat Undertale.
		context.fill((int) (cx - hw) - 2, (int) (cy - hh) - 2, (int) (cx + hw) + 2, (int) (cy - hh), 0xFFFFFFFF);
		context.fill((int) (cx - hw) - 2, (int) (cy + hh), (int) (cx + hw) + 2, (int) (cy + hh) + 2, 0xFFFFFFFF);
		context.fill((int) (cx - hw) - 2, (int) (cy - hh) - 2, (int) (cx - hw), (int) (cy + hh) + 2, 0xFFFFFFFF);
		context.fill((int) (cx + hw), (int) (cy - hh) - 2, (int) (cx + hw) + 2, (int) (cy + hh) + 2, 0xFFFFFFFF);
		context.fill((int) (cx - hw), (int) (cy - hh), (int) (cx + hw), (int) (cy + hh), 0xFF000000);

		if (phase == Phase.FIGHTING || phase == Phase.WAVE_CLEAR) {
			for (Projectile p : projectiles) {
				int px = (int) (cx + p.x);
				int py = (int) (cy + p.y);
				int r = Math.round(p.radius);
				context.fill(px - r, py - r, px + r, py + r, p.color);
			}

			drawHeart(context, (int) (cx + heartX), (int) (cy + heartY), 1);
		}

		drawCenteredText(context, headerText(), this.width / 2, (int) (cy - hh) - 26);
		drawCenteredText(context, hpText(), this.width / 2, (int) (cy + hh) + 18);

		if (phase == Phase.LOST) {
			drawCenteredText(context, Text.translatable("gui.derniere_chance.game_over").getString(), this.width / 2, (int) (cy - hh) - 46);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	private String headerText() {
		return switch (phase) {
			case INTRO -> Text.translatable("gui.derniere_chance.wave_incoming", wave + 1, difficulty.getWaveCount()).getString();
			case FIGHTING, WAVE_CLEAR -> Text.translatable("gui.derniere_chance.wave_progress", wave + 1, difficulty.getWaveCount()).getString();
			case WON -> Text.translatable("gui.derniere_chance.victory").getString();
			case LOST -> Text.translatable("gui.derniere_chance.defeat").getString();
		};
	}

	private String hpText() {
		return "❤ " + Math.max(0, hp) + " / " + difficulty.getMaxHeartHp();
	}

	private void drawCenteredText(DrawContext context, String text, int x, int y) {
		context.drawCenteredTextWithShadow(this.textRenderer, text, x, y, 0xFFFFFF);
	}

	/**
	 * Dessine un petit cœur "pixel art" façon Minecraft (au lieu du cœur Undertale)
	 * en utilisant uniquement des rectangles pleins, sans dépendre d'une texture externe.
	 */
	private void drawHeart(DrawContext context, int centerX, int centerY, int pixelSize) {
		int[][] shape = {
			{0, 1, 1, 0, 1, 1, 0},
			{1, 1, 1, 1, 1, 1, 1},
			{1, 1, 1, 1, 1, 1, 1},
			{0, 1, 1, 1, 1, 1, 0},
			{0, 0, 1, 1, 1, 0, 0},
			{0, 0, 0, 1, 0, 0, 0}
		};

		int color = hp <= difficulty.getMaxHeartHp() / 3 ? 0xFFFF5555 : 0xFFDD1F1F;

		int startX = centerX - (shape[0].length * pixelSize) / 2;
		int startY = centerY - (shape.length * pixelSize) / 2;

		for (int row = 0; row < shape.length; row++) {
			for (int col = 0; col < shape[row].length; col++) {
				if (shape[row][col] == 1) {
					int px = startX + col * pixelSize;
					int py = startY + row * pixelSize;
					context.fill(px, py, px + pixelSize, py + pixelSize, color);
				}
			}
		}
	}
}
