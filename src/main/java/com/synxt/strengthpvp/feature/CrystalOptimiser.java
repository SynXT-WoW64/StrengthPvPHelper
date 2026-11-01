package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Always-on, non-toggleable cache for crystal/player lookups used by overlays.
 * This avoids scanning the complete entity list on every rendered frame.
 */
public final class CrystalOptimiser {
    private static World cachedWorld;
    private static long nextRefresh;
    private static List<EndCrystalEntity> crystals = Collections.emptyList();
    private static List<PlayerEntity> players = Collections.emptyList();
    private static List<ProjectileEntity> projectiles = Collections.emptyList();

    private CrystalOptimiser() { }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            cachedWorld = null; nextRefresh = 0L;
            crystals = Collections.emptyList(); players = Collections.emptyList(); projectiles = Collections.emptyList();
            return;
        }
        boolean needed = ModConfig.isEnabled(FeatureModule.CUSTOM_HITBOXES)
                || ModConfig.isEnabled(FeatureModule.DANGER_INDICATOR)
                || ModConfig.isEnabled(FeatureModule.PROJECTILE_PREDICTOR);
        if (!needed) {
            cachedWorld = null;
            crystals = Collections.emptyList(); players = Collections.emptyList(); projectiles = Collections.emptyList();
            return;
        }
        long worldTime = client.world.getTime();
        if (cachedWorld == client.world && worldTime < nextRefresh) return;
        List<EndCrystalEntity> newCrystals = new ArrayList<>();
        List<PlayerEntity> newPlayers = new ArrayList<>();
        List<ProjectileEntity> newProjectiles = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) newCrystals.add((EndCrystalEntity) entity);
            else if (entity instanceof PlayerEntity && entity.isAlive()) newPlayers.add((PlayerEntity) entity);
            else if (entity instanceof ProjectileEntity && entity.isAlive()) newProjectiles.add((ProjectileEntity) entity);
        }
        crystals = Collections.unmodifiableList(newCrystals);
        players = Collections.unmodifiableList(newPlayers);
        projectiles = Collections.unmodifiableList(newProjectiles);
        cachedWorld = client.world;
        // Extra interval in the local safe profile; no Sodium/Lithium settings are touched.
        nextRefresh = worldTime + (ModConfig.isSuperOptimization() ? 6L : 2L);
    }

    public static List<EndCrystalEntity> getCrystals(World world) {
        return cachedWorld == world ? crystals : Collections.<EndCrystalEntity>emptyList();
    }
    public static List<PlayerEntity> getPlayers(World world) {
        return cachedWorld == world ? players : Collections.<PlayerEntity>emptyList();
    }
    public static List<ProjectileEntity> getProjectiles(World world) {
        return cachedWorld == world ? projectiles : Collections.<ProjectileEntity>emptyList();
    }
}
