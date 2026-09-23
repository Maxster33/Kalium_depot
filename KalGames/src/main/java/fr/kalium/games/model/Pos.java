package fr.kalium.games.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

/** Position dans l'espace de creation (coordonnees d'origine du moderateur). */
public record Pos(double x, double y, double z, float yaw, float pitch) {

    public static Pos of(Location location) {
        return new Pos(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Pos parse(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split(";");
        if (parts.length != 5) {
            return null;
        }
        try {
            return new Pos(
                    Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Float.parseFloat(parts[3]), Float.parseFloat(parts[4]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String serialize() {
        return String.format(Locale.ROOT, "%.2f;%.2f;%.2f;%.1f;%.1f", x, y, z, yaw, pitch);
    }

    /** Position dans un monde, decalee de (dx, dz) (decalage de l'instance). */
    public Location at(World world, double dx, double dz) {
        return new Location(world, x + dx, y, z + dz, yaw, pitch);
    }

    public String pretty() {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z);
    }
}
