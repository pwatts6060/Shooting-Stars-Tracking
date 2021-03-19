package com.shootingstartracking;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShootingStarTrackingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ShootingStarTrackingPlugin.class);
		RuneLite.main(args);
	}
}