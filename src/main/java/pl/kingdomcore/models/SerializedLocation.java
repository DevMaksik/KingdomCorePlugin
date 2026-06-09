package pl.kingdomcore.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record SerializedLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public static SerializedLocation from(Location location) {
        return new SerializedLocation(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Location toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
