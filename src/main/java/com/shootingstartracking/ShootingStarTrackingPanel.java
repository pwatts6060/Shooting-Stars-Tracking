package com.shootingstartracking;

import com.google.common.collect.Ordering;
import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ShootingStarTrackingPanel extends PluginPanel {

    private ShootingStarTrackingTableHeader worldHeader;
    private ShootingStarTrackingTableHeader locationHeader;
    private ShootingStarTrackingTableHeader timeHeader;

    private final JPanel listContainer = new JPanel();

    @Getter
    @Setter
    private List<ShootingStarTrackingData> starData = new ArrayList<>();

    private Order orderIndex = Order.WORLD;
    private boolean ascendingOrder = true;

    ShootingStarTrackingPanel()
    {
        setBorder(null);
        setLayout(new DynamicGridLayout(0,1));
        JPanel header = buildHeader();
        listContainer.setLayout(new GridLayout(0,1));
        add(header);
        add(listContainer);
        add(exportPanel());
        add(importPanel());
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

        header.add(worldHeader,BorderLayout.WEST);
        header.add(locationHeader,BorderLayout.CENTER);
        header.add(timeHeader,BorderLayout.EAST);
        return header;
    }

    void updateList()
    {
        starData.sort((r1,r2) ->
        {
            switch (orderIndex)
            {
                case WORLD:
                    return getCompareValue(r1,r2, ShootingStarTrackingData::getWorld);
                case LOCATION:
                    return getCompareValue(r1,r2, row ->
                            row.getLocation().getLocation());
                case TIME:
                    return getCompareValue(r1,r2, ShootingStarTrackingData::getTime);
                default:
                    return 0;
            }
        });
        listContainer.removeAll();

        starData.forEach((star) -> {
            ShootingStarTrackingTableRow r = new ShootingStarTrackingTableRow(star);
            r.setComponentPopupMenu(buildRemoveMenu(star));
            listContainer.add(r);
        });

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
        removeEntryOption.addActionListener(e -> {
            starData.remove(star);
            updateList();
        });
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
        updateList();
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
                    exportDataToClipboard();
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
                    try {
                        importDataFromClipboard();
                    } catch (IOException | UnsupportedFlavorException ioException) {
                        ioException.printStackTrace();
                    }
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

    private void importDataFromClipboard() throws IOException, UnsupportedFlavorException {
        String clipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        JsonArray json;
        try
        {
            json = new Gson().fromJson(clipboard, JsonArray.class);
        }
        catch (JsonSyntaxException e)
        {
            return;
        }

        for (JsonElement el: json) {
            try
            {
                ShootingStarTrackingData sstd = new Gson().fromJson(el, ShootingStarTrackingData.class);
                if (starData.stream().anyMatch(o -> o.getWorld()==sstd.getWorld()))
                {
                    return;
                }
                starData.add(sstd);
            }
            catch (Exception e)
            {
                return;
            }
        }
        updateList();
    }

    private void exportDataToClipboard()
    {
        String json = new Gson().toJson(starData);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
    }

    private enum Order
    {
        WORLD,
        LOCATION,
        TIME
    }
}
