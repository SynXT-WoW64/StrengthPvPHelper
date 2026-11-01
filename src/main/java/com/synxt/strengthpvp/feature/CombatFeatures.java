package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatFeatures {
    private static final Map<UUID, Integer> POP_COUNTS = new HashMap<>();
    private static int autoTotemDelay;
    private static int lowHealthDelay;
    private static int totemAlertDelay;
    private static boolean pearlWasCooling;
    private static int pearlReadyDisplayTicks;
    private static int previousTotems = -1;
    private static final Deque<Integer> JUNK_DROP_QUEUE = new ArrayDeque<>();
    private static int junkDropDelay;
    private static boolean cleanerApplied;
    private static float oldDistortion;
    private static ParticlesMode oldParticles;
    private static PlayerEntity lastAttackedPlayer;
    private static boolean lastTargetDamaged;
    private static int lastTargetAge;
    private static int autoEzzCooldown;
    private static UUID comboTargetId;
    private static int previousTargetHurtTime;
    private static int comboCount;
    private static int comboTicks;
    private static boolean hasDeathMarker;
    private static double deathX, deathY, deathZ;
    private static boolean playerAliveLastTick;

    private CombatFeatures() {}

    /** Called before vanilla input processing to guard risky use-clicks. */
    public static void preTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null || !ModConfig.isEnabled(FeatureModule.MISCLICK_PREVENTION)) return;
        ItemStack held = client.player.getMainHandStack();
        ItemStack offhand = client.player.getOffHandStack();
        if (isUnsafeUse(client, held, offhand)) {
            while (client.options.keyUse.wasPressed()) { /* consume */ }
            client.options.keyUse.setPressed(false);
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            restoreCleaner(client);
            POP_COUNTS.clear();
            pearlReadyDisplayTicks = 0;
            pearlWasCooling = false;
            previousTotems = -1;
            JUNK_DROP_QUEUE.clear();
            junkDropDelay = 0;
            lastAttackedPlayer = null;
            lastTargetDamaged = false;
            comboTargetId = null;
            comboCount = 0;
            comboTicks = 0;
            playerAliveLastTick = false;
            return;
        }
        if (autoTotemDelay > 0) autoTotemDelay--;
        if (lowHealthDelay > 0) lowHealthDelay--;
        if (totemAlertDelay > 0) totemAlertDelay--;
        if (pearlReadyDisplayTicks > 0) pearlReadyDisplayTicks--;
        if (autoEzzCooldown > 0) autoEzzCooldown--;
        processJunkDropQueue(client);

        applyViewCleaner(client);
        updateAutoEzz(client);
        updateCombo(client);
        boolean playerAliveNow = client.player.getHealth() > 0.0f;
        if (!playerAliveNow && playerAliveLastTick) {
            deathX = client.player.getX();
            deathY = client.player.getY();
            deathZ = client.player.getZ();
            hasDeathMarker = true;
        }
        playerAliveLastTick = playerAliveNow;

        if (ModConfig.isEnabled(FeatureModule.AUTO_TOTEM) && autoTotemDelay == 0) {
            moveTotemToOffhand(client);
            autoTotemDelay = 8;
        }
        if (ModConfig.isEnabled(FeatureModule.LOW_HEALTH_ALERT)) {
            int threshold = ModConfig.getOption(FeatureModule.LOW_HEALTH_ALERT);
            if (client.player.getHealth() <= threshold && lowHealthDelay == 0) {
                alert(client, "Низкое здоровье: " + Math.round(client.player.getHealth()) + " HP!",
                        "Low health: " + Math.round(client.player.getHealth()) + " HP!");
                lowHealthDelay = 100;
            }
        }
        int totemCount = countItem(client, Items.TOTEM_OF_UNDYING);
        int totemThreshold = ModConfig.getOption(FeatureModule.TOTEM_REFILL_ALERT);
        if (ModConfig.isEnabled(FeatureModule.TOTEM_REFILL_ALERT)
                && totemCount <= totemThreshold
                && (previousTotems > totemThreshold || previousTotems < 0) && totemAlertDelay == 0) {
            alert(client, "Осталось тотемов: " + totemCount, "Totems remaining: " + totemCount);
            totemAlertDelay = 140;
        }
        previousTotems = totemCount;

        boolean pearlCooling = client.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL);
        if (ModConfig.isEnabled(FeatureModule.PEARL_READY_ALERT) && pearlWasCooling && !pearlCooling) {
            alert(client, "Эндер-жемчуг готов", "Ender pearl ready");
            pearlReadyDisplayTicks = 40;
        }
        pearlWasCooling = pearlCooling;
    }

    /** Called by a tiny input hook when the local player actually issues an attack click. */
    public static void recordAttack(Entity target) {
        if (!(target instanceof PlayerEntity) || target == MinecraftClient.getInstance().player) return;
        lastAttackedPlayer = (PlayerEntity) target;
        lastTargetDamaged = false;
        lastTargetAge = 0;
    }

    private static void updateAutoEzz(MinecraftClient client) {
        if (lastAttackedPlayer == null) return;
        lastTargetAge++;
        if (lastAttackedPlayer.hurtTime > 0) lastTargetDamaged = true;
        // Check the death health before the entity removal flag: remote player entities
        // can be removed from the client world immediately after their death packet.
        if (lastAttackedPlayer.getHealth() <= 0.0f && lastTargetDamaged) {
            if (ModConfig.isEnabled(FeatureModule.AUTO_EZZ) && autoEzzCooldown == 0
                    && client.currentScreen == null && client.player != null && client.player.networkHandler != null) {
                String message = ModConfig.getAutoEzzText().replace("%player%", lastAttackedPlayer.getName().getString());
                if (!message.trim().isEmpty()) {
                    client.player.sendChatMessage(message);
                    autoEzzCooldown = 100;
                }
            }
            lastAttackedPlayer = null;
            lastTargetDamaged = false;
            return;
        }
        if (lastTargetAge > 240 || lastAttackedPlayer.isRemoved()) {
            lastAttackedPlayer = null;
            lastTargetDamaged = false;
        }
    }

    private static void updateCombo(MinecraftClient client) {
        if (comboTicks > 0) comboTicks--;
        else comboCount = 0;
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult)) {
            comboTargetId = null; previousTargetHurtTime = 0; return;
        }
        Entity target = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
        if (!(target instanceof PlayerEntity) || target == client.player || !target.isAlive()) {
            comboTargetId = null; previousTargetHurtTime = 0; return;
        }
        if (!target.getUuid().equals(comboTargetId)) {
            comboTargetId = target.getUuid(); previousTargetHurtTime = 0; comboCount = 0;
        }
        int hurt = ((PlayerEntity) target).hurtTime;
        if (hurt > 0 && previousTargetHurtTime == 0) {
            comboCount++;
            comboTicks = 40;
        }
        previousTargetHurtTime = hurt;
    }

    public static int getComboCount() { return comboCount; }
    public static int getComboTicks() { return comboTicks; }
    public static boolean hasDeathMarker() { return hasDeathMarker; }
    public static double getDeathX() { return deathX; }
    public static double getDeathY() { return deathY; }
    public static double getDeathZ() { return deathZ; }

    private static boolean isInteractiveBlock(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof BlockHitResult)) return false;
        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        String id = state.getBlock().getTranslationKey().toLowerCase();
        return id.contains("chest") || id.contains("barrel") || id.contains("anvil")
                || id.contains("door") || id.contains("trapdoor") || id.contains("button")
                || id.contains("fence_gate") || id.contains("crafting_table") || id.contains("furnace")
                || id.contains("shulker_box") || id.contains("hopper") || id.contains("lever")
                || id.contains("enchanting_table") || id.contains("brewing_stand") || id.contains("bed");
    }

    private static void applyViewCleaner(MinecraftClient client) {
        if (ModConfig.isEnabled(FeatureModule.VIEW_CLEANER)) {
            if (!cleanerApplied) {
                oldDistortion = client.options.distortionEffectScale;
                oldParticles = client.options.particles;
                cleanerApplied = true;
            }
            client.options.distortionEffectScale = 0.0f;
            client.options.particles = ParticlesMode.MINIMAL;
        } else {
            restoreCleaner(client);
        }
    }

    private static void restoreCleaner(MinecraftClient client) {
        if (!cleanerApplied) return;
        client.options.distortionEffectScale = oldDistortion;
        client.options.particles = oldParticles;
        cleanerApplied = false;
    }

    /** Called from the vanilla entity-status packet mixin; status 35 is a real totem activation. */
    public static void recordTotemPop(PlayerEntity target, MinecraftClient client) {
        if (target == null || target == client.player) return;
        POP_COUNTS.put(target.getUuid(), POP_COUNTS.getOrDefault(target.getUuid(), 0) + 1);
        if (ModConfig.isEnabled(FeatureModule.TOTEM_POP) && client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.65f, 1.3f);
        }
    }

    private static void moveTotemToOffhand(MinecraftClient client) {
        // Never send player-inventory slot ids while a chest/container is open.
        if (client.currentScreen != null || client.player.currentScreenHandler != client.player.playerScreenHandler) return;
        ItemStack offhand = client.player.getInventory().offHand.get(0);
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;
        int found = -1;
        for (int i = 0; i < client.player.getInventory().main.size(); i++) {
            if (client.player.getInventory().main.get(i).getItem() == Items.TOTEM_OF_UNDYING) {
                found = i;
                break;
            }
        }
        if (found < 0 || client.interactionManager == null) return;
        int slotId = found < 9 ? 36 + found : found;
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotId, 40,
                SlotActionType.SWAP, client.player);
        client.player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.45f, 1.5f);
    }

    /** Queue junk stacks and drop at most one stack per tick to avoid packet bursts/desync. */
    public static void dropJunk(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.currentScreen != null
                || client.player.currentScreenHandler != client.player.playerScreenHandler || !JUNK_DROP_QUEUE.isEmpty()) return;
        for (int i = 0; i < client.player.getInventory().main.size(); i++) {
            ItemStack stack = client.player.getInventory().main.get(i);
            if (!stack.isEmpty() && isJunk(stack.getItem())) JUNK_DROP_QUEUE.addLast(i);
        }
        junkDropDelay = 0;
    }

    private static void processJunkDropQueue(MinecraftClient client) {
        if (!ModConfig.isEnabled(FeatureModule.JUNK_DROPPER)) {
            JUNK_DROP_QUEUE.clear();
            return;
        }
        if (JUNK_DROP_QUEUE.isEmpty()) return;
        if (client.currentScreen != null || client.player.currentScreenHandler != client.player.playerScreenHandler
                || client.interactionManager == null) {
            JUNK_DROP_QUEUE.clear();
            return;
        }
        if (junkDropDelay > 0) {
            junkDropDelay--;
            return;
        }
        while (!JUNK_DROP_QUEUE.isEmpty()) {
            int inventoryIndex = JUNK_DROP_QUEUE.removeFirst();
            if (inventoryIndex < 0 || inventoryIndex >= client.player.getInventory().main.size()) continue;
            ItemStack stack = client.player.getInventory().main.get(inventoryIndex);
            if (stack.isEmpty() || !isJunk(stack.getItem())) continue;
            int slotId = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotId, 1,
                    SlotActionType.THROW, client.player);
            client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.25f, 0.65f);
            junkDropDelay = 2;
            return;
        }
    }

    private static boolean isJunk(Item item) {
        return item == Items.DIRT || item == Items.COBBLESTONE || item == Items.ROTTEN_FLESH
                || item == Items.GLASS_BOTTLE || item == Items.GRAVEL || item == Items.ANDESITE
                || item == Items.DIORITE || item == Items.GRANITE;
    }

    public static int countItem(MinecraftClient client, Item item) {
        if (client == null || client.player == null || item == null) return 0;
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) if (stack.getItem() == item) total += stack.getCount();
        for (ItemStack stack : client.player.getInventory().offHand) if (stack.getItem() == item) total += stack.getCount();
        return total;
    }

    private static void alert(MinecraftClient client, String ru, String en) {
        if (client.player == null) return;
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.85f);
        client.player.sendMessage(new LiteralText(ModConfig.isEnglish() ? en : ru), true);
    }

    public static int getPopCount(PlayerEntity player) {
        return POP_COUNTS.getOrDefault(player.getUuid(), 0);
    }

    public static boolean isPearlReadyDisplayActive() {
        return pearlReadyDisplayTicks > 0;
    }

    public static boolean isInteractiveTarget(MinecraftClient client) {
        return client != null && client.world != null && isInteractiveBlock(client);
    }

    public static boolean isUnsafeUseTarget(MinecraftClient client) {
        if (client == null || client.player == null) return false;
        return isUnsafeUse(client, client.player.getMainHandStack(), client.player.getOffHandStack());
    }

    private static boolean isUnsafeUse(MinecraftClient client, ItemStack mainHand, ItemStack offhand) {
        boolean pearlInHand = mainHand.getItem() == Items.ENDER_PEARL || offhand.getItem() == Items.ENDER_PEARL;
        boolean appleInHand = isGoldenApple(mainHand) || isGoldenApple(offhand);
        boolean appleAtFullHealth = appleInHand && client.player.getHealth() >= client.player.getMaxHealth();
        return (pearlInHand && isInteractiveBlock(client)) || appleAtFullHealth;
    }

    private static boolean isGoldenApple(ItemStack stack) {
        return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }
}
