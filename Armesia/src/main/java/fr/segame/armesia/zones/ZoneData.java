package fr.segame.armesia.zones;

import org.bukkit.Location;
import java.util.List;

public class ZoneData {

    private final String id;
    private final Location pos1;
    private final Location pos2;
    private final List<String> mobs;
    private final int max;

    public ZoneData(String id, Location pos1, Location pos2, List<String> mobs, int max) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.mobs = mobs;
        this.max = max;
    }

    public String getId() { return id; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public List<String> getMobs() { return mobs; }
    public int getMax() { return max; }
}