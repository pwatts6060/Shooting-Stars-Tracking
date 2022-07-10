package com.shootingstartracking;

import com.google.common.collect.Ordering;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

public class ShootingStarTrackingPanel extends PluginPanel {

	static final int WORLD_WIDTH = 35;
	static final int LOCATION_WIDTH = 60;
	static final int TIME_WIDTH = 45;

    private ShootingStarTrackingTableHeader worldHeader;
    private ShootingStarTrackingTableHeader locationHeader;
    private ShootingStarTrackingTableHeader minTimeHeader;
    private ShootingStarTrackingTableHeader maxTimeHeader;

    private final JPanel listContainer = new JPanel();

    private final ShootingStarTrackingPlugin plugin;
    private Order orderIndex = Order.MIN_TIME;
    private boolean ascendingOrder = false;

    ShootingStarTrackingPanel(ShootingStarTrackingPlugin plugin) {
        this.plugin = plugin;
        setBorder(null);
        setLayout(new DynamicGridLayout(0, 1));
        JPanel header = buildHeader();
        listContainer.setLayout(new GridLayout(0, 1));
        add(header);
        add(listContainer);
        JPanel buttons = new JPanel();
        buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.add(importPanel());
		buttons.add(Box.createRigidArea(new Dimension(0, 5)));
        buttons.add(exportPanel());
		buttons.add(Box.createRigidArea(new Dimension(0, 5)));
        buttons.add(discordPanel());
        add(buttons);
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());

        worldHeader = new ShootingStarTrackingTableHeader("W");
        worldHeader.setPreferredSize(new Dimension(WORLD_WIDTH,20));
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
        locationHeader.setPreferredSize(new Dimension(LOCATION_WIDTH,20));
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

        JPanel timePanel = new JPanel(new BorderLayout());

        minTimeHeader = new ShootingStarTrackingTableHeader("Min");
		minTimeHeader.setPreferredSize(new Dimension(TIME_WIDTH,20));
		minTimeHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                ascendingOrder = orderIndex != Order.MIN_TIME || !ascendingOrder;
                orderBy(Order.MIN_TIME);
            }
        });
		minTimeHeader.highlight(true,ascendingOrder);

		maxTimeHeader = new ShootingStarTrackingTableHeader("Max");
		maxTimeHeader.setPreferredSize(new Dimension(TIME_WIDTH,20));
		maxTimeHeader.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
				{
					return;
				}
				ascendingOrder = orderIndex != Order.MAX_TIME || !ascendingOrder;
				orderBy(Order.MAX_TIME);
			}
		});

		timePanel.add(minTimeHeader, BorderLayout.WEST);
		timePanel.add(maxTimeHeader, BorderLayout.EAST);

        header.add(worldHeader,BorderLayout.WEST);
        header.add(locationHeader,BorderLayout.CENTER);
        header.add(timePanel,BorderLayout.EAST);
        return header;
    }

    void updateList(List<ShootingStarTrackingData> starData) {
        listContainer.removeAll();
        if (starData.isEmpty()) {
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
                    case MIN_TIME:
                        return getCompareValue(r1, r2, ShootingStarTrackingData::getMinTime);
                    default:
                        return 0;
                }
            });

            int i = 0;
            for (ShootingStarTrackingData data : starData) {
                Color backgroundColor = i % 2 == 0 ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
                ShootingStarTrackingTableRow r = new ShootingStarTrackingTableRow(data, plugin.isDisplayAsMinutes(), backgroundColor, plugin.getWorld());
                r.setComponentPopupMenu(buildRemoveMenu(data));
                r.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						// double click row hops to world
						if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
							plugin.hopTo(data);
						}
					}
				});
                listContainer.add(r);
                i++;
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

	public void updateTimes(List<ShootingStarTrackingData> starData)
	{
		if (starData.isEmpty()) {
			return;
		}

		for (Component component : listContainer.getComponents()) {
			ShootingStarTrackingTableRow r = (ShootingStarTrackingTableRow) component;
			r.updateTime();
		}

		listContainer.repaint();
	}

    private JPopupMenu buildRemoveMenu(ShootingStarTrackingData star)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5,5,5,5));

		JMenuItem hopEntryOption = new JMenuItem();
		hopEntryOption.setText("Hop to");
		hopEntryOption.setFont(FontManager.getRunescapeSmallFont());
		hopEntryOption.addActionListener(e -> plugin.hopTo(star));
		popupMenu.add(hopEntryOption);

        JMenuItem removeEntryOption = new JMenuItem();
        removeEntryOption.setText("Remove");
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
        minTimeHeader.highlight(false, ascendingOrder);
        maxTimeHeader.highlight(false, ascendingOrder);
        switch (order)
        {
            case WORLD:
                worldHeader.highlight(true, ascendingOrder);
                break;
            case LOCATION:
                locationHeader.highlight(true, ascendingOrder);
                break;
            case MIN_TIME:
                minTimeHeader.highlight(true, ascendingOrder);
                break;
			case MAX_TIME:
				maxTimeHeader.highlight(true, ascendingOrder);
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

	private JPanel discordPanel()
	{
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Copy for Discord format");
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setPreferredSize(new Dimension(60,30));
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e))
				{
					plugin.discordFormat();
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
		MIN_TIME,
		MAX_TIME
    }
}
