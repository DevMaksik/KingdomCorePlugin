package pl.kingdomcore.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.kingdomcore.models.Kingdom;

public class KingdomPrestigeChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Kingdom kingdom;
    private final long oldPrestige;
    private final long newPrestige;
    private final String reason;

    public KingdomPrestigeChangeEvent(Kingdom kingdom, long oldPrestige, long newPrestige, String reason) {
        this.kingdom = kingdom;
        this.oldPrestige = oldPrestige;
        this.newPrestige = newPrestige;
        this.reason = reason;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public long getOldPrestige() {
        return oldPrestige;
    }

    public long getNewPrestige() {
        return newPrestige;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
