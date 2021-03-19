package com.shootingstartracking;

import lombok.Getter;

public class ShootingStarTrackingData {

    @Getter
    private final int world;
    @Getter
    private final ShootingStarLocations location;
    @Getter
    private final long time;

    public ShootingStarTrackingData(int world, String location, long time)
    {
        this.world = world;
        this.location = parseLocation(location);
        this.time = time;
    }

    public ShootingStarLocations parseLocation(String location)
    {
        for (ShootingStarLocations loc: ShootingStarLocations.values())
        {
            if (loc.getLocation().contains(location))
            {
                return loc;
            }
        }
        return ShootingStarLocations.UNKNOWN;
    }

    @Override
    public String toString()
    {
        String s = "";
        s += "world:" +world + " location:" + location + " time:" + time;
        return s;
    }

    enum ShootingStarLocations {
        ASGARNIA("Asgarnia"),
        KARAMJA("Crandor or Karamja"),
        FELDIP_HILLS("Feldip Hills or the Isle of Souls"),
        FOSSIL_ISLAND("Fossil Island or Mos Le'Harmless"),
        FREMENNIK("Fremennik Lands or Lunar Isle"),
        KOUREND("Great Kourend"),
        KANDARIN("Kandarin"),
        KEBOS("Kebos Lowlands"),
        KHARIDIAN_DESERT("Kharidian Desert"),
        MISTHALIN("Misthalin"),
        MORYTANIA("Morytania"),
        PISCATORIS("Piscatoris or the Gnome Stronghold"),
        TIRANNWN("Tirannwn"),
        WILDERNESS("Wilderness"),
        UNKNOWN("Unknown");

        @Getter
        private final String location;

        ShootingStarLocations(String location)
        {
            this.location = location;
        }
    }
}
