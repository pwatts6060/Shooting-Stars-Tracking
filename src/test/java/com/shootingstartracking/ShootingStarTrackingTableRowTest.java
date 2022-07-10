package com.shootingstartracking;

import java.time.Instant;
import junit.framework.TestCase;

public class ShootingStarTrackingTableRowTest extends TestCase
{
	public void testConvertTime()
	{
		assertEquals("0:00", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli()));
		assertEquals("0:15", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 15 * 1000));
		assertEquals("-0:15", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() - 15 * 1000));
		assertEquals("1:15", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 75 * 1000));
		assertEquals("-1:15", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() - 75 * 1000));
		assertEquals("-1:45", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() - 105 * 1000));
		assertEquals("60:00", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 60 * 60 * 1000));
		assertEquals("600m", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 600 * 60 * 1000 + 15 * 1000));
		assertEquals("100m", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 100 * 60 * 1000 + 15 * 1000));
		assertEquals("99:15", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() + 99 * 60 * 1000 + 15 * 1000));
		assertEquals("-600m", ShootingStarTrackingTableRow.convertTime(Instant.now().toEpochMilli() - 600 * 60 * 1000 - 15 * 1000));
	}
}