package com.shootingstartracking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

	ShootingStarTrackingTableRow(ShootingStarTrackingData starData, boolean displayAsMinutes, Color backgroundColor, int world)
    {
        this.starData = starData;
        this.displayAsMinutes = displayAsMinutes;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2,0,2,0));
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
        });

        JPanel worldField = buildWorldField(world);
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
        });
        locationField.setInheritsPopupMenu(true);
        this.setInheritsPopupMenu(true);
        locationField.setPreferredSize(new Dimension(ShootingStarTrackingPanel.LOCATION_WIDTH,20));
        locationField.setOpaque(false);

        JPanel timeField = buildTimeField();
        timeField.setPreferredSize(new Dimension(ShootingStarTrackingPanel.TIME_WIDTH,20));
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
        JLabel locationField = new JLabel(starData.getLocation().getLocation());
        locationField.setFont(FontManager.getRunescapeSmallFont());
        locationField.setForeground(getTimeColor(starData));
        panel.add(locationField,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTimeField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        String time;
        if (displayAsMinutes) {
            long timeDelta = TimeUnit.MILLISECONDS.toMinutes(starData.getTime() - Instant.now().toEpochMilli());
            time = timeDelta + " mins";
        } else {
            Instant instant = Instant.ofEpochMilli(starData.getTime());
            time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dtf);
        }
        JLabel timeField = new JLabel(time);
        timeField.setFont(FontManager.getRunescapeSmallFont());
		timeField.setForeground(getTimeColor(starData));
        panel.add(timeField);
        return panel;
    }

    private Color getTimeColor(ShootingStarTrackingData starData) {
		if (starData.getTime() > ZonedDateTime.now(utcZoneId).toInstant().toEpochMilli())
		{
			return LIGHT_GRAY_COLOR;
		}
		return MEDIUM_GRAY_COLOR;
	}
}
