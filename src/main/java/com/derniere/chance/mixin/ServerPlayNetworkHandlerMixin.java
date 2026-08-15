package com.derniere.chance.mixin;

import com.derniere.chance.LastChanceState;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Empêche un joueur marqué "définitivement mort" (après avoir perdu le
 * combat "Dernière Chance") de réapparaître, y compris en quittant et en
 * rejoignant la partie.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

	@Shadow public ServerPlayerEntity player;

	@Inject(method = "onClientStatus", at = @At("HEAD"), cancellable = true)
	private void derniereChance$blockRespawn(ClientStatusC2SPacket packet, CallbackInfo ci) {
		if (packet.getMode() != ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) {
			return;
		}

		LastChanceState state = LastChanceState.get(player.getServerWorld());
		if (state.isPermanentlyDead(player.getUuid())) {
			ci.cancel();
			player.networkHandler.disconnect(Text.translatable("gui.derniere_chance.permanently_dead"));
		}
	}
}
