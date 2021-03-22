package com.shootingstartracking;

import com.google.common.collect.Ordering;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

public class ShootingStarTrackingPanel extends PluginPanel {

    private ShootingStarTrackingTableHeader worldHeader;
    private ShootingStarTrackingTableHeader locationHeader;
    private ShootingStarTrackingTableHeader timeHeader;

    private final JPanel listContainer = new JPanel();

    private final ShootingStarTrackingPlugin plugin;
    private Order orderIndex = Order.TIME;
    private boolean ascendingOrder = true;

    ShootingStarTrackingPanel(ShootingStarTrackingPlugin plugin) {
        this.plugin = plugin;
        setBorder(null);
        setLayout(new DynamicGridLayout(0, 1));
        JPanel header = buildHeader();
        listContainer.setLayout(new GridLayout(0, 1));
        add(header);
        add(listContainer);
        JPanel buttons = new JPanel();
        buttons.add(importPanel(), BorderLayout.WEST);
        buttons.add(exportPanel(),BorderLayout.EAST);
        add(buttons);
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());

        worldHeader = new ShootingStarTrackingTableHeader("World");
        worldHeader.setPreferredSize(new Dimension(60,20));
        worldHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                ascendingOrder = orderIndex != Order.WORLD || !ascendingOrder;
                orderBy(Order.WORLD);
            }
        });

        locationHeader = new ShootingStarTrackingTableHeader("Location");
        locationHeader.setPreferredSize(new Dimension(50,20));
        locationHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                ascendingOrder = orderIndex != Order.LOCATION || !ascendingOrder;
                orderBy(Order.LOCATION);
            }
        });

        timeHeader = new ShootingStarTrackingTableHeader("Time");
        timeHeader.setPreferredSize(new Dimension(60,20));
        timeHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                ascendingOrder = orderIndex != Order.TIME || !ascendingOrder;
                orderBy(Order.TIME);
            }
        });
        timeHeader.highlight(true,ascendingOrder);

        header.add(worldHeader,BorderLayout.WEST);
        header.add(locationHeader,BorderLayout.CENTER);
        header.add(timeHeader,BorderLayout.EAST);
        return header;
    }

    void updateList(List<ShootingStarTrackingData> starData) {
        listContainer.removeAll();
        if (starData.size() == 0) {
            JLabel noStarsLabel = new JLabel("Look through a telescope to start tracking stars");
            noStarsLabel.setFont(FontManager.getRunescapeSmallFont());
            noStarsLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
            listContainer.add(noStarsLabel);
        } else {
            starData.sort((r1, r2) ->
            {
                switch (orderIndex) {
                    case WORLD:
                        return getCompareValue(r1, r2, ShootingStarTrackingData::getWorld);
                    case LOCATION:
                        return getCompareValue(r1, r2, row ->
                                row.getLocation().getLocation());
                    case TIME:
                        return getCompareValue(r1, r2, ShootingStarTrackingData::getTime);
                    default:
                        return 0;
                }
            });

            for (int i = 0; i < starData.size(); i++)
            {
                Color backgroundColor = i % 2 == 0 ? ColorScheme.DARK_GRAY_COLOR :ColorScheme.DARKER_GRAY_COLOR;
                ShootingStarTrackingTableRow r = new ShootingStarTrackingTableRow(starData.get(i), plugin.isDisplayAsMinutes(), backgroundColor);
                r.setComponentPopupMenu(buildRemoveMenu(starData.get(i)));
                listContainer.add(r);
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPopupMenu buildRemoveMenu(ShootingStarTrackingData star)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5,5,5,5));

        JMenuItem removeEntryOption = new JMenuItem();
        removeEntryOption.setText("Remove?");
        removeEntryOption.setFont(FontManager.getRunescapeSmallFont());
        removeEntryOption.addActionListener(e -> plugin.removeStar(star));
        popupMenu.add(removeEntryOption);
        return popupMenu;
    }

    private int getCompareValue(ShootingStarTrackingData row1, ShootingStarTrackingData row2, Function<ShootingStarTrackingData, Comparable> compareFn)
    {
        Ordering<Comparable> ordering = Ordering.natural();
        if (!ascendingOrder)
        {
            ordering = ordering.reverse();
        }
        ordering = ordering.reverse();
        return ordering.compare(compareFn.apply(row1),compareFn.apply(row2));
    }

    private void orderBy(Order order)
    {
        worldHeader.highlight(false, ascendingOrder);
        locationHeader.highlight(false, ascendingOrder);
        timeHeader.highlight(false, ascendingOrder);
        switch (order)
        {
            case WORLD:
                worldHeader.highlight(true, ascendingOrder);
                break;
            case LOCATION:
                locationHeader.highlight(true, ascendingOrder);
                break;
            case TIME:
                timeHeader.highlight(true, ascendingOrder);
                break;
        }
        orderIndex = order;
        updateList(plugin.getStarData());
    }

    private JPanel exportPanel()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Export");
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setPreferredSize(new Dimension(60,30));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e))
                {
                    plugin.exportData();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });
        panel.add(label);
        return panel;
    }

    private JPanel importPanel()
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Import");
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setPreferredSize(new Dimension(60,30));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e))
                {
                    plugin.importData();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });
        panel.add(label);
        return panel;
    }

    private enum Order
    {
        WORLD,
        LOCATION,
        TIME
    }
}
