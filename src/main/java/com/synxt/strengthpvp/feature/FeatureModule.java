package com.synxt.strengthpvp.feature;

/** Client-side visual, informational and convenience modules. No automated aiming/attacking. */
public enum FeatureModule {
    ITEM_COUNTER("Счётчик предметов", "Item counter", "Считает расходники во всём инвентаре", "Counts supplies in the whole inventory", Category.HUD, true),
    ARMOR_HUD("Прочность брони", "Armor durability", "Проценты, единицы и предупреждение", "Percent, exact durability and warning", Category.HUD, true, "Порог", "Threshold", 15, 10, 30, 5),
    COOLDOWN_HUD("Кулдауны в хотбаре", "Hotbar cooldowns", "Таймер прямо на слоте предмета", "Timer overlay on the item's hotbar slot", Category.HUD, true),
    STATUS_EFFECTS("Эффекты", "Status effects", "Компактные баффы и время", "Compact buffs and remaining time", Category.HUD, true),
    ATTACK_TIMING("Тайминг удара", "Attack timing", "Показывает заряд сильной атаки", "Shows the strong-hit charge meter", Category.HUD, false),
    TARGET_INFO("Информация о цели", "Target information", "Здоровье, дистанция и тотемы цели", "Target health, distance and totem pops", Category.HUD, false),
    COORDINATES_HUD("Координаты", "Coordinates", "Точные координаты и измерение", "Player coordinates and dimension", Category.HUD, true),
    DIRECTION_HUD("Компас", "Compass", "Сторона света и угол поворота", "Cardinal direction and yaw", Category.HUD, false),
    PERFORMANCE_HUD("FPS и пинг", "FPS and ping", "Частота кадров и задержка сервера", "FPS and server latency", Category.HUD, false),
    KEYSTROKES_HUD("Клавиши", "Keystrokes", "Отображает WASD, прыжок, бег и присед", "Displays WASD, jump, sprint and sneak", Category.HUD, false),
    HELD_DURABILITY("Прочность предмета", "Held item durability", "Показывает износ предмета в руке", "Shows held item durability", Category.HUD, false, "Порог", "Warning threshold", 25, 5, 80, 5),
    WORLD_CLOCK("Время мира", "World clock", "Игровое время и номер дня", "In-game time and day number", Category.HUD, false),
    SESSION_TIMER("Время сессии", "Session timer", "Продолжительность текущей сессии", "Current session duration", Category.HUD, false),
    HIT_RANGE("HitRange", "HitRange", "Показывает дистанцию удара и границы досягаемости", "Shows hit distance and reach boundaries", Category.COMBAT, false, "Дальность удара", "Hit distance", 30, 20, 65, 5),
    COMBO_COUNTER("Combo Counter", "Combo Counter", "Считает подтверждённые серии попаданий по цели", "Counts observed hit streaks on the target", Category.COMBAT, false),
    TARGET_DIRECTION("Направление цели", "Target direction", "Показывает угол и направление до цели под прицелом", "Bearing and direction to the crosshair target", Category.COMBAT, false),
    AUTO_EZZ("AutoEzz", "AutoEzz", "Отправляет настраиваемый текст после подтверждённого добивания", "Sends a custom message after a tracked target dies", Category.COMBAT, false),
    ENEMY_ARMOR("Броня противника", "Enemy armor", "Предметы брони и основные чары цели", "Target armor pieces and key enchants", Category.COMBAT, false),
    TOTEM_POP("Сбитые тотемы", "Totem pop counter", "Считает срабатывания тотемов игроков", "Counts detected totem activations", Category.COMBAT, false),
    PROJECTILE_PREDICTOR("Предиктор снарядов", "Projectile predictor", "Дуга броска и ближайшие снаряды", "Throw arc and nearby projectiles", Category.COMBAT, false, "Дальность", "Range", 24, 12, 48, 4),
    CUSTOM_HITBOXES("Цветные хитбоксы", "Custom hitboxes", "Цвет по hurt-time и дистанции", "Color by hurt-time and distance", Category.COMBAT, false, "Радиус", "Range", 12, 4, 24, 2),
    DANGER_INDICATOR("Опасные кристаллы", "Crystal danger", "Приблизительная оценка угрозы взрывов", "Approximate nearby explosion threat", Category.COMBAT, false, "Радиус", "Radius", 6, 3, 8, 1),
    ASPECT_RATIO("Aspect Ratio", "Aspect Ratio", "Растягивает проекцию мира под выбранное соотношение сторон", "Stretches the world projection to a selected aspect ratio", Category.VISUAL, false, "Формат", "Preset", 3, 1, 3, 1),
    BETTER_PVP("BetterPvP", "BetterPvP", "Настройка положения руки, наклона и собственной анимации замаха", "Customize hand position, tilt and your own swing animation", Category.VISUAL, false, "Сила анимации", "Animation strength", 100, 0, 100, 1),
    VIEW_CLEANER("Чистый обзор", "View cleaner", "Убирает hurt-cam, тошноту и часть частиц", "Removes hurt cam, nausea and selected particles", Category.VISUAL, false),
    FREE_LOOK("Свободный обзор", "FreeLook", "Поворачивает камеру отдельно от направления игрока", "Look around without changing server-facing rotation", Category.VISUAL, false),
    LOW_FIRE("Низкий огонь", "Low fire", "Уменьшает огонь перед камерой и огонь на земле", "Reduces first-person and ground fire", Category.VISUAL, false),
    CROSSHAIR_CUSTOM("Кастомный прицел", "Custom crosshair", "Тонкий настраиваемый по цвету прицел", "Thin crosshair using the module accent color", Category.VISUAL, false),
    JUNK_DROPPER("Сброс мусора", "Junk dropper", "Выбрасывает выбранный мусор по клавише", "Drops selected junk items on key press", Category.UTILITY, false),
    MISCLICK_PREVENTION("Защита от миссклика", "Misclick guard", "Блокирует опасное использование предметов", "Blocks risky item use in common cases", Category.UTILITY, false),
    AUTO_TOTEM("Автототем", "AutoTotem", "Переставляет тотем во вторую руку", "Moves a totem into the offhand", Category.UTILITY, false),
    FOOD_HUD("Еда и насыщение", "Food and saturation", "Полосы голода и насыщения", "Hunger and saturation bars", Category.HUD, false),
    BIOME_HUD("Биом", "Biome", "Показывает тип текущего биома", "Shows the current biome category", Category.HUD, false),
    DEATH_MARKER("Последняя смерть", "Last death", "Запоминает координаты последней смерти", "Remembers your last death coordinates", Category.HUD, false),
    INVENTORY_SLOTS_HUD("Свободные слоты", "Free inventory slots", "Считает свободные слоты инвентаря", "Counts free inventory slots", Category.HUD, false),
    SHIELD_STATUS("Щит в руке", "Shield status", "Прочность и статус щита", "Shield durability and blocking status", Category.HUD, false),
    TOTEM_HAND_ALERT("Тотем не в руке", "Totem offhand alert", "Предупреждает, если тотем есть в инвентаре, но не во второй руке", "Warns if a totem is in inventory but not in the offhand", Category.EXTRA, false),
    ARMOR_WARNINGS("Предупреждение брони", "Armor warnings", "Отдельно предупреждает о почти сломанной броне", "Warns when an armor piece is nearly broken", Category.EXTRA, false, "Порог %", "Threshold %", 20, 5, 50, 5),
    LOW_HEALTH_ALERT("Низкое здоровье", "Low health alert", "Звуковой сигнал при малом HP", "Sound alert at low HP", Category.EXTRA, false, "HP", "Health", 8, 4, 14, 1),
    TOTEM_REFILL_ALERT("Остаток тотемов", "Totem supply alert", "Предупреждает, когда тотемов мало", "Warns when totems are running low", Category.EXTRA, false, "Остаток", "Remaining", 1, 0, 5, 1),
    QUICK_HEAL_REMINDER("Напоминание о лечении", "Quick heal reminder", "Подсказывает, если есть хилки и мало HP", "Reminds you to heal when health is low", Category.EXTRA, false, "HP", "Health", 10, 4, 16, 1),
    PEARL_READY_ALERT("Перл готов", "Pearl ready alert", "Сигнализирует о завершении кулдауна", "Alerts when the pearl cooldown ends", Category.EXTRA, false);

