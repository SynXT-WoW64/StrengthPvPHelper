package com.synxt.strengthpvp.hud;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.CombatFeatures;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.ui.UiDraw;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.synxt.strengthpvp.feature.CrystalOptimiser;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.PlayerListEntry;
import com.synxt.strengthpvp.feature.CooldownUtil;

import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

/** HUD elements with independent, persistent position/scale/palette. */
public final class HudRenderer {
    private static boolean armorWasLow;
    private static int armorAlertCooldown;
    private static long fpsWindowStart = System.nanoTime();
    private static int fpsFrames;
    private static int measuredFps;
    private static ClientWorld sessionWorld;
    private static long sessionStartMillis;

    private HudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(HudRenderer::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            armorWasLow = false;
            return;
        }
        if (armorAlertCooldown > 0) armorAlertCooldown--;
        if (!ModConfig.isEnabled(FeatureModule.ARMOR_HUD)) { armorWasLow = false; return; }
        boolean low = false;
        int threshold = ModConfig.getArmorWarningPercent();
        for (ItemStack stack : client.player.getInventory().armor) {
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
            int percent = Math.round(remaining * 100.0f / stack.getMaxDamage());
            if (percent <= threshold) low = true;
        }
        if (low && !armorWasLow && armorAlertCooldown == 0) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.7f);
            armorAlertCooldown = 40;
        }
        armorWasLow = low;
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        updateRuntimeStats(client);

        if (ModConfig.isEnabled(FeatureModule.ITEM_COUNTER)) drawWidget(client, matrices, FeatureModule.ITEM_COUNTER, 164, 122,
                () -> renderItemCounter(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.ARMOR_HUD)) drawWidget(client, matrices, FeatureModule.ARMOR_HUD, 178, 94,
                () -> renderArmor(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.STATUS_EFFECTS)) drawWidget(client, matrices, FeatureModule.STATUS_EFFECTS, 154, 140,
                () -> renderEffects(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.ATTACK_TIMING)) drawWidget(client, matrices, FeatureModule.ATTACK_TIMING, 140, 30,
                () -> renderAttackTiming(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.TARGET_INFO)) drawWidget(client, matrices, FeatureModule.TARGET_INFO, 210, 50,
                () -> renderTargetInfo(client, matrices, false));
        if (ModConfig.isEnabled(FeatureModule.ENEMY_ARMOR)) drawWidget(client, matrices, FeatureModule.ENEMY_ARMOR, 210, 56,
                () -> renderTargetInfo(client, matrices, true));
        if (ModConfig.isEnabled(FeatureModule.TOTEM_POP)) drawWidget(client, matrices, FeatureModule.TOTEM_POP, 170, 45,
                () -> renderTrackedPops(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.COORDINATES_HUD)) drawWidget(client, matrices, FeatureModule.COORDINATES_HUD, 176, 43,
                () -> renderCoordinates(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.DIRECTION_HUD)) drawWidget(client, matrices, FeatureModule.DIRECTION_HUD, 150, 34,
                () -> renderDirection(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.PERFORMANCE_HUD)) drawWidget(client, matrices, FeatureModule.PERFORMANCE_HUD, 166, 34,
                () -> renderPerformance(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.KEYSTROKES_HUD)) drawWidget(client, matrices, FeatureModule.KEYSTROKES_HUD, 88, 82,
                () -> renderKeystrokes(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.HELD_DURABILITY)) drawWidget(client, matrices, FeatureModule.HELD_DURABILITY, 176, 42,
                () -> renderHeldDurability(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.WORLD_CLOCK)) drawWidget(client, matrices, FeatureModule.WORLD_CLOCK, 138, 38,
                () -> renderWorldClock(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.SESSION_TIMER)) drawWidget(client, matrices, FeatureModule.SESSION_TIMER, 138, 38,
                () -> renderSessionTimer(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.HIT_RANGE)) drawWidget(client, matrices, FeatureModule.HIT_RANGE, 178, 42,
                () -> renderHitRangeHud(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.COMBO_COUNTER)) drawWidget(client, matrices, FeatureModule.COMBO_COUNTER, 138, 38,
                () -> renderComboCounter(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.TARGET_DIRECTION)) drawWidget(client, matrices, FeatureModule.TARGET_DIRECTION, 178, 38,
                () -> renderTargetDirection(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.FOOD_HUD)) drawWidget(client, matrices, FeatureModule.FOOD_HUD, 166, 40,
                () -> renderFoodHud(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.BIOME_HUD)) drawWidget(client, matrices, FeatureModule.BIOME_HUD, 178, 38,
                () -> renderBiomeHud(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.DEATH_MARKER) && CombatFeatures.hasDeathMarker()) drawWidget(client, matrices, FeatureModule.DEATH_MARKER, 178, 38,
                () -> renderDeathMarker(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.INVENTORY_SLOTS_HUD)) drawWidget(client, matrices, FeatureModule.INVENTORY_SLOTS_HUD, 156, 38,
                () -> renderInventorySlots(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.SHIELD_STATUS)) drawWidget(client, matrices, FeatureModule.SHIELD_STATUS, 166, 38,
                () -> renderShieldStatus(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.TOTEM_HAND_ALERT)) drawWidget(client, matrices, FeatureModule.TOTEM_HAND_ALERT, 190, 32,
                () -> renderTotemHandAlert(client, matrices));
        if (ModConfig.isEnabled(FeatureModule.CROSSHAIR_CUSTOM)) renderCustomCrosshair(client, matrices);

        // Cooldowns belong to their corresponding hotbar slots, not to a separate screen panel.
        if (ModConfig.isEnabled(FeatureModule.COOLDOWN_HUD)) renderHotbarCooldowns(client, matrices, tickDelta);
        renderAlerts(client, matrices);
    }

    private interface WidgetDraw { void draw(); }

    private static void drawWidget(MinecraftClient client, MatrixStack matrices, FeatureModule module,
                                   int baseWidth, int baseHeight, WidgetDraw draw) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float scale = ModConfig.getScale(module) / 100.0f;
        int actualW = Math.max(1, Math.round(baseWidth * scale));
        int actualH = Math.max(1, Math.round(baseHeight * scale));
        int x = Math.round(width * ModConfig.getHudX(module) / 1000.0f);
        int y = Math.round(height * ModConfig.getHudY(module) / 1000.0f);
        x = Math.max(0, Math.min(width - actualW, x));
        y = Math.max(0, Math.min(height - actualH, y));
        matrices.push();
        matrices.translate(x, y, 0.0);
        matrices.scale(scale, scale, 1.0f);
        draw.draw();
        matrices.pop();
    }

    private static void renderItemCounter(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.ITEM_COUNTER;
        List<CounterEntry> entries = new ArrayList<>();
        entries.add(new CounterEntry(Items.TOTEM_OF_UNDYING, countItem(client, Items.TOTEM_OF_UNDYING), text("Тотемы", "Totems")));
        entries.add(new CounterEntry(Items.GOLDEN_APPLE, countItem(client, Items.GOLDEN_APPLE) + countItem(client, Items.ENCHANTED_GOLDEN_APPLE), text("Золотые яблоки", "Golden apples")));
        entries.add(new CounterEntry(Items.END_CRYSTAL, countItem(client, Items.END_CRYSTAL), text("Кристаллы", "Crystals")));
        entries.add(new CounterEntry(Items.RESPAWN_ANCHOR, countItem(client, Items.RESPAWN_ANCHOR), text("Якоря", "Anchors")));
        entries.add(new CounterEntry(Items.ENDER_PEARL, countItem(client, Items.ENDER_PEARL), text("Перлы", "Pearls")));
        entries.add(new CounterEntry(Items.POTION, countHealingPotions(client), text("Хилки", "Healing")));

        int width = 164, height = entries.size() * 17 + 20;
        panel(client, matrices, m, 0, 0, width, height);
        UiDraw.drawText(client.textRenderer, matrices, text("РАСХОДНИКИ", "SUPPLIES"), 7, 4, ModConfig.getAccentColor(m));
        int y = 18;
        for (CounterEntry entry : entries) {
            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();
            float widgetScale = ModConfig.getScale(m) / 100.0f;
            int widgetX = Math.max(0, Math.min(screenW - Math.round(164 * widgetScale), Math.round(screenW * ModConfig.getHudX(m) / 1000.0f)));
            int widgetY = Math.max(0, Math.min(screenH - Math.round(122 * widgetScale), Math.round(screenH * ModConfig.getHudY(m) / 1000.0f)));
            renderScaledItem(client, new ItemStack(entry.icon), widgetX + Math.round(6 * widgetScale),
                    widgetY + Math.round((y - 2) * widgetScale), widgetScale);
            UiDraw.drawText(client.textRenderer, matrices, entry.label, 27, y + 1, ModConfig.getTextColor(m));
            String count = String.valueOf(entry.count);
            UiDraw.drawText(client.textRenderer, matrices, count, width - 9 - client.textRenderer.getWidth(count), y + 1,
                    entry.count == 0 ? 0xFF818C9D : ModConfig.getTextColor(m));
            y += 17;
        }
    }

    private static void renderArmor(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.ARMOR_HUD;
        List<ItemStack> armor = client.player.getInventory().armor;
        int count = 0;
        for (ItemStack stack : armor) if (!stack.isEmpty() && stack.isDamageable()) count++;
        if (count == 0) return;
        int width = 178, height = count * 17 + 20;
        panel(client, matrices, m, 0, 0, width, height);
        UiDraw.drawText(client.textRenderer, matrices, text("БРОНЯ", "ARMOR"), 7, 4, ModConfig.getAccentColor(m));
        String[] namesRu = {"Ботинки", "Поножи", "Нагрудник", "Шлем"};
        String[] namesEn = {"Boots", "Leggings", "Chestplate", "Helmet"};
        int y = 18, slot = 0;
        for (ItemStack stack : armor) {
            if (stack.isEmpty() || !stack.isDamageable()) { slot++; continue; }
            int max = stack.getMaxDamage();
            int remaining = Math.max(0, max - stack.getDamage());
            int percent = Math.round(remaining * 100.0f / max);
            int threshold = ModConfig.getArmorWarningPercent();
            int color = percent <= threshold ? 0xFFFF5C62 : (percent <= 50 ? 0xFFFFC45E : 0xFF76DDA5);
            String name = ModConfig.isEnglish() ? namesEn[slot] : namesRu[slot];
            String value = percent + "%  " + remaining + "/" + max;
            int valueX = width - 7 - client.textRenderer.getWidth(value);
            if (percent <= threshold && (client.world.getTime() / 5L % 2L == 0L)) {
                UiDraw.drawText(client.textRenderer, matrices, "!", 4, y + 1, 0xFFFF5C62);
            }
            UiDraw.drawText(client.textRenderer, matrices, name, 12, y + 1, ModConfig.getTextColor(m));
            if (12 + client.textRenderer.getWidth(name) + 5 > valueX) {
                UiDraw.drawText(client.textRenderer, matrices, "…", Math.max(12, valueX - 10), y + 1, ModConfig.getTextColor(m));
            }
            UiDraw.drawText(client.textRenderer, matrices, value, valueX, y + 1, color);
            y += 17;
            slot++;
        }
    }

    private static void renderEffects(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.STATUS_EFFECTS;
        List<StatusEffectInstance> effects = new ArrayList<>();
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (effect.getEffectType() == StatusEffects.STRENGTH || effect.getEffectType() == StatusEffects.SPEED
                    || effect.getEffectType() == StatusEffects.FIRE_RESISTANCE || effect.getEffectType() == StatusEffects.RESISTANCE
                    || effect.getEffectType() == StatusEffects.ABSORPTION || effect.getEffectType() == StatusEffects.REGENERATION) effects.add(effect);
        }
        if (effects.isEmpty()) return;
        int width = 154;
        panel(client, matrices, m, 0, 0, width, effects.size() * 17 + 20);
        UiDraw.drawText(client.textRenderer, matrices, text("ЭФФЕКТЫ", "EFFECTS"), 7, 4, ModConfig.getAccentColor(m));
        int y = 18;
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType() == StatusEffects.STRENGTH ? text("Сила", "Strength")
                    : effect.getEffectType() == StatusEffects.SPEED ? text("Скорость", "Speed")
                    : effect.getEffectType() == StatusEffects.FIRE_RESISTANCE ? text("Огнестойкость", "Fire Res.")
                    : effect.getEffectType() == StatusEffects.RESISTANCE ? text("Сопротивление", "Resistance")
                    : effect.getEffectType() == StatusEffects.ABSORPTION ? text("Поглощение", "Absorption")
                    : text("Регенерация", "Regeneration");
            String amplifier = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String time = formatDuration(effect.getDuration());
            UiDraw.drawText(client.textRenderer, matrices, name + amplifier, 7, y + 1, ModConfig.getTextColor(m));
            UiDraw.drawText(client.textRenderer, matrices, time, width - 7 - client.textRenderer.getWidth(time), y + 1, ModConfig.getAccentColor(m));
            y += 17;
        }
    }

    private static void renderAttackTiming(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.ATTACK_TIMING;
        float charge = client.player.getAttackCooldownProgress(0.0f);
        int width = 140;
        panel(client, matrices, m, 0, 0, width, 30);
        UiDraw.drawText(client.textRenderer, matrices, text("УДАР", "HIT"), 7, 4, ModConfig.getTextColor(m));
        String percent = Math.round(charge * 100.0f) + "%";
        UiDraw.drawText(client.textRenderer, matrices, percent, width - 7 - client.textRenderer.getWidth(percent), 4,
                charge >= 0.9f ? 0xFF76DDA5 : ModConfig.getAccentColor(m));
        UiDraw.roundedRect(matrices, 7, 18, width - 14, 5, 0xFF3B4656, 3);
        UiDraw.roundedRect(matrices, 7, 18, Math.round((width - 14) * charge), 5, ModConfig.getAccentColor(m), 3);
    }

    private static void renderTargetInfo(MinecraftClient client, MatrixStack matrices, boolean armor) {
        if (!(client.crosshairTarget instanceof EntityHitResult)) return;
        Entity targetEntity = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!(targetEntity instanceof LivingEntity) || targetEntity == client.player) return;
        LivingEntity target = (LivingEntity) targetEntity;
        if (!target.isAlive()) return;
        FeatureModule m = armor ? FeatureModule.ENEMY_ARMOR : FeatureModule.TARGET_INFO;
        int width = 210;
        int height = armor ? 56 : 38;
        panel(client, matrices, m, 0, 0, width, height);
        String name = target.getEntityName();
        double distance = Math.sqrt(client.player.squaredDistanceTo(target));
        String info = trim(name, 92) + "  " + String.format("%.1f", target.getHealth()) + " HP  " + String.format("%.1f", distance) + "m";
        UiDraw.drawText(client.textRenderer, matrices, info, 7, 4, ModConfig.getTextColor(m));
        if (target instanceof PlayerEntity && ModConfig.isEnabled(FeatureModule.TOTEM_POP)) {
            String pops = text("Тотемы: ", "Pops: ") + CombatFeatures.getPopCount((PlayerEntity) target);
            UiDraw.drawText(client.textRenderer, matrices, pops, 7, 17, ModConfig.getAccentColor(m));
        }
        if (armor && target instanceof PlayerEntity) {
            int itemX = 7;
            for (ItemStack stack : target.getArmorItems()) {
                if (stack.isEmpty()) { itemX += 30; continue; }
                client.getItemRenderer().renderInGui(stack, itemX, 20);
                int prot = EnchantmentHelper.getLevel(Enchantments.PROTECTION, stack);
                int thorns = EnchantmentHelper.getLevel(Enchantments.THORNS, stack);
                String ench = prot > 0 ? "P" + prot : "";
                if (thorns > 0) ench += (ench.isEmpty() ? "" : " ") + "Th" + thorns;
                if (!ench.isEmpty()) UiDraw.drawText(client.textRenderer, matrices, ench, itemX + 2, 38, ModConfig.getAccentColor(m));
                itemX += 30;
            }
        }
    }

    private static void renderTrackedPops(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.TOTEM_POP;
        int total = 0;
        PlayerEntity target = null;
        if (client.crosshairTarget instanceof EntityHitResult) {
            Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (entity instanceof PlayerEntity && entity != client.player) target = (PlayerEntity) entity;
        }
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != client.player) total += CombatFeatures.getPopCount((PlayerEntity) entity);
        }
        if (total == 0) return;
        int w = 170;
        panel(client, matrices, m, 0, 0, w, 38);
        String all = text("Сбитые тотемы: ", "Totem pops: ") + total;
        UiDraw.drawText(client.textRenderer, matrices, all, 7, 4, ModConfig.getTextColor(m));
        if (target != null) {
            String line = target.getEntityName() + ": " + CombatFeatures.getPopCount(target);
            UiDraw.drawText(client.textRenderer, matrices, trim(line, 154), 7, 18, ModConfig.getAccentColor(m));
        }
    }

    private static void renderHotbarCooldowns(MinecraftClient client, MatrixStack matrices, float tickDelta) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int startX = screenWidth / 2 - 90;
        int y = screenHeight - 19;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().main.get(slot);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!client.player.getItemCooldownManager().isCoolingDown(item)) continue;
            float progress = client.player.getItemCooldownManager().getCooldownProgress(item, tickDelta);
            if (progress <= 0.001f) continue;
            int remainingTicks = CooldownUtil.getRemainingTicks(client.player.getItemCooldownManager(), item, tickDelta);
            if (remainingTicks <= 0) continue;
            int x = startX + slot * 20 + 1;
            int overlayHeight = Math.max(1, Math.min(16, Math.round(16 * progress)));
            DrawableHelper.fill(matrices, x, y + 16 - overlayHeight, x + 16, y + 16, 0x99000000);
            String label = remainingTicks >= 20
                    ? String.format(Locale.ROOT, "%.0fs", remainingTicks / 20.0)
                    : String.format(Locale.ROOT, "%.1fs", remainingTicks / 20.0);
            matrices.push();
            matrices.translate(x + 2.0, y + 5.0, 0.0);
            matrices.scale(0.58f, 0.58f, 1.0f);
            UiDraw.drawText(client.textRenderer, matrices, label, 0, 0,
                    ModConfig.getAccentColor(FeatureModule.COOLDOWN_HUD));
            matrices.pop();
        }
    }

    private static void renderCoordinates(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.COORDINATES_HUD;
        panel(client, matrices, module, 0, 0, 176, 43);
        UiDraw.drawText(client.textRenderer, matrices, text("КООРДИНАТЫ", "COORDINATES"), 7, 4,
                ModConfig.getAccentColor(module));
        String coords = String.format(Locale.ROOT, "X %.1f   Y %.1f   Z %.1f",
                client.player.getX(), client.player.getY(), client.player.getZ());
        UiDraw.drawText(client.textRenderer, matrices, coords, 7, 18, ModConfig.getTextColor(module));
        String dimension = client.world.getRegistryKey().getValue().getPath();
        if ("overworld".equals(dimension)) dimension = text("Обычный мир", "Overworld");
        else if ("the_nether".equals(dimension)) dimension = text("Незер", "The Nether");
        else if ("the_end".equals(dimension)) dimension = text("Энд", "The End");
        UiDraw.drawText(client.textRenderer, matrices, trim(dimension, 160), 7, 31,
                ModConfig.getSecondaryColor(module));
    }

    private static void renderDirection(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.DIRECTION_HUD;
        panel(client, matrices, module, 0, 0, 150, 34);
        float yaw = MathHelper.wrapDegrees(client.player.getYaw());
        int sector = Math.floorMod(Math.round(yaw / 45.0f), 8);
        String[] directions = ModConfig.isEnglish()
                ? new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"}
                : new String[]{"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
        String label = text("НАПРАВЛЕНИЕ", "DIRECTION") + "  " + directions[sector];
        UiDraw.drawText(client.textRenderer, matrices, label, 7, 4, ModConfig.getAccentColor(module));
        String angle = String.format(Locale.ROOT, "Yaw  %.1f°", yaw);
        UiDraw.drawText(client.textRenderer, matrices, angle, 7, 18, ModConfig.getTextColor(module));
    }

    private static void renderPerformance(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.PERFORMANCE_HUD;
        panel(client, matrices, module, 0, 0, 166, 34);
        UiDraw.drawText(client.textRenderer, matrices, text("ПРОИЗВОДИТЕЛЬНОСТЬ", "PERFORMANCE"), 7, 4,
                ModConfig.getAccentColor(module));
        int ping = -1;
        if (client.getNetworkHandler() != null && client.player != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        String fpsLabel = "FPS " + measuredFps;
        String pingLabel = ping < 0 ? "PING --" : "PING " + ping + "ms";
        UiDraw.drawText(client.textRenderer, matrices, fpsLabel, 7, 18, ModConfig.getTextColor(module));
        int pingX = 166 - 7 - client.textRenderer.getWidth(pingLabel);
        UiDraw.drawText(client.textRenderer, matrices, pingLabel, pingX, 18, ModConfig.getSecondaryColor(module));
    }

    private static void renderKeystrokes(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.KEYSTROKES_HUD;
        panel(client, matrices, module, 0, 0, 88, 82);
        int left = 9;
        int top = 5;
        drawKeyTile(client, matrices, module, left + 22, top, 18, 15, "W", client.options.keyForward.isPressed());
        drawKeyTile(client, matrices, module, left, top + 17, 18, 15, "A", client.options.keyLeft.isPressed());
        drawKeyTile(client, matrices, module, left + 22, top + 17, 18, 15, "S", client.options.keyBack.isPressed());
        drawKeyTile(client, matrices, module, left + 44, top + 17, 18, 15, "D", client.options.keyRight.isPressed());
        drawKeyTile(client, matrices, module, left, top + 35, 62, 12, "SPACE", client.options.keyJump.isPressed());
        drawKeyTile(client, matrices, module, left, top + 50, 30, 12, "SHIFT", client.options.keySneak.isPressed());
        drawKeyTile(client, matrices, module, left + 32, top + 50, 30, 12, "CTRL", client.options.keySprint.isPressed());
    }

    private static void drawKeyTile(MinecraftClient client, MatrixStack matrices, FeatureModule module,
                                    int x, int y, int width, int height, String label, boolean pressed) {
        int background = pressed ? ModConfig.getAccentColor(module) : 0xFF2B3748;
        UiDraw.roundedRect(matrices, x, y, width, height, UiDraw.withAlpha(background, pressed ? 235 : 220), 3);
        int labelWidth = client.textRenderer.getWidth(label);
        UiDraw.drawText(client.textRenderer, matrices, label, x + (width - labelWidth) / 2,
                y + (height - 8) / 2, pressed ? 0xFFFFFFFF : ModConfig.getTextColor(module));
    }

    private static void renderHeldDurability(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.HELD_DURABILITY;
        int width = 176;
        panel(client, matrices, module, 0, 0, width, 42);
        UiDraw.drawText(client.textRenderer, matrices, text("ПРОЧНОСТЬ В РУКЕ", "HELD ITEM DURABILITY"), 7, 4,
                ModConfig.getAccentColor(module));
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            UiDraw.drawText(client.textRenderer, matrices, text("Предмет не изнашивается", "Item has no durability"), 7, 20,
                    ModConfig.getTextColor(module));
            return;
        }
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
        int percent = Math.round(remaining * 100.0f / stack.getMaxDamage());
        int threshold = ModConfig.getOption(module);
        int color = percent <= threshold ? 0xFFFF626B : (percent <= 50 ? 0xFFFFC45E : 0xFF76DDA5);
        String itemName = stack.getTranslationKey().replace("item.minecraft.", "").replace("item.", "")
                .replace('_', ' ');
        itemName = trim(itemName, 105);
        UiDraw.drawText(client.textRenderer, matrices, itemName, 7, 20, ModConfig.getTextColor(module));
        String status = percent + "%";
        UiDraw.drawText(client.textRenderer, matrices, status, width - 7 - client.textRenderer.getWidth(status), 20, color);
    }

    private static void renderWorldClock(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.WORLD_CLOCK;
        panel(client, matrices, module, 0, 0, 138, 38);
        long dayTime = client.world.getTimeOfDay();
        long clockTicks = Math.floorMod(dayTime + 6000L, 24000L);
        int hours = (int) (clockTicks / 1000L);
        int minutes = (int) ((clockTicks % 1000L) * 60L / 1000L);
        long day = Math.floorDiv(dayTime, 24000L) + 1L;
        UiDraw.drawText(client.textRenderer, matrices, text("ВРЕМЯ МИРА", "WORLD TIME"), 7, 4,
                ModConfig.getAccentColor(module));
        String time = String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
        UiDraw.drawText(client.textRenderer, matrices, time, 7, 18, ModConfig.getTextColor(module));
        UiDraw.drawText(client.textRenderer, matrices, text("День ", "Day ") + day, 83, 20,
                ModConfig.getSecondaryColor(module));
    }

    private static void renderSessionTimer(MinecraftClient client, MatrixStack matrices) {
        FeatureModule module = FeatureModule.SESSION_TIMER;
        panel(client, matrices, module, 0, 0, 138, 38);
        long elapsed = Math.max(0L, (System.currentTimeMillis() - sessionStartMillis) / 1000L);
        long hours = elapsed / 3600L;
        long minutes = (elapsed / 60L) % 60L;
        long seconds = elapsed % 60L;
        String time = hours > 0
                ? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        UiDraw.drawText(client.textRenderer, matrices, text("ВРЕМЯ СЕССИИ", "SESSION TIME"), 7, 4,
                ModConfig.getAccentColor(module));
        UiDraw.drawText(client.textRenderer, matrices, time, 7, 18, ModConfig.getTextColor(module));
    }

    private static void updateRuntimeStats(MinecraftClient client) {
        long now = System.nanoTime();
        fpsFrames++;
        long elapsed = now - fpsWindowStart;
        if (elapsed >= 1_000_000_000L) {
            measuredFps = Math.round(fpsFrames * 1_000_000_000.0f / elapsed);
            fpsFrames = 0;
            fpsWindowStart = now;
        }
        if (sessionWorld != client.world) {
            sessionWorld = client.world;
            sessionStartMillis = System.currentTimeMillis();
        }
    }

    private static void renderScaledItem(MinecraftClient client, ItemStack stack, int x, int y, float scale) {
        MatrixStack modelView = RenderSystem.getModelViewStack();
        modelView.push();
        modelView.translate(x, y, 0.0);
        modelView.scale(scale, scale, 1.0f);
        RenderSystem.applyModelViewMatrix();
        client.getItemRenderer().renderInGui(stack, 0, 0);
        client.getItemRenderer().renderGuiItemOverlay(client.textRenderer, stack, 0, 0);
        modelView.pop();
        RenderSystem.applyModelViewMatrix();
    }

    private static void renderHitRangeHud(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.HIT_RANGE;
        panel(client, matrices, m, 0, 0, 178, 42);
        UiDraw.drawText(client.textRenderer, matrices, "HIT RANGE", 7, 4, ModConfig.getAccentColor(m));
        String range = String.format(Locale.ROOT, "Reach: %.1f m", ModConfig.getOption(m) / 10.0f);
        UiDraw.drawText(client.textRenderer, matrices, range, 7, 19, ModConfig.getTextColor(m));
        if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
            Entity target = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
            if (target instanceof PlayerEntity && target != client.player) {
                String distance = String.format(Locale.ROOT, "Target: %.2f m", Math.sqrt(client.player.squaredDistanceTo(target)));
                UiDraw.drawText(client.textRenderer, matrices, distance, 7, 30, ModConfig.getSecondaryColor(m));
            }
        }
    }

    private static void renderComboCounter(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.COMBO_COUNTER;
        panel(client, matrices, m, 0, 0, 138, 38);
        UiDraw.drawText(client.textRenderer, matrices, "COMBO", 7, 4, ModConfig.getAccentColor(m));
        String count = String.valueOf(CombatFeatures.getComboCount());
        UiDraw.drawText(client.textRenderer, matrices, count + " hits", 7, 18, ModConfig.getTextColor(m));
        if (CombatFeatures.getComboTicks() > 0) UiDraw.drawText(client.textRenderer, matrices,
                String.format(Locale.ROOT, "%.1fs", CombatFeatures.getComboTicks() / 20.0f), 90, 18, ModConfig.getSecondaryColor(m));
    }

    private static void renderTargetDirection(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.TARGET_DIRECTION;
        panel(client, matrices, m, 0, 0, 178, 38);
        UiDraw.drawText(client.textRenderer, matrices, text("НАПРАВЛЕНИЕ ЦЕЛИ", "TARGET BEARING"), 7, 4, ModConfig.getAccentColor(m));
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult)) {
            UiDraw.drawText(client.textRenderer, matrices, text("Нет цели", "No target"), 7, 19, ModConfig.getTextColor(m)); return;
        }
        Entity target = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
        if (!(target instanceof PlayerEntity) || target == client.player) {
            UiDraw.drawText(client.textRenderer, matrices, text("Нет игрока под прицелом", "No player targeted"), 7, 19, ModConfig.getTextColor(m)); return;
        }
        double dx = target.getX() - client.player.getX();
        double dz = target.getZ() - client.player.getZ();
        float bearing = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        String direction = compass(bearing);
        String value = direction + "  " + Math.round(bearing) + "°  " + String.format(Locale.ROOT, "%.1fm", Math.sqrt(dx*dx+dz*dz));
        UiDraw.drawText(client.textRenderer, matrices, value, 7, 19, ModConfig.getTextColor(m));
    }

    private static String compass(float yaw) {
        String[] names = ModConfig.isEnglish() ? new String[]{"S","SW","W","NW","N","NE","E","SE"}
                : new String[]{"Ю","ЮЗ","З","СЗ","С","СВ","В","ЮВ"};
        int index = Math.floorMod(Math.round(yaw / 45.0f) + 4, 8);
        return names[index];
    }

    private static void renderFoodHud(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.FOOD_HUD;
        panel(client, matrices, m, 0, 0, 166, 40);
        int food = client.player.getHungerManager().getFoodLevel();
        float saturation = client.player.getHungerManager().getSaturationLevel();
        UiDraw.drawText(client.textRenderer, matrices, text("ЕДА И НАСЫЩЕНИЕ", "FOOD & SATURATION"), 7, 4, ModConfig.getAccentColor(m));
        UiDraw.roundedRect(matrices, 7, 20, 150, 5, 0xFF303B4A, 2);
        UiDraw.roundedRect(matrices, 7, 20, Math.round(150 * food / 20.0f), 5, ModConfig.getAccentColor(m), 2);
        UiDraw.roundedRect(matrices, 7, 29, 150, 4, 0xFF303B4A, 2);
        UiDraw.roundedRect(matrices, 7, 29, Math.round(150 * Math.min(20.0f, saturation) / 20.0f), 4, ModConfig.getTertiaryColor(m), 2);
        UiDraw.drawText(client.textRenderer, matrices, food + "/20", 118, 9, ModConfig.getTextColor(m));
    }

    private static void renderBiomeHud(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.BIOME_HUD;
        panel(client, matrices, m, 0, 0, 178, 38);
        String biome = client.world.getBiomeAccess().getBiome(client.player.getBlockPos()).getCategory().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        UiDraw.drawText(client.textRenderer, matrices, text("БИОМ", "BIOME"), 7, 4, ModConfig.getAccentColor(m));
        UiDraw.drawText(client.textRenderer, matrices, biome, 7, 19, ModConfig.getTextColor(m));
    }

    private static void renderDeathMarker(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.DEATH_MARKER;
        panel(client, matrices, m, 0, 0, 178, 38);
        UiDraw.drawText(client.textRenderer, matrices, text("ПОСЛЕДНЯЯ СМЕРТЬ", "LAST DEATH"), 7, 4, ModConfig.getAccentColor(m));
        String coords = String.format(Locale.ROOT, "X %.0f  Y %.0f  Z %.0f", CombatFeatures.getDeathX(), CombatFeatures.getDeathY(), CombatFeatures.getDeathZ());
        UiDraw.drawText(client.textRenderer, matrices, coords, 7, 19, ModConfig.getTextColor(m));
    }

    private static void renderInventorySlots(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.INVENTORY_SLOTS_HUD;
        int free = 0;
        for (int i = 0; i < 36; i++) if (client.player.getInventory().getStack(i).isEmpty()) free++;
        panel(client, matrices, m, 0, 0, 156, 38);
        UiDraw.drawText(client.textRenderer, matrices, text("ИНВЕНТАРЬ", "INVENTORY"), 7, 4, ModConfig.getAccentColor(m));
        UiDraw.drawText(client.textRenderer, matrices, text("Свободно слотов: ", "Free slots: ") + free + "/36", 7, 19,
                free <= 3 ? ModConfig.getSecondaryColor(m) : ModConfig.getTextColor(m));
    }

    private static void renderShieldStatus(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.SHIELD_STATUS;
        ItemStack shield = client.player.getOffHandStack().getItem() == Items.SHIELD ? client.player.getOffHandStack()
                : client.player.getMainHandStack().getItem() == Items.SHIELD ? client.player.getMainHandStack() : ItemStack.EMPTY;
        panel(client, matrices, m, 0, 0, 166, 38);
        UiDraw.drawText(client.textRenderer, matrices, text("ЩИТ", "SHIELD"), 7, 4, ModConfig.getAccentColor(m));
        if (shield.isEmpty()) {
            UiDraw.drawText(client.textRenderer, matrices, text("Не экипирован", "Not equipped"), 7, 19, ModConfig.getTextColor(m));
        } else {
            int remaining = shield.getMaxDamage() - shield.getDamage();
            int percent = Math.round(remaining * 100.0f / Math.max(1, shield.getMaxDamage()));
            UiDraw.drawText(client.textRenderer, matrices, percent + "%  " + (client.player.isBlocking() ? text("БЛОК", "BLOCKING") : text("Готов", "Ready")),
                    7, 19, percent <= 20 ? ModConfig.getSecondaryColor(m) : ModConfig.getTextColor(m));
        }
    }

    private static void renderTotemHandAlert(MinecraftClient client, MatrixStack matrices) {
        FeatureModule m = FeatureModule.TOTEM_HAND_ALERT;
        if (client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        boolean hasSpare = countItem(client, Items.TOTEM_OF_UNDYING) > 0;
        panel(client, matrices, m, 0, 0, 190, 32);
        String message = hasSpare ? text("ТОТЕМ НЕ ВО ВТОРОЙ РУКЕ", "TOTEM NOT IN OFFHAND")
                : text("ТОТЕМОВ НЕТ В ИНВЕНТАРЕ", "NO TOTEMS IN INVENTORY");
        UiDraw.drawText(client.textRenderer, matrices, message, 7, 10,
                hasSpare ? ModConfig.getAccentColor(m) : ModConfig.getSecondaryColor(m));
    }

    private static void renderCustomCrosshair(MinecraftClient client, MatrixStack matrices) {
        if (client.options.hudHidden) return;
        int size = ModConfig.getCrosshairSize();
        int pixel = Math.max(1, Math.round(2.0f * ModConfig.getScale(FeatureModule.CROSSHAIR_CUSTOM) / 100.0f));
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int left = cx - (size * pixel) / 2;
        int top = cy - (size * pixel) / 2;
        String grid = ModConfig.getCrosshairPixels();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int index = y * size + x;
                if (index >= grid.length()) continue;
                int paletteIndex = grid.charAt(index) - '0';
                if (paletteIndex <= 0) continue;
                int color = ModConfig.getCrosshairPaletteColor(paletteIndex);
                DrawableHelper.fill(matrices, left + x * pixel, top + y * pixel,
                        left + (x + 1) * pixel, top + (y + 1) * pixel, color);
            }
        }
    }

    private static void renderAlerts(MinecraftClient client, MatrixStack matrices) {
        if (ModConfig.isEnabled(FeatureModule.LOW_HEALTH_ALERT)
                && client.player.getHealth() <= ModConfig.getOption(FeatureModule.LOW_HEALTH_ALERT)) {
            drawAlertWidget(client, matrices, FeatureModule.LOW_HEALTH_ALERT,
                    text("Мало здоровья: ", "Low health: ") + String.format("%.1f HP", client.player.getHealth()));
        }
        if (ModConfig.isEnabled(FeatureModule.TOTEM_REFILL_ALERT)
                && countItem(client, Items.TOTEM_OF_UNDYING) <= ModConfig.getOption(FeatureModule.TOTEM_REFILL_ALERT)) {
            drawAlertWidget(client, matrices, FeatureModule.TOTEM_REFILL_ALERT,
                    text("Осталось тотемов: ", "Totems left: ") + countItem(client, Items.TOTEM_OF_UNDYING));
        }
        if (ModConfig.isEnabled(FeatureModule.QUICK_HEAL_REMINDER)
                && client.player.getHealth() <= ModConfig.getOption(FeatureModule.QUICK_HEAL_REMINDER)
                && (countHealingPotions(client) > 0 || countItem(client, Items.GOLDEN_APPLE) + countItem(client, Items.ENCHANTED_GOLDEN_APPLE) > 0)) {
            drawAlertWidget(client, matrices, FeatureModule.QUICK_HEAL_REMINDER,
                    text("Есть средство лечения", "Healing item available"));
        }
        if (ModConfig.isEnabled(FeatureModule.PEARL_READY_ALERT) && CombatFeatures.isPearlReadyDisplayActive()) {
            drawAlertWidget(client, matrices, FeatureModule.PEARL_READY_ALERT,
                    text("Эндер-жемчуг готов", "Ender pearl ready"));
        }
        if (ModConfig.isEnabled(FeatureModule.ARMOR_WARNINGS)) {
            int threshold = ModConfig.getOption(FeatureModule.ARMOR_WARNINGS);
            for (ItemStack armor : client.player.getInventory().armor) {
                if (!armor.isEmpty() && armor.isDamageable()) {
                    int remaining = armor.getMaxDamage() - armor.getDamage();
                    int percent = Math.round(remaining * 100.0f / Math.max(1, armor.getMaxDamage()));
                    if (percent <= threshold) {
                        drawAlertWidget(client, matrices, FeatureModule.ARMOR_WARNINGS,
                                text("Броня почти сломана: ", "Armor nearly broken: ") + percent + "%");
                        break;
                    }
                }
            }
        }
        if (ModConfig.isEnabled(FeatureModule.DANGER_INDICATOR)
                && client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult
                && !net.minecraft.block.RespawnAnchorBlock.isNether(client.world)) {
            net.minecraft.util.math.BlockPos pos = ((net.minecraft.util.hit.BlockHitResult) client.crosshairTarget).getBlockPos();
            net.minecraft.block.BlockState state = client.world.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.block.RespawnAnchorBlock
                    && state.get(net.minecraft.block.RespawnAnchorBlock.CHARGES) > 0) {
                drawAlertWidget(client, matrices, FeatureModule.DANGER_INDICATOR,
                        text("Заряженный якорь — опасно!", "Charged anchor — dangerous!"));
            }
        }
        if (ModConfig.isEnabled(FeatureModule.MISCLICK_PREVENTION)) {
            if (CombatFeatures.isUnsafeUseTarget(client)) drawAlertWidget(client, matrices, FeatureModule.MISCLICK_PREVENTION,
                    text("Использование заблокировано", "Use blocked"));
        }
    }

    private static void drawAlertWidget(MinecraftClient client, MatrixStack matrices, FeatureModule module, String message) {
        drawWidget(client, matrices, module, 220, 26, () -> {
            int width = 220;
            panel(client, matrices, module, 0, 0, width, 24);
            String clipped = trim(message, width - 16);
            UiDraw.drawText(client.textRenderer, matrices, clipped, 8, 8, ModConfig.getAccentColor(module));
        });
    }

    private static int countItem(MinecraftClient client, Item item) { return CombatFeatures.countItem(client, item); }
    private static int countHealingPotions(MinecraftClient client) {
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) if (isHealingPotion(stack)) total += stack.getCount();
        for (ItemStack stack : client.player.getInventory().offHand) if (isHealingPotion(stack)) total += stack.getCount();
        return total;
    }
    private static boolean isHealingPotion(ItemStack stack) {
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) return false;
        return PotionUtil.getPotion(stack) == Potions.HEALING || PotionUtil.getPotion(stack) == Potions.STRONG_HEALING;
    }
    private static String formatDuration(int ticks) {
        if (ticks < 0) return "∞";
        int seconds = Math.max(0, ticks / 20);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
    private static void panel(MinecraftClient client, MatrixStack matrices, FeatureModule module, int x, int y, int width, int height) {
        int alpha = Math.round(255.0f * ModConfig.getModuleOpacity(module) / 100.0f);
        int bg = ModConfig.getBackgroundColor(module);
        // Respect the user opacity slider while preserving each module's selected RGB background.
        int color = (Math.min(255, alpha) << 24) | (bg & 0x00FFFFFF);
        UiDraw.roundedRect(matrices, x, y, width, height, color, 5);
        UiDraw.roundedRect(matrices, x + 4, y, Math.max(1, width - 8), 1,
                UiDraw.withAlpha(ModConfig.getAccentColor(module), Math.min(180, alpha)), 1);
    }
    private static String text(String ru, String en) { return ModConfig.isEnglish() ? en : ru; }
    private static String trim(String value, int max) {
        String result = value;
        while (!result.isEmpty() && MinecraftClient.getInstance().textRenderer.getWidth(result) > max) result = result.substring(0, result.length() - 1);
        return result;
    }
    private static final class CounterEntry {
        final Item icon; final int count; final String label;
        CounterEntry(Item icon, int count, String label) { this.icon = icon; this.count = count; this.label = label; }
    }
}
