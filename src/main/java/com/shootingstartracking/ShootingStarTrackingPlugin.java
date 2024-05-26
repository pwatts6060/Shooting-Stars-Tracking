package com.shootingstartracking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import com.shootingstartracking.ShootingStarTrackingData.ShootingStarLocations;
import java.awt.Toolkit;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
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
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
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
	private static final File SAVE_FILE = new File(RuneLite.RUNELITE_DIR, "shooting-star-tracking.json");
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
	@Getter
	private WorldHop worldHop;

	@Inject
	private Notifier notifier;

	@Inject
	private Gson gson;

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
		save();
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
		ShootingStarTrackingData oldStar = starData.stream()
			.filter(star -> data.getWorld() == star.getWorld())
			.findFirst()
			.orElse(null);

		if (oldStar == null) {
			starData.add(data);
			return;
		}

		if (data.getMaxTime() < oldStar.getMinTime()) {
			// data imported is an older star
			return;
		}
		starData.remove(oldStar);

		if (!data.getLocation().equals(oldStar.getLocation())) {
			starData.add(data);
			return;
		}

		long minTime = Math.max(oldStar.getMinTime(), data.getMinTime());
		long maxTime = data.getMaxTime();
		if (data.getMaxTime() >= oldStar.getMinTime()) {
			maxTime = Math.min(oldStar.getMaxTime(), data.getMaxTime());
		}
		ShootingStarTrackingData newStar = new ShootingStarTrackingData(data.getWorld(), data.getLocation(), minTime, maxTime);
		newStar.setNotify(oldStar.isNotify());
		starData.add(newStar);
	}

	private void save()
	{
		String json = gson.toJson(starData);
		try
		{
			Files.write(SAVE_FILE.toPath(), json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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
		boolean fullUpdate = false;

		for (ShootingStarTrackingData star : starData) {
			if (checkNotification(star, now)) {
				fullUpdate = true;
			}
			if (star.getMinTime() < now.minusMinutes(removeTime).toInstant().toEpochMilli()) {
				stars.remove(star);
				fullUpdate = true;
			}
		}

		if (fullUpdate) {
			starData = stars;
			SwingUtilities.invokeLater(() -> panel.updateList(starData));
		} else {
			SwingUtilities.invokeLater(() -> panel.updateTimes(starData));
		}
	}

	private boolean checkNotification(ShootingStarTrackingData star, ZonedDateTime now) {
		if (!star.isNotify()) {
			return false;
		}
		long time = star.getMinTime() + config.notifyPercentage() * (star.getMaxTime() - star.getMinTime()) / 100;
		if (time < now.toInstant().toEpochMilli()) {
			star.setNotify(false);
			String msg = "Star W" + star.getWorld() + " " + star.getLocation().getShortLocation();
			NotifyType type = config.notifyType();
			if (type == NotifyType.BOTH || type == NotifyType.NOTIFICATION) {
				notifier.notify(msg);
			}
			if (type == NotifyType.BOTH || type == NotifyType.CHAT_MESSAGE) {
				sendChatMessage(msg);
			}
			return true;
		}
		return false;
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
		try
		{
			String data = new String(Files.readAllBytes(SAVE_FILE.toPath()));
			load(data);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		panel.updateList(starData);
	}

	private void load(String data)
	{
		JsonArray json = gson.fromJson(data, JsonArray.class);
		json.forEach((star) -> {
			ShootingStarTrackingData parsedStar = gson.fromJson(star, ShootingStarTrackingData.class);
			addToList(parsedStar);
		});
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
		save();
	}

	public void importData()
	{
		try {
			final String clipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString().trim();
			load(clipboard);
		} catch (IOException | UnsupportedFlavorException | JsonSyntaxException er) {
			sendChatMessage("Error importing star data.");
			return;
		}
		sendChatMessage("Imported star data.");
		save();
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
		if (starData.isEmpty()) {
			sendChatMessage("No data to export.");
			return;
		}
		String json = gson.toJson(starData);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
		sendChatMessage("Star data exported to clipboard.");
	}

	public void discordFormat() {
		StringBuilder sb = new StringBuilder();
		for (ShootingStarTrackingData star : starData) {
			sb.append("`")
				.append(star.getWorld())
				.append("` - `")
				.append(star.getLocation().getShortLocation())
				.append("` earliest: ")
				.append("<t:")
				.append(star.getMinTime() / 1000)
				.append(":R>")
				.append(" latest: ")
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

	public void removeWorldsInClipboard()
	{
		try {
			final String clipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString().trim();
			Matcher matcher = Pattern.compile("[wW]([3-5][0-9][0-9])").matcher(clipboard);
			Set<Integer> worlds = new HashSet<>();
			while (matcher.find()) {
				try {
					worlds.add(Integer.parseInt(matcher.group(1)));
				} catch (NumberFormatException ignored) {

				}
			}
			if (worlds.isEmpty()) {
				sendChatMessage("No worlds in format w451 found in clipboard.");
				return;
			}
			long timeNow = ZonedDateTime.now(utcZoneId).toInstant().toEpochMilli();
			// don't remove stars if it's impossible for them to have landed already.
			int sizeBefore = starData.size();
			starData.removeIf(s -> timeNow >= s.getMinTime() && worlds.contains(s.getWorld()));
			int sizeAfter = starData.size();
			sendChatMessage("Removed " + (sizeBefore - sizeAfter) + " worlds.");
		} catch (NumberFormatException | IOException | UnsupportedFlavorException | JsonSyntaxException er) {
			sendChatMessage("Error encountered.");
			return;
		}
		save();
		panel.updateList(starData);
	}
}