    public final String titleRu, titleEn, descriptionRu, descriptionEn;
    public final Category category;
    public final boolean defaultEnabled;
    public final String optionRu, optionEn;
    public final int defaultOption, minOption, maxOption, optionStep;

    FeatureModule(String titleRu, String titleEn, String descriptionRu, String descriptionEn,
                  Category category, boolean defaultEnabled) {
        this(titleRu, titleEn, descriptionRu, descriptionEn, category, defaultEnabled, "", "", 0, 0, 0, 0);
    }
    FeatureModule(String titleRu, String titleEn, String descriptionRu, String descriptionEn,
                  Category category, boolean defaultEnabled, String optionRu, String optionEn,
                  int defaultOption, int minOption, int maxOption, int optionStep) {
        this.titleRu=titleRu; this.titleEn=titleEn; this.descriptionRu=descriptionRu; this.descriptionEn=descriptionEn;
        this.category=category; this.defaultEnabled=defaultEnabled; this.optionRu=optionRu; this.optionEn=optionEn;
        this.defaultOption=defaultOption; this.minOption=minOption; this.maxOption=maxOption; this.optionStep=optionStep;
    }
    public boolean hasOption() { return optionStep > 0 && maxOption > minOption; }
    public String title(boolean en) { return en ? titleEn : titleRu; }
    public String description(boolean en) { return en ? descriptionEn : descriptionRu; }
    public String optionLabel(boolean en) { return en ? optionEn : optionRu; }
    public boolean hasHudWidget() {
        switch (this) {
            case ITEM_COUNTER: case ARMOR_HUD: case COOLDOWN_HUD: case STATUS_EFFECTS: case ATTACK_TIMING:
            case TARGET_INFO: case ENEMY_ARMOR: case TOTEM_POP: case DANGER_INDICATOR: case MISCLICK_PREVENTION:
            case LOW_HEALTH_ALERT: case TOTEM_REFILL_ALERT: case QUICK_HEAL_REMINDER: case PEARL_READY_ALERT:
            case COORDINATES_HUD: case DIRECTION_HUD: case PERFORMANCE_HUD: case KEYSTROKES_HUD:
            case HELD_DURABILITY: case WORLD_CLOCK: case SESSION_TIMER: case HIT_RANGE: case COMBO_COUNTER:
            case TARGET_DIRECTION: case FOOD_HUD: case BIOME_HUD: case DEATH_MARKER:
            case INVENTORY_SLOTS_HUD: case SHIELD_STATUS: case TOTEM_HAND_ALERT: case ARMOR_WARNINGS:
                return true;
            default: return false;
        }
    }
    public enum Category {
        HUD("HUD", "HUD"), COMBAT("Бой", "Combat"), VISUAL("Визуал", "Visuals"),
        UTILITY("Утилиты", "Utility"), EXTRA("Дополнительно", "Extra");
        public final String titleRu, titleEn;
        Category(String ru, String en) { titleRu=ru; titleEn=en; }
        public String title(boolean en) { return en ? titleEn : titleRu; }
    }
}
