package com.synxt.strengthpvp.hud;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    private static boolean armorWasLow = false;
    private static int armorAlertCooldown = 0;

    private HudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            armorWasLow = false;
            return;
        }
        if (armorAlertCooldown > 0) armorAlertCooldown--;
        if (!ModConfig.isEnabled(FeatureModule.ARMOR_HUD)) {
            armorWasLow = false;
            return;
        }

        boolean low = false;
        int threshold = ModConfig.getArmorWarningPercent();
        for (ItemStack stack : client.player.getInventory().armor) {
            if (!stack.isEmpty() && stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                int percent = Math.round(remaining * 100.0f / stack.getMaxDamage());
                if (percent <= threshold) low = true;
            }
        }
        if (low && !armorWasLow && armorAlertCooldown == 0) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.75f, 0.75f);
            armorAlertCooldown = 30;
        }
        armorWasLow = low;
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int x = 8;
        int y = 8;
        if (ModConfig.isEnabled(FeatureModule.ITEM_COUNTER)) {
            y = renderItemCounter(client, matrices, x, y);
        }
        if (ModConfig.isEnabled(FeatureModule.ARMOR_HUD)) {
            y = renderArmor(client, matrices, x, y + (y == 8 ? 0 : 5));
        }
        if (ModConfig.isEnabled(FeatureModule.STATUS_EFFECTS)) {
            renderEffects(client, matrices);
        }
    }

    private static int renderItemCounter(MinecraftClient client, MatrixStack matrices, int x, int y) {
        List<CounterEntry> entries = new ArrayList<>();
        entries.add(new CounterEntry(Items.TOTEM_OF_UNDYING, countItem(client, Items.TOTEM_OF_UNDYING), "Тотемы"));
        entries.add(new CounterEntry(Items.GOLDEN_APPLE, countItem(client, Items.GOLDEN_APPLE) + countItem(client, Items.ENCHANTED_GOLDEN_APPLE), "Золотые яблоки"));
        entries.add(new CounterEntry(Items.END_CRYSTAL, countItem(client, Items.END_CRYSTAL), "Кристаллы"));
        entries.add(new CounterEntry(Items.RESPAWN_ANCHOR, countItem(client, Items.RESPAWN_ANCHOR), "Якоря"));
        entries.add(new CounterEntry(Items.ENDER_PEARL, countItem(client, Items.ENDER_PEARL), "Перлы"));
        entries.add(new CounterEntry(Items.POTION, countHealingPotions(client), "Хилки"));

        int width = 132;
        int height = entries.size() * 18 + 6;
        client.inGameHud.fill(matrices, x - 3, y - 3, x + width, y + height, 0x990D111A);
        client.textRenderer.draw(matrices, "РАСХОДНИКИ", x + 3, y, 0xFFFF5555);
        y += 13;
        for (CounterEntry entry : entries) {
            client.getItemRenderer().renderInGui(new ItemStack(entry.icon), x + 2, y - 1);
            client.textRenderer.draw(matrices, entry.label, x + 22, y + 2, 0xFFE2E8F0);
            String count = String.valueOf(entry.count);
            client.textRenderer.draw(matrices, count, x + width - client.textRenderer.getWidth(count) - 5, y + 2,
                    entry.count == 0 ? 0xFF777F8C : 0xFFFFFFFF);
            y += 18;
        }
        return y;
    }

    private static int renderArmor(MinecraftClient client, MatrixStack matrices, int x, int y) {
        int rows = 0;
        for (ItemStack stack : client.player.getInventory().armor) {
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            rows++;
        }
        if (rows == 0) return y;
        client.inGameHud.fill(matrices, x - 3, y - 3, x + 155, y + rows * 18 + 3, 0x990D111A);
        client.textRenderer.draw(matrices, "БРОНЯ", x + 3, y, 0xFFFF5555);
        y += 13;
        int threshold = ModConfig.getArmorWarningPercent();
        int armorSlot = 0;
        for (ItemStack stack : client.player.getInventory().armor) {
            if (stack.isEmpty() || !stack.isDamageable()) {
                armorSlot++;
                continue;
            }
            int max = stack.getMaxDamage();
            int remaining = max - stack.getDamage();
            int percent = Math.round(remaining * 100.0f / max);
            int color = percent <= threshold ? 0xFFFF5555 : (percent <= 50 ? 0xFFFFC857 : 0xFF7CDE8B);
            String name = armorSlot == 0 ? "Ботинки" : armorSlot == 1 ? "Поножи" : armorSlot == 2 ? "Нагрудник" : "Шлем";
            String value = percent + "%  " + remaining + "/" + max;
            if (percent <= threshold && (client.world.getTime() / 5L % 2L == 0L)) {
                client.textRenderer.draw(matrices, "!", x + 3, y + 2, 0xFFFF5555);
            }
            client.textRenderer.draw(matrices, name, x + 12, y + 2, 0xFFE2E8F0);
            client.textRenderer.draw(matrices, value, x + 60, y + 2, color);
            y += 18;
            armorSlot++;
        }
        return y;
    }

    private static void renderEffects(MinecraftClient client, MatrixStack matrices) {
        int x = client.getWindow().getScaledWidth() - 154;
        int y = 8;
        List<StatusEffectInstance> effects = new ArrayList<>();
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            String id = effect.getEffectType().getTranslationKey();
            if (id.contains("strength") || id.contains("speed") || id.contains("fire_resistance")) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) return;
        client.inGameHud.fill(matrices, x - 5, y - 4, x + 150, y + effects.size() * 19 + 2, 0x990D111A);
        client.textRenderer.draw(matrices, "ЭФФЕКТЫ", x, y, 0xFFFF5555);
        y += 13;
        for (StatusEffectInstance effect : effects) {
            String id = effect.getEffectType().getTranslationKey();
            String name = id.contains("strength") ? "Сила" : (id.contains("speed") ? "Скорость" : "Огнестойкость");
            String amplifier = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String time = formatDuration(effect.getDuration());
            client.textRenderer.draw(matrices, name + amplifier, x, y + 1, 0xFFE2E8F0);
            client.textRenderer.draw(matrices, time, x + 108, y + 1, 0xFF7CDE8B);
            y += 19;
        }
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        return ticks < 0 ? "∞" : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static int countItem(MinecraftClient client, Item item) {
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) {
            if (stack.getItem() == item) total += stack.getCount();
        }
        for (ItemStack stack : client.player.getInventory().offHand) {
            if (stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private static int countHealingPotions(MinecraftClient client) {
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) {
            if (isHealingPotion(stack)) total += stack.getCount();
        }
        for (ItemStack stack : client.player.getInventory().offHand) {
            if (isHealingPotion(stack)) total += stack.getCount();
        }
        return total;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) {
            return false;
        }
        return PotionUtil.getPotion(stack) == Potions.HEALING || PotionUtil.getPotion(stack) == Potions.STRONG_HEALING;
    }

    private static final class CounterEntry {
        final Item icon;
        final int count;
        final String label;

        CounterEntry(Item icon, int count, String label) {
            this.icon = icon;
            this.count = count;
            this.label = label;
        }
    }
}
