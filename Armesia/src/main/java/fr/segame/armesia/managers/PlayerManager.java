package fr.segame.armesia.managers;

import fr.segame.armesia.player.GamePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final Map<UUID, GamePlayer> players = new HashMap<>();

    public GamePlayer getPlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, GamePlayer::new);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }
}