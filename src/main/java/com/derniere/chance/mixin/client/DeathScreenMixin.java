package com.derniere.chance.mixin.client;

import com.derniere.chance.ClientCombatData;
import com.derniere.chance.gui.CombatDifficulty;
import com.derniere.chance.gui.LastChanceCombatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

	protected DeathScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "initWidgets", at = @At("TAIL"))
	private void derniereChance$addButton(CallbackInfo ci) {
		// Si le joueur est déjà définitivement mort, on ne propose plus l'option :
		// la "dernière chance" a déjà été utilisée et perdue.
		if (ClientCombatData.isPermanentlyDead()) {
			return;
		}

		int buttonWidth = 150;
		int x = this.width / 2 - buttonWidth / 2;
		int y = this.height - 42;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.derniere_chance.button"), button -> {
			CombatDifficulty difficulty = new CombatDifficulty(ClientCombatData.getAttempts());
			MinecraftClient.getInstance().setScreen(new LastChanceCombatScreen(difficulty));
		}).dimensions(x, y, buttonWidth, 20).build());
	}
}
