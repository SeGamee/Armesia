package fr.segame.armesiaMobs.mobs;

import org.bukkit.entity.EntityType;

public class MobData {

    private final String id;
    private String name;
    private EntityType entityType;
    private double health;
    private int moneyMin;
    private int moneyMax;
    private int xpMin;
    private int xpMax;
    private String lootTable;

    public MobData(String id, String name, EntityType entityType, double health,
                   int moneyMin, int moneyMax, int xpMin, int xpMax, String lootTable) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
        this.health = health;
        this.moneyMin = moneyMin;
        this.moneyMax = moneyMax;
        this.xpMin = xpMin;
        this.xpMax = xpMax;
        this.lootTable = lootTable;
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }

    public int getMoneyMin() { return moneyMin; }
    public void setMoneyMin(int moneyMin) { this.moneyMin = moneyMin; }

    public int getMoneyMax() { return moneyMax; }
    public void setMoneyMax(int moneyMax) { this.moneyMax = moneyMax; }

    public int getXpMin() { return xpMin; }
    public void setXpMin(int xpMin) { this.xpMin = xpMin; }

    public int getXpMax() { return xpMax; }
    public void setXpMax(int xpMax) { this.xpMax = xpMax; }

    public String getLootTable() { return lootTable; }
    public void setLootTable(String lootTable) { this.lootTable = lootTable; }
}