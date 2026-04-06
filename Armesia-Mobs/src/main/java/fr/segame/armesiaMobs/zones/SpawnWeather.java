package fr.segame.armesiaMobs.zones;

public enum SpawnWeather {
    ANY,
    CLEAR,
    RAIN,
    STORM;

    public static SpawnWeather fromString(String s) {
        try { return valueOf(s.toUpperCase()); }
        catch (Exception e) { return ANY; }
    }
}

