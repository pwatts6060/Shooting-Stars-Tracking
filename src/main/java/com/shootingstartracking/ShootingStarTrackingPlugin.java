package com.shootingstartracking;

import javax.inject.Inject;
import javax.swing.*;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Shooting Star Tracking"
)
public class ShootingStarTrackingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ShootingStarTrackingConfig config;

	private ShootingStarTrackingPanel panel;

	private NavigationButton navButton;

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() == 229)
		{
			extractStarInformation();
		}
	}

	@Provides
	ShootingStarTrackingConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShootingStarTrackingConfig.class);
	}

	private void extractStarInformation()
	{
		clientThread.invokeLater(() ->
		{
			final Widget widget = client.getWidget(229,1);
			if (widget == null) {
				return;
			}
			final String[] locations = new String[]{"Misthalin","Desert","Wilderness","Asgarnia","Karamja","Fremennik","Tirannwn","Kandarin","Fossil","Feldip Hills",
					"Kebos","Kourend","Morytania","Piscatoris"};
			ZonedDateTime time = ZonedDateTime.now(ZoneId.of("UTC"));
			String widgetText;

			try{
				widgetText = widget.getText().replace("<br>"," ");
			} catch (NullPointerException e) {
				return;
			}

			Optional<String> match = Arrays.stream(locations).filter(widgetText::contains).findAny();
			if (!match.isPresent())
			{
				log.info("No match found");
				return;
			}

			Pattern p = Pattern.compile("in the next( ([12]) hour)? ([0-9]+) minutes|next ([0-9]+) to");
			Matcher matcher = p.matcher(widgetText);
			if (matcher.find())
			{
				int hours  = 0;
				int mins = 0;
				if (matcher.group(4) != null)
				{
					mins = Integer.parseInt(matcher.group(4));
				}
				else if (matcher.group(2) != null && matcher.group(3) != null)
				{
					hours = Integer.parseInt(matcher.group(2));
					mins = Integer.parseInt(matcher.group(3));
				}
				time = time.plusHours(hours);
				time = time.plusMinutes(mins);
			}
			addToList(new ShootingStarTrackingData(client.getWorld(),match.get(),time.toInstant().toEpochMilli()));
		});
	}

	private void addToList(ShootingStarTrackingData data)
	{
		log.info(data.toString());
		if (panel.getStarData().stream().anyMatch(o -> o.getWorld()==data.getWorld()))
		{
			return;
		}
		panel.getStarData().add(data);
		SwingUtilities.invokeLater(() -> panel.updateList());
	}

	@Schedule(
			period = 20,
			unit = ChronoUnit.SECONDS
	)
	public void checkDepletedStars()
	{
		List<ShootingStarTrackingData> stars = new ArrayList<>(panel.getStarData());
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

		for (ShootingStarTrackingData star : panel.getStarData())
		{
			if (star.getTime() < now.minusMinutes(config.timeTillRemoveConfig()).toInstant().toEpochMilli())
			{
				stars.remove(star);
			}
		}
		panel.setStarData(stars);
		SwingUtilities.invokeLater(() -> panel.updateList());
	}

	@Override
	protected void startUp() throws Exception
	{
		final BufferedImage icon = ImageUtil.loadImageResource(ShootingStarTrackingPlugin.class, "/shooting_star.png");
		panel = new ShootingStarTrackingPanel();

		navButton = NavigationButton.builder()
				.tooltip("Shooting Star Tracking")
				.icon(icon)
				.panel(panel)
				.priority(6)
				.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
	}
}
