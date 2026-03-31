package fr.segame.armesia.mobs;

public class MobData {

    private final String id;
    private final String name;
    private final int level;
    private final double health;
    private final int xp;
    private final int money;
    private final String lootTable;

    public MobData(String id, String name, int level, double health, int xp, int money, String lootTable) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.health = health;
        this.xp = xp;
        this.money = money;
        this.lootTable = lootTable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public double getHealth() { return health; }
    public int getXp() { return xp; }
    public int getMoney() { return money; }
    public String getLootTable() { return lootTable; }
}