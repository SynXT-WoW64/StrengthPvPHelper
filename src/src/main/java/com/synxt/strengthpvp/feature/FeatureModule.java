package com.synxt.strengthpvp.feature;

public enum FeatureModule {
    ITEM_COUNTER("Счётчик предметов", "Показывает расходники во всём инвентаре", Category.HUD, true),
    ARMOR_HUD("Прочность брони", "Проценты, оставшиеся единицы и предупреждение", Category.HUD, true),
    COOLDOWN_HUD("Кулдауны", "Перезарядка перлов, хорусов и щита", Category.HUD, false),
    STATUS_EFFECTS("Эффекты", "Компактный список важных эффектов", Category.HUD, true),

    TOTEM_POP("Totem Pop Counter", "Считает срабатывания тотемов игроков", Category.COMBAT, false),
    ENEMY_ARMOR("Броня противника", "Информация о броне и зачарованиях цели", Category.COMBAT, false),
    PROJECTILE_PREDICTOR("Предиктор снарядов", "Траектория и точка приземления", Category.COMBAT, false),
    CUSTOM_HITBOXES("Цветные хитбоксы", "Цвет зависит от hurt-time и дистанции", Category.COMBAT, false),
    HOLD_HIT("HoldHit", "Удар по готовности сильной атаки", Category.COMBAT, false),

    VIEW_CLEANER("Чистый обзор", "Убирает мешающие визуальные эффекты", Category.VISUAL, false),
    DANGER_INDICATOR("Опасные зоны", "Оценка опасности кристаллов и якорей", Category.VISUAL, false),

    JUNK_DROPPER("Сброс мусора", "Быстро выбрасывает выбранные ненужные предметы", Category.UTILITY, false),
    MISCLICK_PREVENTION("Защита от миссклика", "Блокирует случайное использование предметов", Category.UTILITY, false),

    TARGET_INFO("Информация о цели", "Дополнительные данные о существе под прицелом", Category.EXTRA, false),
    LOW_HEALTH_ALERT("Предупреждение о здоровье", "Сигнал при низком уровне здоровья", Category.EXTRA, false),
    TOTEM_REFILL_ALERT("Контроль тотемов", "Предупреждает, если тотемов осталось мало", Category.EXTRA, false);

    public final String title;
    public final String description;
    public final Category category;
    public final boolean implemented;

    FeatureModule(String title, String description, Category category, boolean implemented) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.implemented = implemented;
    }

    public enum Category {
        HUD("HUD"), COMBAT("Бой"), VISUAL("Визуал"), UTILITY("Утилиты"), EXTRA("Дополнительно");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }
}
