package pl.kingdomcore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.kingdomcore.models.Kingdom;

public class KingdomCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player creator;
    private final Kingdom kingdom;

    public KingdomCreateEvent(Player creator, Kingdom kingdom) {
        this.creator = creator;
        this.kingdom = kingdom;
    }

    public Player getCreator() {
        return creator;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
