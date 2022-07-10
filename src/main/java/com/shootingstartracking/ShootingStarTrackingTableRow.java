package com.shootingstartracking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import lombok.Getter;
import static net.runelite.client.ui.ColorScheme.BRAND_ORANGE;
import static net.runelite.client.ui.ColorScheme.LIGHT_GRAY_COLOR;
import static net.runelite.client.ui.ColorScheme.MEDIUM_GRAY_COLOR;
import net.runelite.client.ui.FontManager;

import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class ShootingStarTrackingTableRow extends JPanel {

	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
	private static final ZoneId utcZoneId = ZoneId.of("UTC");

    @Getter
    private final ShootingStarTrackingData starData;

    private final boolean displayAsMinutes;

    private JLabel minTimeField;
    private JLabel maxTimeField;

	ShootingStarTrackingTableRow(ShootingStarTrackingData starData, boolean displayAsMinutes, Color backgroundColor, final WorldHop worldHop, int curWorld)
    {
        this.starData = starData;
        this.displayAsMinutes = displayAsMinutes;
        setLayout(new BorderLayout());
		updateNotifyBorder();
        setBackground(backgroundColor);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(getBackground().brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(backgroundColor);
            }
			@Override
			public void mouseClicked(MouseEvent e) {
				// double click row hops to world
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					worldHop.hop(starData.getWorld());
				}
			}
        });

        JPanel worldField = buildWorldField(curWorld);
        worldField.setPreferredSize(new Dimension(ShootingStarTrackingPanel.WORLD_WIDTH,20));
        worldField.setOpaque(false);

        JPanel locationField = buildLocationField();
        locationField.setToolTipText(starData.getLocation().getLocation());
        locationField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(getBackground().brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(backgroundColor);
            }

			@Override
			public void mouseClicked(MouseEvent e) {
				// double click row hops to world
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					worldHop.hop(starData.getWorld());
				}
			}
        });
        locationField.setInheritsPopupMenu(true);
        this.setInheritsPopupMenu(true);
        locationField.setPreferredSize(new Dimension(ShootingStarTrackingPanel.LOCATION_WIDTH,20));
        locationField.setOpaque(false);

        JPanel timeField = buildTimeField();
        timeField.setPreferredSize(new Dimension(ShootingStarTrackingPanel.TIME_WIDTH * 2,20));
        timeField.setOpaque(false);

        add(worldField,BorderLayout.WEST);
        add(locationField,BorderLayout.CENTER);
        add(timeField,BorderLayout.EAST);
    }

    private JPanel buildWorldField(int world)
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
		JLabel worldLabel = new JLabel(Integer.toString(starData.getWorld()));
		worldLabel.setFont(FontManager.getRunescapeSmallFont());
		worldLabel.setForeground(worldColor(world));
        panel.add(worldLabel,BorderLayout.CENTER);
        return panel;
    }

	private Color worldColor(int curWorld) {
		if (starData.getWorld() == curWorld) {
			return BRAND_ORANGE;
		} else {
			return getTimeColor(starData);
		}
	}

    private JPanel buildLocationField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel locationField = new JLabel(starData.getLocation().getShortLocation());
        locationField.setFont(FontManager.getRunescapeSmallFont());
        locationField.setForeground(getTimeColor(starData));
        panel.add(locationField,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTimeField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        minTimeField = new JLabel();
		minTimeField.setFont(FontManager.getRunescapeSmallFont());
        panel.add(minTimeField, BorderLayout.CENTER);
		maxTimeField = new JLabel();
		maxTimeField.setFont(FontManager.getRunescapeSmallFont());
		panel.add(maxTimeField, BorderLayout.EAST);
		updateTime();
        return panel;
    }

    void updateTime() {
		String minTime;
		String maxTime;
		if (displayAsMinutes) {
			minTime = convertTime(starData.getMinTime());
			maxTime = convertTime(starData.getMaxTime());
		} else {
			Instant minInstant = Instant.ofEpochMilli(starData.getMinTime());
			minTime = LocalDateTime.ofInstant(minInstant, ZoneId.systemDefault()).format(dtf);
			Instant maxInstant = Instant.ofEpochMilli(starData.getMaxTime());
			maxTime = LocalDateTime.ofInstant(maxInstant, ZoneId.systemDefault()).format(dtf);
		}
		minTimeField.setText(minTime);
		maxTimeField.setText(maxTime);

		Color color = getTimeColor(starData);
		minTimeField.setForeground(color);
		maxTimeField.setForeground(color);
	}

    public static String convertTime(long epoch) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(epoch - Instant.now().toEpochMilli());
		boolean negative = seconds < 0;
		seconds = Math.abs(seconds);
		String time = negative ? "-" : "";
		long minutes = seconds / 60;
		seconds %= 60;
		time += String.format("%d:%02d", minutes, seconds);
		return time;
	}

    private Color getTimeColor(ShootingStarTrackingData starData) {
		if (starData.getMinTime() > ZonedDateTime.now(utcZoneId).toInstant().toEpochMilli())
		{
			return LIGHT_GRAY_COLOR;
		}
		return MEDIUM_GRAY_COLOR;
	}

	public void updateNotifyBorder()
	{
		if (starData.isNotify())
			setBorder(new LineBorder(BRAND_ORANGE, 1));
		else
			setBorder(new EmptyBorder(1,1,1,1));
	}
}
