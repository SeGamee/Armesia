package fr.segame.armesia.zones;

public enum SpawnCondition {
    ALWAYS,
    DAY,
    NIGHT;

    public static SpawnCondition fromString(String s) {
        try { return valueOf(s.toUpperCase()); }
        catch (Exception e) { return ALWAYS; }
    }
}

