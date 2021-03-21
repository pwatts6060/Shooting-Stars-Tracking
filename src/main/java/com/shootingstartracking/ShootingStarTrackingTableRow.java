package com.shootingstartracking;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class ShootingStarTrackingTableRow extends JPanel {

    @Getter
    private final ShootingStarTrackingData starData;

    private final boolean displayAsMinutes;

    ShootingStarTrackingTableRow(ShootingStarTrackingData starData, boolean displayAsMinutes)
    {
        this.starData = starData;
        this.displayAsMinutes = displayAsMinutes;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2,0,2,0));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(getBackground().brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(getBackground().darker());
            }
        });

        JPanel worldField = buildWorldField();
        worldField.setPreferredSize(new Dimension(60,20));
        worldField.setOpaque(false);

        JPanel locationField = buildLocationField();
        locationField.setPreferredSize(new Dimension(50,20));
        locationField.setOpaque(false);

        JPanel timeField = buildTimeField();
        timeField.setPreferredSize(new Dimension(60,20));
        timeField.setOpaque(false);

        add(worldField,BorderLayout.WEST);
        add(locationField,BorderLayout.CENTER);
        add(timeField,BorderLayout.EAST);
    }

    private JPanel buildWorldField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel worldField = new JLabel(starData.getWorld() + "");
        worldField.setFont(FontManager.getRunescapeSmallFont());
        worldField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        panel.add(worldField,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLocationField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel locationField = new JLabel(starData.getLocation().getLocation());
        locationField.setFont(FontManager.getRunescapeSmallFont());
        locationField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
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
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            Instant instant = Instant.ofEpochMilli(starData.getTime());
            time = LocalDateTime.ofInstant(instant,ZoneId.systemDefault()).format(dtf);
        }
        JLabel timeField = new JLabel(time);
        timeField.setFont(FontManager.getRunescapeSmallFont());
        timeField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        panel.add(timeField);
        return panel;
    }
}
