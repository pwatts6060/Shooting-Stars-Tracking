package com.shootingstartracking;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ShootingStarTrackingTableRow extends JPanel {

    @Getter
    private final ShootingStarTrackingData starData;

    ShootingStarTrackingTableRow(ShootingStarTrackingData starData)
    {
        this.starData = starData;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2,0,2,0));

        JPanel worldField = buildWorldField();
        worldField.setPreferredSize(new Dimension(60,20));

        JPanel locationField = buildLocationField();
        locationField.setPreferredSize(new Dimension(50,20));
        JPanel timeField = buildTimeField();
        timeField.setPreferredSize(new Dimension(60,20));

        add(worldField,BorderLayout.WEST);
        add(locationField,BorderLayout.CENTER);
        add(timeField,BorderLayout.EAST);
    }

    private JPanel buildWorldField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel worldField = new JLabel(starData.getWorld() + "");
        worldField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        panel.add(worldField,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLocationField()
    {
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel locationField = new JLabel(starData.getLocation().getLocation());
        locationField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        panel.add(locationField,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTimeField()
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        Instant instant = Instant.ofEpochMilli(starData.getTime());
        String time = LocalDateTime.ofInstant(instant,ZoneId.systemDefault()).format(dtf);
        JPanel panel = new JPanel(new BorderLayout(7,0));
        panel.setBorder(new EmptyBorder(0,5,0,5));
        JLabel timeField = new JLabel(time);
        timeField.setForeground((starData.getTime() > ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()) ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        panel.add(timeField);
        return panel;
    }
}
