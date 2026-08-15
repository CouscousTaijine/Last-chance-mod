package com.derniere.chance.gui;

/** Un projectile simple à esquiver dans l'arène de combat. */
public class Projectile {

	public float x;
	public float y;
	public final float vx;
	public final float vy;
	public final float radius;
	public final int color;
	public boolean dead = false;

	public Projectile(float x, float y, float vx, float vy, float radius, int color) {
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
		this.radius = radius;
		this.color = color;
	}

	/** Avance le projectile d'un tick. */
	public void tick() {
		x += vx;
		y += vy;
	}

	public boolean collidesWith(float hx, float hy, float hr) {
		float dx = x - hx;
		float dy = y - hy;
		float dist2 = dx * dx + dy * dy;
		float r = radius + hr;
		return dist2 <= r * r;
	}
}
