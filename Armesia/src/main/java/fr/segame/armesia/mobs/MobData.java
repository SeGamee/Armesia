package fr.segame.armesia.mobs;

import org.bukkit.entity.EntityType;

public class MobData {

    private final String id;
    private String name;
    private EntityType entityType;
    private int level;
    private double health;
    private int money;
    private String lootTable;

    public MobData(String id, String name, EntityType entityType, int level, double health, int money, String lootTable) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
        this.level = level;
        this.health = health;
        this.money = money;
        this.lootTable = lootTable;
    }

    /** Backward-compat sans EntityType */
    public MobData(String id, String name, int level, double health, int money, String lootTable) {
        this(id, name, EntityType.ZOMBIE, level, health, money, lootTable);
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }

    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }

    public String getLootTable() { return lootTable; }
    public void setLootTable(String lootTable) { this.lootTable = lootTable; }
}