package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.feature.FreeLookController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D"))
    private void strengthpvp$applyFreeLookBeforeThirdPersonMove(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!thirdPerson || !FreeLookController.isActive()
                || focusedEntity != MinecraftClient.getInstance().player) return;
        float yaw = FreeLookController.getCameraYaw();
        float pitch = FreeLookController.getCameraPitch();
        if (inverseView) { yaw += 180.0f; pitch = -pitch; }
        setRotation(yaw, pitch);
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void strengthpvp$applyFreeLookInFirstPerson(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson || !FreeLookController.isActive()
                || focusedEntity != MinecraftClient.getInstance().player) return;
        setRotation(FreeLookController.getCameraYaw(), FreeLookController.getCameraPitch());
    }
}
