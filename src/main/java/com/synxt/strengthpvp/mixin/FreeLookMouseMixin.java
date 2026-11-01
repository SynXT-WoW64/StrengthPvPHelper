package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.feature.FreeLookController;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public abstract class FreeLookMouseMixin {
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void strengthpvp$routeMouseToCamera(ClientPlayerEntity player, double deltaX, double deltaY) {
        if (FreeLookController.isActive()) {
            FreeLookController.changeLookDirection(deltaX, deltaY);
        } else {
            player.changeLookDirection(deltaX, deltaY);
        }
    }
}
