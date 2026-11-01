package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.feature.CombatFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/** Counts the actual vanilla totem-pop entity status packet (status 35). */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class TotemPopPacketMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void strengthpvp$countTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Entity entity = packet.getEntity(client.world);
        if (entity instanceof PlayerEntity && entity != client.player) {
            CombatFeatures.recordTotemPop((PlayerEntity) entity, client);
        }
    }
}
