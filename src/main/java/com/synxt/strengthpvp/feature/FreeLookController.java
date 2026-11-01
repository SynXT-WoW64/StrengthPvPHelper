package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Client-only camera rotation. The player's yaw and pitch are left untouched,
 * so the server continues to receive the player's real movement direction.
 */
public final class FreeLookController {
    private static boolean active;
    private static PlayerEntity owner;
    private static float cameraYaw;
    private static float cameraPitch;

    private FreeLookController() { }

    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        boolean requested = ModConfig.isEnabled(FeatureModule.FREE_LOOK)
                && player != null && client.world != null && client.currentScreen == null && !player.isSleeping();

        if (!requested) {
            active = false;
            owner = null;
            return;
        }

        if (!active || owner != player) {
            owner = player;
            cameraYaw = player.getYaw();
            cameraPitch = player.getPitch();
            active = true;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    /** Receives mouse deltas after vanilla sensitivity and invert-Y processing. */
    public static void changeLookDirection(double deltaX, double deltaY) {
        if (!active) return;
        cameraPitch = MathHelper.clamp(cameraPitch + (float) (deltaY * 0.15), -90.0f, 90.0f);
        cameraYaw = MathHelper.wrapDegrees(cameraYaw + (float) (deltaX * 0.15));
    }
}
