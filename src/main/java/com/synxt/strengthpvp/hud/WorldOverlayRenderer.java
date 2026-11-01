package com.synxt.strengthpvp.hud;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.feature.CrystalOptimiser;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.text.Style;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorldOverlayRenderer {
    private static Object cachedWorld;
    private static BlockPos cachedCenter;
    private static int cachedRadius = -1;
    private static long nextBlockScan;
    private static List<BlockPos> cachedCrystalBases = Collections.emptyList();
    private static List<BlockPos> cachedAnchors = Collections.emptyList();

    private WorldOverlayRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || context.world() == null) return;
        boolean hitboxes = ModConfig.isEnabled(FeatureModule.CUSTOM_HITBOXES);
        boolean danger = ModConfig.isEnabled(FeatureModule.DANGER_INDICATOR);
        boolean projectiles = ModConfig.isEnabled(FeatureModule.PROJECTILE_PREDICTOR);
        boolean targetInfo = ModConfig.isEnabled(FeatureModule.ENEMY_ARMOR)
                || ModConfig.isEnabled(FeatureModule.TOTEM_POP);
        boolean hitRange = ModConfig.isEnabled(FeatureModule.HIT_RANGE);
        if (targetInfo) renderTargetNameTag(context, client);
        if (!hitboxes && !danger && !projectiles && !hitRange) return;

        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        Vec3d camera = context.camera().getPos();
        if (hitboxes) renderPlayerHitboxes(context, lines, camera, client.player);
        if (hitRange) renderHitRange(context, lines, camera, client.player, client);
        if (danger) {
            renderCrystalDanger(context, lines, camera, client.player);
            renderPotentialCrystalSpots(context, lines, camera, client.player);
            renderNearbyAnchors(context, lines, camera, client.player);
        }
        if (projectiles) {
            renderHeldTrajectory(context, lines, camera, client.player);
            renderNearbyProjectiles(context, lines, camera, client.player);
        }
    }

    private static void renderTargetNameTag(WorldRenderContext context, MinecraftClient client) {
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult)) return;
        Entity entity = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
        if (!(entity instanceof PlayerEntity) || entity == client.player || !entity.isAlive()) return;
        PlayerEntity target = (PlayerEntity) entity;
        Identifier font = new Identifier("strengthpvp", "synxt_ui");
        LiteralText label = new LiteralText(target.getEntityName());
        label.setStyle(Style.EMPTY.withFont(font).withColor(0xFFFFFFFF));
        if (ModConfig.isEnabled(FeatureModule.TOTEM_POP)) {
            label.append(new LiteralText("  Pops " + com.synxt.strengthpvp.feature.CombatFeatures.getPopCount(target))
                    .setStyle(Style.EMPTY.withFont(font).withColor(0xFFFFD45C)));
        }
        if (ModConfig.isEnabled(FeatureModule.ENEMY_ARMOR)) {
            int prot = 0, thorns = 0, pieces = 0;
            for (ItemStack stack : target.getArmorItems()) {
                if (stack.isEmpty()) continue;
                pieces++;
                prot += net.minecraft.enchantment.EnchantmentHelper.getLevel(net.minecraft.enchantment.Enchantments.PROTECTION, stack);
                thorns += net.minecraft.enchantment.EnchantmentHelper.getLevel(net.minecraft.enchantment.Enchantments.THORNS, stack);
            }
            String armor = "  Armor " + pieces + "/4" + (prot > 0 ? " P" + prot : "") + (thorns > 0 ? " Th" + thorns : "");
            label.append(new LiteralText(armor).setStyle(Style.EMPTY.withFont(font).withColor(0xFF71D5FF)));
        }
        Vec3d camera = context.camera().getPos();
        net.minecraft.client.util.math.MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(target.getX() - camera.x, target.getY() + target.getHeight() + 0.35 - camera.y, target.getZ() - camera.z);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        net.minecraft.util.math.Matrix4f matrix = matrices.peek().getModel();
        int textWidth = client.textRenderer.getWidth(label);
        client.textRenderer.draw(label, -textWidth / 2.0f, 0.0f, 0xFFFFFFFF, true, matrix,
                context.consumers(), true, 0x55000000, client.getEntityRenderDispatcher().getLight(target, context.tickDelta()));
        matrices.pop();
    }

    private static void renderPlayerHitboxes(WorldRenderContext context, VertexConsumer lines, Vec3d camera, PlayerEntity self) {
        double maxDistance = ModConfig.getOption(FeatureModule.CUSTOM_HITBOXES);
        double maxSq = maxDistance * maxDistance;
        for (PlayerEntity entity : CrystalOptimiser.getPlayers(context.world())) {
            if (entity == self || !entity.isAlive()) continue;
            if (self.squaredDistanceTo(entity) > maxSq) continue;
            int color = ModConfig.getAccentColor(FeatureModule.CUSTOM_HITBOXES);
            PlayerEntity target = (PlayerEntity) entity;
            if (target.hurtTime > 0) {
                color = ModConfig.getSecondaryColor(FeatureModule.CUSTOM_HITBOXES);
            } else if (self.squaredDistanceTo(target) <= 9.0) {
                color = ModConfig.getTertiaryColor(FeatureModule.CUSTOM_HITBOXES);
            }
            float red = ((color >> 16) & 255) / 255.0f;
            float green = ((color >> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;
            drawBox(context, lines, camera, target.getBoundingBox().expand(0.025), red, green, blue, 0.95f);
        }
    }

    private static void renderHitRange(WorldRenderContext context, VertexConsumer lines, Vec3d camera,
                                       PlayerEntity self, MinecraftClient client) {
        double range = ModConfig.getOption(FeatureModule.HIT_RANGE) / 10.0;
        int own = ModConfig.getAccentColor(FeatureModule.HIT_RANGE);
        drawGroundCircle(context, lines, camera, self.getX(), self.getBoundingBox().minY + 0.025, self.getZ(), range,
                ((own >> 16) & 255) / 255.0f, ((own >> 8) & 255) / 255.0f, (own & 255) / 255.0f, 0.95f);
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult)) return;
        Entity targetEntity = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
        if (!(targetEntity instanceof PlayerEntity) || targetEntity == self || !targetEntity.isAlive()) return;
        PlayerEntity target = (PlayerEntity) targetEntity;
        int enemy = ModConfig.getSecondaryColor(FeatureModule.HIT_RANGE);
        drawGroundCircle(context, lines, camera, target.getX(), target.getBoundingBox().minY + 0.025, target.getZ(), range,
                ((enemy >> 16) & 255) / 255.0f, ((enemy >> 8) & 255) / 255.0f, (enemy & 255) / 255.0f, 0.95f);
        Vec3d a = new Vec3d(self.getX(), self.getBoundingBox().minY + 0.05, self.getZ());
        Vec3d b = new Vec3d(target.getX(), target.getBoundingBox().minY + 0.05, target.getZ());
        drawWorldLine(context, lines, camera, a, b, 1.0f, 0.78f, 0.26f, 0.9f);
    }

    private static void drawGroundCircle(WorldRenderContext context, VertexConsumer lines, Vec3d camera,
                                         double cx, double y, double cz, double radius,
                                         float red, float green, float blue, float alpha) {
        int segments = 48;
        double step = Math.PI * 2.0 / segments;
        Vec3d previous = new Vec3d(cx + radius, y, cz);
        for (int i = 1; i <= segments; i++) {
            double angle = step * i;
            Vec3d next = new Vec3d(cx + Math.cos(angle) * radius, y, cz + Math.sin(angle) * radius);
            drawWorldLine(context, lines, camera, previous, next, red, green, blue, alpha);
            previous = next;
        }
    }

    private static void drawWorldLine(WorldRenderContext context, VertexConsumer lines, Vec3d camera,
                                      Vec3d from, Vec3d to, float red, float green, float blue, float alpha) {
        net.minecraft.util.math.Matrix4f matrix = context.matrixStack().peek().getModel();
        lines.vertex(matrix, (float) (from.x - camera.x), (float) (from.y - camera.y), (float) (from.z - camera.z))
                .color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
        lines.vertex(matrix, (float) (to.x - camera.x), (float) (to.y - camera.y), (float) (to.z - camera.z))
                .color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f).next();
    }

    private static void renderCrystalDanger(WorldRenderContext context, VertexConsumer lines, Vec3d camera, PlayerEntity self) {
        double radius = ModConfig.getOption(FeatureModule.DANGER_INDICATOR);
        for (EndCrystalEntity entity : CrystalOptimiser.getCrystals(context.world())) {
            double distance = Math.sqrt(self.squaredDistanceTo(entity));
            if (distance > radius) continue;
            // Conservative visual estimate only; actual server damage depends on terrain and server rules.
            double estimatedDamage = Math.max(0.0, 42.0 * (1.0 - distance / 12.0));
            estimatedDamage *= Math.max(0.28, 1.0 - self.getArmor() / 35.0);
            boolean lethalEstimate = estimatedDamage >= self.getHealth();
            int color = lethalEstimate ? ModConfig.getSecondaryColor(FeatureModule.DANGER_INDICATOR) : ModConfig.getAccentColor(FeatureModule.DANGER_INDICATOR);
            float r = ((color >> 16) & 255) / 255.0f;
            float g = ((color >> 8) & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            drawBox(context, lines, camera, entity.getBoundingBox().expand(0.15), r, g, b, 1.0f);
        }
    }


    /** Expensive block scans are refreshed every four world ticks, not every rendered frame. */
    private static void refreshDangerBlockCache(WorldRenderContext context, PlayerEntity self, int radius) {
        long worldTime = context.world().getTime();
        BlockPos center = self.getBlockPos().toImmutable();
        if (cachedWorld == context.world() && center.equals(cachedCenter) && cachedRadius == radius
                && worldTime < nextBlockScan) return;

        List<BlockPos> crystals = new ArrayList<>();
        List<BlockPos> anchors = new ArrayList<>();
        int verticalRadius = 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (self.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                            > (double) radius * radius + 4.0) continue;
                    net.minecraft.block.BlockState state = context.world().getBlockState(pos);
                    if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) {
                        if (context.world().isAir(pos.up()) && context.world().isAir(pos.up(2))) {
                            crystals.add(pos.toImmutable());
                        }
                    }
                    if (state.getBlock() instanceof RespawnAnchorBlock
                            && state.get(RespawnAnchorBlock.CHARGES) > 0) {
                        anchors.add(pos.toImmutable());
                    }
                }
            }
        }
        cachedWorld = context.world();
        cachedCenter = center;
        cachedRadius = radius;
        cachedCrystalBases = crystals;
        cachedAnchors = anchors;
        nextBlockScan = worldTime + (ModConfig.isSuperOptimization() ? 9L : 4L);
    }

    private static void renderPotentialCrystalSpots(WorldRenderContext context, VertexConsumer lines,
                                                     Vec3d camera, PlayerEntity self) {
        int radius = Math.min(8, ModConfig.getOption(FeatureModule.DANGER_INDICATOR));
        refreshDangerBlockCache(context, self, radius);
        for (BlockPos base : cachedCrystalBases) {
            Vec3d crystalPos = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
            double estimate = estimateExplosionDamage(self, crystalPos);
            boolean lethal = estimate >= self.getHealth();
            int markerColor = lethal ? ModConfig.getSecondaryColor(FeatureModule.DANGER_INDICATOR)
                    : ModConfig.getAccentColor(FeatureModule.DANGER_INDICATOR);
            float r = ((markerColor >> 16) & 255) / 255.0f;
            float g = ((markerColor >> 8) & 255) / 255.0f;
            float b = (markerColor & 255) / 255.0f;
            Box marker = new Box(base.getX() + 0.18, base.getY() + 1.0, base.getZ() + 0.18,
                    base.getX() + 0.82, base.getY() + 2.3, base.getZ() + 0.82);
            drawBox(context, lines, camera, marker, r, g, b, lethal ? 1.0f : 0.65f);
        }
    }

    private static void renderNearbyAnchors(WorldRenderContext context, VertexConsumer lines,
                                             Vec3d camera, PlayerEntity self) {
        if (RespawnAnchorBlock.isNether(context.world())) return;
        int radius = Math.min(8, ModConfig.getOption(FeatureModule.DANGER_INDICATOR));
        refreshDangerBlockCache(context, self, radius);
        for (BlockPos pos : cachedAnchors) {
            Vec3d anchorCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double estimate = estimateExplosionDamage(self, anchorCenter);
            boolean lethal = estimate >= self.getHealth();
            int color = lethal ? ModConfig.getSecondaryColor(FeatureModule.DANGER_INDICATOR)
                    : ModConfig.getAccentColor(FeatureModule.DANGER_INDICATOR);
            float red = ((color >> 16) & 255) / 255.0f;
            float green = ((color >> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;
            drawBox(context, lines, camera, new Box(pos).expand(0.06), red, green, blue, 1.0f);
        }
    }

    private static void renderHeldTrajectory(WorldRenderContext context, VertexConsumer lines, Vec3d camera, PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        boolean pearl = stack.getItem() == Items.ENDER_PEARL;
        boolean potion = stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
        boolean bow = stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW;
        if (!pearl && !potion && !bow) return;

        Vec3d look = player.getRotationVec(context.tickDelta()).normalize();
        Vec3d start = player.getPos().add(0.0, player.getHeight() * 0.88, 0.0).add(look.multiply(0.35));
        double speed = pearl ? 1.5 : (potion ? 0.5 : 3.0);
        Vec3d velocity = look.multiply(speed).add(player.getVelocity().multiply(0.5));
        double gravity = pearl ? 0.03 : (potion ? 0.05 : 0.05);
        int color = ModConfig.getAccentColor(FeatureModule.PROJECTILE_PREDICTOR);
        drawTrajectory(context, lines, camera, start, velocity, gravity,
                ((color >> 16) & 255) / 255.0f, ((color >> 8) & 255) / 255.0f, (color & 255) / 255.0f, 32);
    }

    private static void renderNearbyProjectiles(WorldRenderContext context, VertexConsumer lines, Vec3d camera, PlayerEntity self) {
        double maxDistance = ModConfig.getOption(FeatureModule.PROJECTILE_PREDICTOR);
        double maxSq = maxDistance * maxDistance;
        for (ProjectileEntity entity : CrystalOptimiser.getProjectiles(context.world())) {
            if (!(entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof PotionEntity)) continue;
            if (entity.getOwner() == self || self.squaredDistanceTo(entity) > maxSq) continue;
            Vec3d velocity = entity.getVelocity();
            double gravity = entity instanceof ArrowEntity ? 0.05 : (entity instanceof PotionEntity ? 0.05 : 0.03);
            int color = ModConfig.getSecondaryColor(FeatureModule.PROJECTILE_PREDICTOR);
            drawTrajectory(context, lines, camera, entity.getPos(), velocity, gravity,
                    ((color >> 16) & 255) / 255.0f, ((color >> 8) & 255) / 255.0f,
                    (color & 255) / 255.0f, 18);
        }
    }


    private static double estimateExplosionDamage(PlayerEntity player, Vec3d explosionPos) {
        double distance = Math.sqrt(player.getCameraPosVec(1.0f).squaredDistanceTo(explosionPos));
        double raw = Math.max(0.0, 42.0 * (1.0 - distance / 12.0));
        // Armor-only attenuation is deliberately conservative; enchantments, resistance,
        // block exposure and server-specific damage rules are not fully simulated here.
        return raw * Math.max(0.28, 1.0 - player.getArmor() / 35.0);
    }

    private static void drawTrajectory(WorldRenderContext context, VertexConsumer lines, Vec3d camera,
                                       Vec3d start, Vec3d initialVelocity, double gravity,
                                       float red, float green, float blue, int steps) {
        Vec3d pos = start;
        Vec3d velocity = initialVelocity;
        for (int i = 0; i < steps; i++) {
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.99).add(0.0, -gravity, 0.0);
            if (i % 2 == 0) {
                double size = i == steps - 1 ? 0.075 : 0.025;
                Box marker = new Box(pos.x - size, pos.y - size, pos.z - size,
                        pos.x + size, pos.y + size, pos.z + size);
                drawBox(context, lines, camera, marker, red, green, blue, 0.95f);
            }
            if (pos.y < context.world().getBottomY()) break;
        }
    }

    private static void drawBox(WorldRenderContext context, VertexConsumer lines, Vec3d camera, Box worldBox,
                                float red, float green, float blue, float alpha) {
        Box relative = worldBox.offset(-camera.x, -camera.y, -camera.z);
        WorldRenderer.drawBox(context.matrixStack(), lines, relative, red, green, blue, alpha);
    }
}
