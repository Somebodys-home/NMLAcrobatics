package io.github.NoOne.nMLAcrobatics.maneuvers;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PerformedManeuverEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String maneuver;

    public PerformedManeuverEvent(@NotNull Player player, String maneuver) {
        this.player = player;
        this.maneuver = maneuver;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public String getManeuver() {
        return maneuver;
    }
}