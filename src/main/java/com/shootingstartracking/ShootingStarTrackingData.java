package com.shootingstartracking;

import lombok.Getter;

public class ShootingStarTrackingData {

    @Getter
    private final int world;
    @Getter
    private final ShootingStarLocations location;
    @Getter
    private final long time;

    public ShootingStarTrackingData(int world, ShootingStarLocations location, long time)
    {
        this.world = world;
        this.location = location;
        this.time = time;
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
        FELDIP_HILLS("Feldip Hills or on the Isle of Souls"),
        FOSSIL_ISLAND("Fossil Island or on Mos Le'Harmless"),
        FREMENNIK("Fremennik Lands or on Lunar Isle"),
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
