package com.shootingstartracking;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ShootingStarTrackingConfig.configGroup)
public interface ShootingStarTrackingConfig extends Config
{
	String configGroup = "Shooting Stars Tracking";

	String displayAsTime = "displayAsTime";

    @ConfigItem(
            keyName = "timeTillRemove",
            name = "Minutes till stars are removed",
            description = "The number of minutes after landing the star will remain on the list")
    default int timeTillRemoveConfig() {return 85;}

    @ConfigItem(
            keyName = displayAsTime,
            name = "Show minutes remaining",
            description = "Show the minutes remaining rather than the expected time"
    )
    default boolean displayAsMinutes() {return true;}
}
