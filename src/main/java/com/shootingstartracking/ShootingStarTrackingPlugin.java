package com.shootingstartracking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import com.shootingstartracking.ShootingStarTrackingData.ShootingStarLocations;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
	private static final int TELESCOPE_WIDGET_ID = 229;
	private static final ZoneId utcZoneId = ZoneId.of("UTC");

	private static final Pattern minutesThenHoursPattern = Pattern.compile(".* next (\\d+) minutes to (\\d+) hours? (\\d+) .*");
	private static final Pattern minutesPattern = Pattern.compile(".* (\\d+) to (\\d+) .*");
	private static final Pattern hoursPattern = Pattern.compile(".* next (\\d+) hours? (\\d+) minutes? to (\\d+) hours? (\\d+) .*");

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

	@Inject
	private WorldHop worldHop;

	private ShootingStarTrackingPanel panel;

	private NavigationButton navButton;

	@Getter
	private boolean displayAsMinutes;

	@Getter
	@Setter
	private List<ShootingStarTrackingData> starData = new ArrayList<>();

	private int lastWorld;

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
		if (widgetLoaded.getGroupId() == TELESCOPE_WIDGET_ID) {
			clientThread.invokeLater(this::extractStarInformation);
		}
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ShootingStarTrackingConfig.configGroup)) {
			return;
		}

		if (event.getKey().equals(ShootingStarTrackingConfig.displayAsTime)) {
			displayAsMinutes = config.displayAsMinutes();
			panel.updateList(starData);
		}
	}

	@Provides
	ShootingStarTrackingConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShootingStarTrackingConfig.class);
	}

	private void extractStarInformation()
	{
		final Widget widget = client.getWidget(TELESCOPE_WIDGET_ID,1);
		if (widget == null) {
			return;
		}
		ZonedDateTime minTime = ZonedDateTime.now(utcZoneId);
		String widgetText;

		try{
			widgetText = widget.getText().replace("<br>"," ");
		} catch (NullPointerException e) {
			return;
		}
		Optional<ShootingStarLocations> locationOp = Arrays.stream(ShootingStarLocations.values)
			.filter(o -> widgetText.contains(o.getLocation()))
			.findAny();
		if (!locationOp.isPresent())
		{
			log.debug("No match found");
			return;
		}

		int[] minutesInterval = minutesInterval(widgetText);
		if (minutesInterval == null) {
			return;
		}
		ZonedDateTime maxTime = minTime.plusMinutes(minutesInterval[1]);
		minTime = minTime.plusMinutes(minutesInterval[0]);

		ShootingStarTrackingData data = new ShootingStarTrackingData(client.getWorld(), locationOp.get(), minTime.toInstant().toEpochMilli(), maxTime.toInstant().toEpochMilli());
		addToList(data);
		SwingUtilities.invokeLater(() -> panel.updateList(starData));
	}

	private int[] minutesInterval(String widgetText) {

		Matcher m = minutesThenHoursPattern.matcher(widgetText);
		if (m.find())
		{
			int minTime = Integer.parseInt(m.group(1));
			int maxTime = 60 * Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(3));
			return new int[]{minTime, maxTime};
		}

		m = hoursPattern.matcher(widgetText);
		if (m.find())
		{
			int minTime = 60 * Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
			int maxTime = 60 * Integer.parseInt(m.group(3)) + Integer.parseInt(m.group(4));
			return new int[]{minTime, maxTime};
		}

		m = minutesPattern.matcher(widgetText);
		if (m.find())
		{
			int minTime = Integer.parseInt(m.group(1));
			int maxTime = Integer.parseInt(m.group(2));
			return new int[]{minTime, maxTime};
		}
		return null;
	}

	private void addToList(ShootingStarTrackingData data)
	{
		ShootingStarTrackingData oldStar = starData.stream().filter(star -> data.getWorld() == star.getWorld()).findFirst().orElse(null);
		if (oldStar != null) {
			if (data.getMinTime() < oldStar.getMinTime()) {
				return;
			}
			starData.remove(oldStar);
		}
		starData.add(data);
	}

	@Schedule(
			period = 5,
			unit = ChronoUnit.SECONDS
	)
	public void checkDepletedStars()
	{
		List<ShootingStarTrackingData> stars = new ArrayList<>(starData);
		ZonedDateTime now = ZonedDateTime.now(utcZoneId);
		int removeTime = config.timeTillRemoveConfig();
		boolean removed = false;

		for (ShootingStarTrackingData star : starData) {
			if (star.getMinTime() < now.minusMinutes(removeTime).toInstant().toEpochMilli()) {
				stars.remove(star);
				removed = true;
			}
		}

		if (removed) {
			starData = stars;
			SwingUtilities.invokeLater(() -> panel.updateList(starData));
		} else {
			SwingUtilities.invokeLater(() -> panel.updateTimes(starData));
		}
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
				.priority(7)
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
		if (client.getLocalPlayer() == null) {
			return;
		}

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

	public void discordFormat() {
		StringBuilder sb = new StringBuilder();
		for (ShootingStarTrackingData star : starData) {
			sb.append(star.getWorld())
				.append(" ")
				.append(star.getLocation())
				.append(" between ")
				.append("<t:")
				.append(star.getMinTime() / 1000)
				.append(":R>")
				.append(" and ")
				.append("<t:")
				.append(star.getMaxTime() / 1000)
				.append(":R>")
				.append("\n");
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
		sendChatMessage("Star data exported to clipboard in discord format.");
	}

	public void hopTo(ShootingStarTrackingData star)
	{
		worldHop.hop(star.getWorld());
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		worldHop.onGameTick();
	}

	public int getWorld()
	{
		return client.getWorld();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN && client.getWorld() != lastWorld) {
			lastWorld = client.getWorld();
			SwingUtilities.invokeLater(() -> panel.updateList(starData));
		}
	}
}
