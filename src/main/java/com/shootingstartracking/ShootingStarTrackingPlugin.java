package com.shootingstartracking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

	@Inject
	private ChatMessageManager chatMessageManager;

	private ShootingStarTrackingPanel panel;

	private NavigationButton navButton;

	@Getter
	private boolean displayAsMinutes;

	@Getter
	@Setter
	private List<ShootingStarTrackingData> starData = new ArrayList<>();

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
		if (widgetLoaded.getGroupId() == 229) {
			extractStarInformation();
		}
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (event.getGroup().equals("Shooting Stars Tracking")) {
			if (event.getKey().equals("displayAsTime")) {
				displayAsMinutes = config.displayAsMinutes();
				panel.updateList(starData);
			}
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
			ShootingStarTrackingData.ShootingStarLocations[] locations = ShootingStarTrackingData.ShootingStarLocations.values();
			ZonedDateTime time = ZonedDateTime.now(ZoneId.of("UTC"));
			String widgetText;

			try{
				widgetText = widget.getText().replace("<br>"," ");
			} catch (NullPointerException e) {
				return;
			}
			Optional<ShootingStarTrackingData.ShootingStarLocations> match = Arrays.stream(locations).filter(o -> widgetText.contains(o.getLocation())).findAny();
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
		ShootingStarTrackingData oldStar = starData.stream().filter(star -> data.getWorld() == star.getWorld()).findFirst().orElse(null);
		if (oldStar != null) {
			if (data.getTime() < oldStar.getTime()) {
				return;
			}
			starData.remove(oldStar);
		}
		starData.add(data);
		SwingUtilities.invokeLater(() -> panel.updateList(starData));
	}

	@Schedule(
			period = 20,
			unit = ChronoUnit.SECONDS
	)
	public void checkDepletedStars()
	{
		List<ShootingStarTrackingData> stars = new ArrayList<>(starData);
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

		for (ShootingStarTrackingData star : starData) {
			if (star.getTime() < now.minusMinutes(config.timeTillRemoveConfig()).toInstant().toEpochMilli()) {
				stars.remove(star);
			}
		}
		starData = stars;
		SwingUtilities.invokeLater(() -> panel.updateList(starData));
	}

	@Override
	protected void startUp() throws Exception
	{
		final BufferedImage icon = ImageUtil.loadImageResource(ShootingStarTrackingPlugin.class, "/shooting_star.png");
		panel = new ShootingStarTrackingPanel(this);
		displayAsMinutes = config.displayAsMinutes();
		navButton = NavigationButton.builder()
				.tooltip("Shooting Star Tracking")
				.icon(icon)
				.panel(panel)
				.priority(6)
				.build();
		clientToolbar.addNavigation(navButton);
		panel.updateList(starData);
	}

	@Override
	protected void shutDown() throws Exception
	{
		starData.clear();
		clientToolbar.removeNavigation(navButton);
	}

	public void removeStar(ShootingStarTrackingData star)
	{
		starData.remove(star);
		panel.updateList(starData);
	}

	public void importData()
	{
		try {
			final String clipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString().trim();
			JsonArray json = new Gson().fromJson(clipboard, JsonArray.class);
			json.forEach((star) -> {
				ShootingStarTrackingData parsedStar = new Gson().fromJson(star, ShootingStarTrackingData.class);
				addToList(parsedStar);
			});
		} catch (IOException | UnsupportedFlavorException | JsonSyntaxException er) {
			sendChatMessage("Error importing star data.");
			return;
		}
		sendChatMessage("Imported star data.");
		panel.updateList(starData);
	}

	public void sendChatMessage(String chatMessage)
	{
		final String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(chatMessage)
				.build();

		chatMessageManager.queue(
				QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(message)
						.build());
	}

	public void exportData()
	{
		String json = new Gson().toJson(starData);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
		sendChatMessage("Star data exported to clipboard.");
	}
}
