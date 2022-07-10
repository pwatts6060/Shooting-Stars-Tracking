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
		return "world:" + world + " location:" + location + " time:" + time;
    }

    enum ShootingStarLocations {
        ASGARNIA("Asgarnia"),
        KARAMJA("Crandor or Karamja", "Crandor / Karamja"),
        FELDIP_HILLS("Feldip Hills or on the Isle of Souls", "Feldip Hills / Isle of Souls"),
        FOSSIL_ISLAND("Fossil Island or on Mos Le'Harmless", "Fossil Island / Mos Le'Harmless"),
        FREMENNIK("Fremennik Lands or on Lunar Isle", "Fremennik Lands / Lunar Isle"),
        KOUREND("Great Kourend"),
        KANDARIN("Kandarin"),
        KEBOS("Kebos Lowlands"),
        KHARIDIAN_DESERT("Kharidian Desert"),
        MISTHALIN("Misthalin"),
        MORYTANIA("Morytania"),
        PISCATORIS("Piscatoris or the Gnome Stronghold", "Piscatoris / Gnome Stronghold"),
        TIRANNWN("Tirannwn"),
        WILDERNESS("Wilderness"),
        UNKNOWN("Unknown");

        public static final ShootingStarLocations[] values = ShootingStarLocations.values();

        @Getter
        private final String location;
		@Getter
		private final String shortLocation;

        ShootingStarLocations(String location, String shortLocation)
        {
            this.location = location;
			this.shortLocation = shortLocation;
		}

		ShootingStarLocations(String location)
		{
			this(location, location);
		}
    }
}
