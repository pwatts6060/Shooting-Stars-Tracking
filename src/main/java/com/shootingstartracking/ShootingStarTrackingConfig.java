package com.shootingstartracking;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Shooting Stars Tracking")
public interface ShootingStarTrackingConfig extends Config
{
    int TIME_TILL_REMOVE = 85;
    boolean DISPLAY_AS_MINUTES = false;

    @ConfigItem(
            keyName = "timeTillRemove",
            name = "Minutes till stars are removed",
            description = "The number of minutes after landing the star will remain on the list")
    default int timeTillRemoveConfig() {return TIME_TILL_REMOVE;}

    @ConfigItem(
            keyName = "displayAsTime",
            name = "Show minutes remaining",
            description = "Show the minutes remaining rather than the expected time"
    )
    default boolean displayAsMinutes() {return DISPLAY_AS_MINUTES;}
}
