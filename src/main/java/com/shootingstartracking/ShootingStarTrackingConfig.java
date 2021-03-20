package com.shootingstartracking;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Shooting Stars Tracking")
public interface ShootingStarTrackingConfig extends Config
{
    int TIME_TILL_REMOVE = 5;

    @ConfigItem(
            keyName = "timeTillRemove",
            name = "Time till stars are removed",
            description = "The number of minutes after landing until the star is removed from the list")
    default int timeTillRemoveConfig() {return TIME_TILL_REMOVE;}
}
